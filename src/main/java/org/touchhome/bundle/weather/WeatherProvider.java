package org.touchhome.bundle.weather;

public interface WeatherProvider<T> {

  T readWeather(String city);

  Double readWeatherTemperature(String city);

  Double readWeatherHumidity(String city);

  Double readWeatherPressure(String city);
}
