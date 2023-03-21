package org.touchhome.bundle.weather.providers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Component;
import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.hquery.hardware.network.NetworkHardwareRepository;
import org.touchhome.bundle.weather.setting.WeatherApiKeySetting;
import org.touchhome.bundle.weather.setting.WeatherLangSetting;
import org.touchhome.bundle.weather.setting.WeatherUnitSetting;

@Component
public class OpenWeatherMapProvider extends BaseWeatherProvider<OpenWeatherMapProvider.WeatherJSON> {

  private static final String URL =
      "https://api.openweathermap.org/data/2.5/onecall?lat=${lat}&lon=${lon}&appid=${key}&units=${unit}&lang=${lang}";
  private final EntityContext entityContext;

  public OpenWeatherMapProvider(EntityContext entityContext, NetworkHardwareRepository networkHardwareRepository) {
    super(WeatherJSON.class, URL, networkHardwareRepository);
    this.entityContext = entityContext;
  }

  @Override
  public Double readWeatherTemperature(String city) {
    return this.readWeather(city).current.temp;
  }

  @Override
  public Double readWeatherHumidity(String city) {
    return this.readWeather(city).current.humidity;
  }

  @Override
  public Double readWeatherPressure(String city) {
    return this.readWeather(city).current.pressure;
  }

  @Override
  public String getDescription() {
    return "You has to acquire api key for provider<\br><a href='https://openweathermap.org/'>OpenWeather</a>";
  }

  @Override
  protected StringSubstitutor buildWeatherRequest(String city, String latt, String longt) {
    Map<String, String> valuesMap = new HashMap<>();

    valuesMap.put("lat", latt);
    valuesMap.put("lon", longt);
    valuesMap.put("unit", entityContext.setting().getValue(WeatherUnitSetting.class).name());
    valuesMap.put("key", entityContext.setting().getValue(WeatherApiKeySetting.class));
    valuesMap.put("lang", entityContext.setting().getValue(WeatherLangSetting.class).name());
    return new StringSubstitutor(valuesMap);
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
