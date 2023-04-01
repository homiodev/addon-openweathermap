package org.homio.bundle.weather.setting;

import org.homio.bundle.weather.providers.OpenWeatherMapProvider;
import org.homio.bundle.api.setting.SettingPluginOptionsBean;
import org.homio.bundle.weather.WeatherProvider;

public class WeatherProviderSetting implements SettingPluginOptionsBean<WeatherProvider> {

  @Override
  public Class<WeatherProvider> getType() {
    return WeatherProvider.class;
  }

  @Override
  public String getDefaultValue() {
    return OpenWeatherMapProvider.class.getSimpleName();
  }

  @Override
  public int order() {
    return 1;
  }
}
