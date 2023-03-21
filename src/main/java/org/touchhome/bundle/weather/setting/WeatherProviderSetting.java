package org.touchhome.bundle.weather.setting;

import org.touchhome.bundle.api.setting.SettingPluginOptionsBean;
import org.touchhome.bundle.weather.WeatherProvider;
import org.touchhome.bundle.weather.providers.OpenWeatherMapProvider;

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
