package org.homio.bundle.weather.setting;

import org.homio.bundle.api.setting.SettingPluginOptionsEnum;
import org.homio.bundle.api.util.Lang;

public class WeatherLangSetting implements SettingPluginOptionsEnum<Lang> {

  @Override
  public Class<Lang> getType() {
    return Lang.class;
  }

  @Override
  public int order() {
    return 60;
  }
}
