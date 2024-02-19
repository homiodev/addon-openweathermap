package org.homio.addon.openweathermap;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.text.StringSubstitutor;
import org.homio.addon.openweathermap.OpenWeatherMapService.WeatherJSON.WeatherStat;
import org.homio.api.Context;
import org.homio.api.ContextBGP;
import org.homio.api.ContextBGP.ThreadContext;
import org.homio.api.ContextNetwork.CityGeolocation;
import org.homio.api.ContextVar.Variable;
import org.homio.api.model.OptionModel.HasDescription;
import org.homio.api.model.Status;
import org.homio.api.service.EntityService.ServiceInstance;
import org.homio.api.service.WeatherEntity.WeatherInfo;
import org.homio.api.service.WeatherEntity.WeatherInfo.HourWeatherInfo;
import org.homio.api.service.WeatherEntity.WeatherInfoType;
import org.homio.api.service.WeatherEntity.WeatherService;
import org.homio.hquery.Curl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpenWeatherMapService extends ServiceInstance<OpenWeatherMapEntity> implements
    HasDescription, WeatherService {

  private static final String ONECALL_URL = "https://api.openweathermap.org/data/2.5/onecall?lat=${lat}&lon=${lon}&appid=${key}&units=${unit}&lang=${lang}";
  private static final String TIMEMACHINE_URL = "https://api.openweathermap.org/data/2.5/onecall/timemachine?lat=${lat}&lon=${lon}&dt=${time}&appid=${key"
      + "}&units=${unit}&lang=${lang}";

  private final Class<WeatherJSON> weatherJSONType = WeatherJSON.class;

  private final Map<String, Variable> owmVariables = new ConcurrentHashMap<>();
  private final LoadingCache<String, WeatherJSON> dataCache;
  private ThreadContext<Void> weatherListeners;

  public OpenWeatherMapService(Context context, OpenWeatherMapEntity entity) {
    super(context, entity, true);

    this.dataCache = CacheBuilder
        .newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build(new CacheLoader<>() {
          @Override
          public @NotNull WeatherJSON load(@NotNull String city) {
            CityGeolocation cityInfo = context.hardware().network().getCityGeolocation(city);
            return Curl.get(buildWeatherRequest(
                cityInfo.getLat(),
                cityInfo.getLon(), null)
                .replace(ONECALL_URL), weatherJSONType);
          }
        });

    context.var().onVariableCreated("owm-listener", this::addVariableToListen);
    context.var().onVariableRemoved("owm-listener", (var) -> {
      if (owmVariables.remove(var.getId()) != null && owmVariables.isEmpty()) {
        ContextBGP.cancel(weatherListeners);
        weatherListeners = null;
      }
    });
    for (Variable var : context.var().getVariables()) {
      addVariableToListen(var);
    }
  }

  @Override
  public String isRequireRestartService() {
    if (entity.getStatus() == Status.ERROR) {
      return "Error status of: " + entity.getTitle();
    }
    return null;
  }

  protected StringSubstitutor buildWeatherRequest(double latt, double longt, @Nullable Long timestamp) {
    Map<String, String> valuesMap = new HashMap<>();

    valuesMap.put("lat", String.valueOf(latt));
    valuesMap.put("lon", String.valueOf(longt));
    if (timestamp != null) {
      valuesMap.put("time", String.valueOf(timestamp));
    }
    valuesMap.put("unit", entity.getUnit().name());
    valuesMap.put("key", entity.getApiToken().asString());
    valuesMap.put("lang", entity.getLang().name());
    return new StringSubstitutor(valuesMap);
  }

  private void addVariableToListen(Variable var) {
    if (entity.getEntityID().equals(var.getJsonData().optString("weatherProvider"))) {
      owmVariables.put(var.getId(), var);
      if (weatherListeners == null) {
        weatherListeners = context
            .bgp()
            .builder("owm-weather")
            .interval(Duration.ofMinutes(1))
            .execute(() -> {
              for (Variable variable : owmVariables.values()) {
                try {
                  var.set(readWeather(variable));
                } catch (Exception ex) {
                  log.warn("Unable to read weather info for city: {}", var.getJsonData().optString("city"), ex);
                }
              }
            });
      }
    }
  }

  @Override
  public @NotNull WeatherInfo readWeather(@NotNull String city, @Nullable Long timestamp) {
    WeatherJSON json = readJson(city, timestamp);
    WeatherInfo info = buildWeatherInfo(json);
    for (WeatherStat hourInfo : json.getHourly()) {
      HourWeatherInfo hi = new HourWeatherInfo();
      hi.setDt(hourInfo.getDt());
      hi.setTemperature(hourInfo.getTemp());
      hi.setFeelsLike(hourInfo.getFeels_like());
      hi.setHumidity(hourInfo.getHumidity());
      hi.setPressure(hourInfo.getPressure());
      info.getHours().put(hourInfo.getDt(), hi);
    }

    return info;
  }

  @Override
  public String getDescription() {
    return "You has to acquire api key for provider<\br><a href='https://openweathermap.org/'>OpenWeather</a>";
  }

  @Override
  public void destroy(boolean forRestart, @Nullable Exception ex) {

  }

  @SneakyThrows
  private Double readWeather(Variable variable) {
    WeatherJSON data = dataCache.get(variable.getJsonData().getString("city"));
    return switch (WeatherInfoType.valueOf(variable.getJsonData().getString("type"))) {
      case Temperature -> data.getCurrent().getTemp();
      case Pressure -> data.getCurrent().getPressure();
      case Humidity -> data.getCurrent().getHumidity();
      case WindSpeed -> data.getCurrent().getWind_speed();
      case WindDegree -> data.getCurrent().getWind_deg();
      case FeelsLike -> data.getCurrent().getFeels_like();
      case Visibility -> data.getCurrent().getVisibility();
      case Clouds -> data.getCurrent().getClouds();
    };
  }

  @Override
  protected void initialize() {
    testServiceWithSetStatus();
  }

  @Override
  protected void testService() {
    readWeather("London", System.currentTimeMillis());
  }

  @NotNull
  private static WeatherInfo buildWeatherInfo(WeatherJSON json) {
    WeatherInfo info = new WeatherInfo();
    info.setDt(json.getCurrent().getDt());
    info.setFeelsLike(json.getCurrent().getFeels_like());
    info.setHumidity(json.getCurrent().getHumidity());
    info.setPressure(json.getCurrent().getPressure());
    info.setSunrise(json.getCurrent().getSunrise());
    info.setSunset(json.getCurrent().getSunset());
    info.setTemperature(json.getCurrent().getTemp());
    info.setVisibility(json.getCurrent().getVisibility());
    info.setWindDegree(json.getCurrent().getWind_deg());
    info.setWindSpeed(json.getCurrent().getWind_speed());
    info.setHours(new HashMap<>());
    return info;
  }

  /**
   * Read from weather provider not ofter than one minute
   */
  @SneakyThrows
  private synchronized WeatherJSON readJson(@NotNull String city, @Nullable Long timestamp) {
    if (timestamp == null) {
      return dataCache.get(city);
    }
    CityGeolocation cityInfo = context.hardware().network().getCityGeolocation(city);
    return Curl.get(buildWeatherRequest(cityInfo.getLat(), cityInfo.getLon(),
        timestamp).replace(TIMEMACHINE_URL), weatherJSONType);
  }

  @Getter
  @Setter
  public static class WeatherJSON {

    private WeatherStat current;
    private Long lat;
    private Long lon;
    private Long timezone_offset;
    private String timezone;
    private List<WeatherStat> hourly;

    @Getter
    @Setter
    public static class WeatherStat {

      private Long dt;
      private Double temp;
      private Double pressure;
      private Double humidity;
      private Double feels_like;
      private Double clouds;
      private Double visibility;
      private Double wind_speed;
      private Double wind_deg;
      private Long sunrise;
      private Long sunset;
    }
  }
}
