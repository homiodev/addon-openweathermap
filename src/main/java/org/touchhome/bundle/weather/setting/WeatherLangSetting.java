package org.touchhome.bundle.weather.setting;

import org.touchhome.bundle.api.setting.SettingPluginOptionsEnum;
import org.touchhome.bundle.api.util.Lang;

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
