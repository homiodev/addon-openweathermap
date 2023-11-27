package org.homio.addon.openweathermap;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.text.StringSubstitutor;
import org.homio.addon.openweathermap.OpenWeatherMapService.WeatherJSON.WeatherStat;
import org.homio.api.Context;
import org.homio.api.exception.ServerException;
import org.homio.api.model.OptionModel.HasDescription;
import org.homio.api.service.EntityService.ServiceInstance;
import org.homio.api.service.WeatherEntity.WeatherInfo;
import org.homio.api.service.WeatherEntity.WeatherInfo.HourWeatherInfo;
import org.homio.api.service.WeatherEntity.WeatherService;
import org.homio.hquery.Curl;
import org.homio.hquery.hardware.network.NetworkHardwareRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpenWeatherMapService extends ServiceInstance<OpenWeatherMapEntity> implements
    HasDescription, WeatherService {

  private static final String ONECALL_URL = "https://api.openweathermap.org/data/2.5/onecall?lat=${lat}&lon=${lon}&appid=${key}&units=${unit}&lang=${lang}";
  private static final String TIMEMACHINE_URL = "https://api.openweathermap.org/data/2.5/onecall/timemachine?lat=${lat}&lon=${lon}&dt=${time}&appid=${key"
      + "}&units=${unit}&lang=${lang}";

  private final Class<WeatherJSON> weatherJSONType = WeatherJSON.class;
  private final NetworkHardwareRepository networkHardwareRepository;
  private WeatherJSON data;
  private long lastRequestTimeout;

  public OpenWeatherMapService(Context context, OpenWeatherMapEntity entity) {
    super(context, entity, true);
    this.networkHardwareRepository = context.getBean(NetworkHardwareRepository.class);
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

  protected StringSubstitutor buildWeatherRequest(String latt, String longt, Long timestamp) {
    Map<String, String> valuesMap = new HashMap<>();

    valuesMap.put("lat", latt);
    valuesMap.put("lon", longt);
    if (timestamp != null) {
      valuesMap.put("time", String.valueOf(timestamp));
    }
    valuesMap.put("unit", entity.getUnit().name());
    valuesMap.put("key", entity.getApiToken().asString());
    valuesMap.put("lang", entity.getLang().name());
    return new StringSubstitutor(valuesMap);
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
  private synchronized WeatherJSON readJson(String city, Long timestamp) {
    if (data == null || System.currentTimeMillis() - lastRequestTimeout > 10000) {
      JsonNode cityGeolocation = networkHardwareRepository.getCityGeolocation(city);
      if (cityGeolocation.has("error")) {
        throw new ServerException(cityGeolocation.get("error").get("description").asText());
      }
      data = Curl.get(buildWeatherRequest(
              cityGeolocation.get("latt").asText(),
              cityGeolocation.get("longt").asText(),
              timestamp)
              .replace(timestamp == null ? ONECALL_URL : TIMEMACHINE_URL),
          weatherJSONType);
      lastRequestTimeout = System.currentTimeMillis();
    }
    return data;
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
