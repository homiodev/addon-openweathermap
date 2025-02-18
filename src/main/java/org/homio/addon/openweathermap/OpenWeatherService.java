package org.homio.addon.openweathermap;

import static org.homio.addon.openweathermap.OpenWeatherEntity.WEATHER_PROVIDER;
import static org.homio.api.util.Lang.CURRENT_LANG;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.text.StringSubstitutor;
import org.homio.api.Context;
import org.homio.api.ContextBGP;
import org.homio.api.ContextBGP.ThreadContext;
import org.homio.api.ContextNetwork.CityGeolocation;
import org.homio.api.ContextVar.Variable;
import org.homio.api.model.JSON;
import org.homio.api.model.OptionModel.HasDescription;
import org.homio.api.model.Status;
import org.homio.api.service.EntityService.ServiceInstance;
import org.homio.api.service.WeatherEntity.WeatherInfo;
import org.homio.api.service.WeatherEntity.WeatherInfoType;
import org.homio.api.service.WeatherEntity.WeatherService;
import org.homio.api.widget.CustomWidgetDataStore;
import org.homio.hquery.Curl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpenWeatherService extends ServiceInstance<OpenWeatherEntity>
    implements HasDescription, WeatherService {

  private static final Map<Integer, Double> HOUR_WEIGHTS =
      Map.of(
          0,
          0.5,
          3,
          0.5,
          6,
          0.75, // Night and morning
          9,
          1.0,
          12,
          1.0,
          15,
          1.0, // Day
          18,
          0.75,
          21,
          0.5 // Evening
          );

  private static final List<String> WEATHER_PRIORITY =
      List.of(
          "11", // Thunderstorm
          "09",
          "10", // Rain
          "13", // Snow
          "50", // Mist/Fog
          "04",
          "03",
          "02", // Clouds
          "01" // Clear
          );

  private static final String ONECALL_URL =
      "https://api.openweathermap.org/data/2.5/weather?lat=${lat}&lon=${lon}&appid=${key}&units=${unit}&lang=${lang}";

  private static final String FORECAST_URL =
      "https://api.openweathermap.org/data/2.5/forecast?lat=${lat}&lon=${lon}&appid=${key}&units=${unit}&lang=${lang}";

  private final Map<String, Variable> owmVariables = new ConcurrentHashMap<>();
  private final Map<String, WidgetInfo> widgetListeners = new ConcurrentHashMap<>();

  private final LoadingCache<String, JsonNode> dataCache;
  private ThreadContext<Void> weatherListeners;

  public OpenWeatherService(Context context, OpenWeatherEntity entity) {
    super(context, entity, true, "OpenWeatherMap");

    this.dataCache =
        CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(
                new CacheLoader<>() {
                  @Override
                  public @NotNull JsonNode load(@NotNull String city) {
                    CityGeolocation cityInfo = context.network().getCityGeolocation(city);
                    var weather =
                        Curl.get(
                            buildWeatherRequest(cityInfo.getLat(), cityInfo.getLon())
                                .replace(ONECALL_URL),
                            ObjectNode.class);
                    var forecast =
                        Curl.get(
                            buildWeatherRequest(cityInfo.getLat(), cityInfo.getLon())
                                .replace(FORECAST_URL),
                            JsonNode.class);
                    weather.set("forecast", forecast.get("list"));
                    return weather;
                  }
                });

    context.var().onVariableCreated("owm-listener", this::addVariableToListen);
    context
        .var()
        .onVariableRemoved(
            "owm-listener",
            (var) -> {
              if (owmVariables.remove(var.getId()) != null && owmVariables.isEmpty()) {
                ContextBGP.cancel(weatherListeners);
                weatherListeners = null;
              }
            });
    for (Variable var : context.var().getVariables()) {
      addVariableToListen(var);
    }
  }

  private static void overrideDataForCurrentDayFromMainInfo(
      WeatherInfo info, List<WeatherInfo.DailyForecast> dailyForecasts) {
    var curDate = Instant.ofEpochMilli(info.getDt()).atZone(ZoneId.systemDefault()).toLocalDate();
    for (WeatherInfo.DailyForecast dailyForecast : dailyForecasts) {
      var dayDate =
          Instant.ofEpochMilli(dailyForecast.getDt()).atZone(ZoneId.systemDefault()).toLocalDate();

      if (dayDate.isEqual(curDate)) {
        if (info.getMaxTemperature() > dailyForecast.getMaxTemp()) {
          dailyForecast.setMaxTemp(info.getMaxTemperature());
        }
        if (info.getMinTemperature() < dailyForecast.getMinTemp()) {
          dailyForecast.setMinTemp(info.getMinTemperature());
        }
        dailyForecast.setCondition(info.getCondition());
        dailyForecast.setIcon(info.getIcon());
      }
    }
  }

  @Override
  public String isRequireRestartService() {
    if (entity.getStatus() == Status.ERROR) {
      return "Error status of: " + entity.getTitle();
    }
    return null;
  }

  protected StringSubstitutor buildWeatherRequest(double latt, double longt) {
    Map<String, String> valuesMap = new HashMap<>();

    valuesMap.put("lat", String.valueOf(latt));
    valuesMap.put("lon", String.valueOf(longt));
    valuesMap.put("unit", entity.getUnit().name());
    valuesMap.put("key", entity.getApiToken().asString());
    valuesMap.put("lang", CURRENT_LANG);
    return new StringSubstitutor(valuesMap);
  }

  private void addVariableToListen(Variable var) {
    if (entity.getEntityID().equals(var.getJsonData().optString(WEATHER_PROVIDER))) {
      owmVariables.put(var.getId(), var);
      createWeatherListenerIfRequire();
    }
  }

  @SneakyThrows
  @Override
  public @NotNull WeatherInfo readWeather(@NotNull String city, @Nullable Long timestamp) {
    if (timestamp != null) {
      throw new RuntimeException("Not implemented yet");
    }
    JsonNode json = dataCache.get(city);
    WeatherInfo info = new WeatherInfo();
    info.setDt(json.get("dt").asLong() * 1000);
    JsonNode main = json.get("main");
    info.setPressure(main.get("pressure").asDouble());
    info.setTemperature(main.get("temp").asDouble());
    info.setHumidity(main.get("humidity").asDouble());
    info.setPressure(main.get("pressure").asDouble());
    info.setFeelsLike(main.get("feels_like").asDouble());
    info.setMinTemperature(main.get("temp_min").asDouble());
    info.setMaxTemperature(main.get("temp_max").asDouble());
    info.setVisibility(json.get("visibility").asDouble());
    info.setSunrise(json.get("sys").get("sunrise").asLong() * 1000);
    info.setSunset(json.get("sys").get("sunset").asLong() * 1000);
    info.setWindDegree(json.path("wind").path("deg").asDouble());
    info.setWindSpeed(json.path("wind").path("speed").asDouble());
    info.setClouds(json.path("clouds").path("all").asDouble());
    info.setCity(json.get("name").asText());
    info.setRainSpeed(json.path("rain").path("1h").asDouble());
    var weather = json.withArray("weather").get(0);
    info.setIcon(weather.get("icon").asText());
    info.setCondition(weather.path("main").asText());

    JsonNode forecast = json.get("forecast");
    if (forecast != null) {
      info.setForecast(processForecast(forecast, info));
    }

    return info;
  }

  private List<WeatherInfo.DailyForecast> processForecast(JsonNode forecast, WeatherInfo info) {
    Map<String, DailyForecastData> dailyData = new HashMap<>();
    DateTimeFormatter dateFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    for (JsonNode entry : forecast) {
      long dt = entry.get("dt").asLong() * 1000;
      String date = dateFormatter.format(Instant.ofEpochMilli(dt));
      int hour = Instant.ofEpochMilli(dt).atZone(ZoneId.systemDefault()).getHour();

      dailyData.computeIfAbsent(date, k -> new DailyForecastData(dt));

      ArrayNode weather = entry.withArray("weather");
      Optional<JsonNode> weatherNode =
          Optional.ofNullable(weather).filter(w -> !w.isEmpty()).map(w -> w.get(0));
      String icon = weatherNode.map(w -> w.get("icon").asText()).orElse("01d");
      String condition = weatherNode.map(w -> w.get("main").asText()).orElse("Unknown");

      double weight = HOUR_WEIGHTS.getOrDefault(hour, 0.5);
      dailyData.get(date).addWeightedData(icon, condition, weight);
      dailyData.get(date).addHours(entry);
    }

    var dailyForecasts =
        dailyData.values().stream()
            .sorted(Comparator.comparing(DailyForecastData::getDt))
            .map(DailyForecastData::toDailyForecast)
            .collect(Collectors.toList());
    // override current date
    overrideDataForCurrentDayFromMainInfo(info, dailyForecasts);

    return dailyForecasts;
  }

  @Override
  public String getDescription() {
    return "You has to acquire api key for provider<\br><a href='https://openweathermap.org/'>OpenWeather</a>";
  }

  @Override
  public void destroy(boolean forRestart, @Nullable Exception ex) {
    ContextBGP.cancel(weatherListeners);
  }

  @SneakyThrows
  private Double readWeather(Variable variable) {
    var json = dataCache.get(variable.getJsonData().getString("city"));
    return switch (WeatherInfoType.valueOf(variable.getJsonData().getString("type"))) {
      case Temperature -> json.get("main").get("temp").asDouble();
      case Pressure -> json.get("main").get("pressure").asDouble();
      case Humidity -> json.get("main").get("humidity").asDouble();
      case WindSpeed -> json.get("wind").get("speed").asDouble();
      case WindDegree -> json.get("wind").get("deg").asDouble();
      case FeelsLike -> json.get("main").get("feels_like").asDouble();
      case Visibility -> json.get("visibility").asDouble();
      case Clouds -> json.get("clouds").get("all").asDouble();
    };
  }

  @Override
  protected void initialize() {
    testServiceWithSetStatus();
  }

  @Override
  protected void testService() {
    readWeather("London", null);
  }

  public void setWidgetDataStore(
      CustomWidgetDataStore widgetDataStore,
      @NotNull String widgetEntityID,
      @NotNull JSON widgetData) {
    widgetListeners.put(widgetEntityID, new WidgetInfo(widgetDataStore, widgetData));
    createWeatherListenerIfRequire();
    setWidgetDataToUI();
  }

  public void setWidgetDataToUI() {
    for (WidgetInfo info : widgetListeners.values()) {
      info.store.update(readWeather(info.widgetData.getString("city"), null));
    }
  }

  public void removeWidgetDataStore(@NotNull String widgetEntityID) {
    widgetListeners.remove(widgetEntityID);
    createWeatherListenerIfRequire();
  }

  private void createWeatherListenerIfRequire() {
    if (owmVariables.isEmpty() && widgetListeners.isEmpty() && weatherListeners != null) {
      weatherListeners.cancel();
      weatherListeners = null;
      return;
    }
    if (weatherListeners != null) {
      return;
    }
    weatherListeners =
        context
            .bgp()
            .builder("owm-weather")
            .interval(Duration.ofMinutes(entity.getRefreshRate()))
            .execute(this::updateListeners);
  }

  private void updateListeners() {
    setWidgetDataToUI();
    for (Variable variable : owmVariables.values()) {
      try {
        variable.set(readWeather(variable));
      } catch (Exception ex) {
        log.warn(
            "Unable to read weather info for city: {}",
            variable.getJsonData().optString("city"),
            ex);
      }
    }
  }

  @Getter
  @RequiredArgsConstructor
  private static class DailyForecastData {
    private final long dt;
    private final Map<String, Double> weightedWeatherConditions = new HashMap<>();
    private final Map<String, Double> weightedIconCounts = new HashMap<>();
    private final List<JsonNode> hours = new ArrayList<>();

    public void addWeightedData(String icon, String condition, double weight) {
      weightedWeatherConditions.merge(condition, weight, Double::sum);
      weightedIconCounts.merge(icon, weight, Double::sum);
    }

    public String getMostFrequentWeatherCondition() {
      return weightedWeatherConditions.entrySet().stream()
          .max(Map.Entry.comparingByValue())
          .map(Map.Entry::getKey)
          .orElse("Unknown");
    }

    private String determinePrioritizedIcon() {
      return WEATHER_PRIORITY.stream()
          .flatMap(
              priority ->
                  weightedIconCounts.entrySet().stream()
                      .filter(e -> e.getKey().startsWith(priority))
                      .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                      .map(Map.Entry::getKey))
          .findFirst()
          .orElse("01d");
    }

    public WeatherInfo.DailyForecast toDailyForecast() {
      String dayOfWeek =
          DateTimeFormatter.ofPattern("EEE")
              .format(Instant.ofEpochMilli(dt).atZone(ZoneId.systemDefault()));
      var hours = new LinkedHashMap<Long, WeatherInfo.HourWeatherInfo>();
      double minTemp = Double.MAX_VALUE;
      double maxTemp = Double.MIN_VALUE;
      for (JsonNode hour : this.hours) {
        long dt = hour.get("dt").asLong() * 1000;
        double temp = hour.get("main").get("temp").asDouble();
        double humidity = hour.get("main").get("humidity").asDouble();
        double pressure = hour.get("main").get("pressure").asDouble();
        double feelsLike = hour.get("main").get("feels_like").asDouble();
        double speed = hour.get("wind").get("speed").asDouble();
        double deg = hour.get("wind").get("deg").asDouble();
        double min = hour.get("main").get("temp_min").asDouble();
        double max = hour.get("main").get("temp_max").asDouble();
        String condition = hour.withArray("weather").get(0).get("description").asText();
        String icon = hour.withArray("weather").get(0).get("icon").asText();
        hours.put(
            dt,
            new WeatherInfo.HourWeatherInfo(
                min, max, temp, feelsLike, humidity, pressure, speed, deg, icon, condition, dt));

        minTemp = Math.min(minTemp, min);
        maxTemp = Math.max(maxTemp, max);
      }

      return new WeatherInfo.DailyForecast(
          dt,
          dayOfWeek,
          determinePrioritizedIcon(),
          minTemp,
          maxTemp,
          getMostFrequentWeatherCondition(),
          hours);
    }

    public void addHours(JsonNode hours) {
      this.hours.add(hours);
    }
  }

  private record WidgetInfo(CustomWidgetDataStore store, JSON widgetData) {}
}
