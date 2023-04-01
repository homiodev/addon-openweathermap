package org.homio.bundle.weather.setting;

import org.homio.bundle.api.setting.SettingPluginText;

public class WeatherApiKeySetting implements SettingPluginText {

  @Override
  public int order() {
    return 10;
  }
}
