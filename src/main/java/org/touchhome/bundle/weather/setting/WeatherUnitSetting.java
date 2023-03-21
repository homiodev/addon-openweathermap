package org.touchhome.bundle.weather.setting;

import org.touchhome.bundle.api.setting.SettingPluginOptionsEnum;

public class WeatherUnitSetting implements SettingPluginOptionsEnum<WeatherUnitSetting.WeatherUnit> {

  @Override
  public int order() {
    return 80;
  }

  @Override
  public Class<WeatherUnit> getType() {
    return WeatherUnit.class;
  }

  public enum WeatherUnit {
    metric, imperial
  }
}
