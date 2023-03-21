package org.touchhome.bundle.weather.providers;

import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringSubstitutor;
import org.touchhome.bundle.api.model.HasDescription;
import org.touchhome.bundle.api.util.Curl;
import org.touchhome.bundle.hquery.hardware.network.NetworkHardwareRepository;
import org.touchhome.bundle.weather.WeatherProvider;

@RequiredArgsConstructor
public abstract class BaseWeatherProvider<T> implements WeatherProvider<T>, HasDescription {

  private final Class<T> weatherJSONType;
  private final String url;
  private final NetworkHardwareRepository networkHardwareRepository;
  private T data;
  private long lastRequestTimeout;

  /**
   * Read from weather provider not ofter than one minute
   */
  private synchronized T readJson(String city) {
    if (data == null || System.currentTimeMillis() - lastRequestTimeout > 60000) {
      NetworkHardwareRepository.CityToGeoLocation cityGeolocation = networkHardwareRepository.findCityGeolocation(city);
      data = Curl.get(buildWeatherRequest(city, cityGeolocation.getLatt(), cityGeolocation.getLongt()).replace(url),
          weatherJSONType);
      lastRequestTimeout = System.currentTimeMillis();
    }
    return data;
  }

  public T readWeather(String city) {
    return readJson(city);
  }

  protected abstract StringSubstitutor buildWeatherRequest(String city, String latt, String longt);
}
