package org.homio.addon.openweathermap;

import jakarta.persistence.Entity;
import java.util.Set;
import org.homio.api.Context;
import org.homio.api.service.WeatherEntity;
import org.homio.api.ui.UISidebarChildren;
import org.homio.api.ui.field.UIField;
import org.homio.api.ui.field.UIFieldGroup;
import org.homio.api.util.Lang;
import org.homio.api.util.SecureString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"JpaAttributeTypeInspection", "JpaAttributeMemberSignatureInspection"})
@Entity
@UISidebarChildren(icon = "fas fa-cloud-bolt", color = "#84B380")
public class OpenWeatherMapEntity extends WeatherEntity<OpenWeatherMapService> {

  @Override
  public String getDescriptionImpl() {
    if (getApiToken().isEmpty()) {
      return Lang.getServerMessage("OPENWEATHERMAP.DESCRIPTION");
    }
    return null;
  }

  @UIField(order = 1, required = true, inlineEditWhenEmpty = true)
  @UIFieldGroup(value = "SECURITY", order = 100, borderColor = "#A4405B")
  public SecureString getApiToken() {
    return getJsonSecure("apiToken");
  }

  public void setApiToken(String value) {
    setJsonData("apiToken", value);
  }

  @UIField(order = 1)
  @UIFieldGroup(value = "MISC", order = 120, borderColor = "2C838B")
  public Lang getLang() {
    return getJsonDataEnum("lang", Lang.en);
  }

  public void setLang(String value) {
    setJsonData("lang", value);
  }

  @UIField(order = 2)
  @UIFieldGroup("MISC")
  public WeatherUnit getUnit() {
    return getJsonDataEnum("unit", WeatherUnit.metric);
  }

  public void setUnit(String value) {
    setJsonData("unit", value);
  }

  @Override
  public String getDefaultName() {
    return "OpenWeatherMap";
  }

  @Override
  public @Nullable Set<String> getConfigurationErrors() {
    if (getApiToken().isEmpty()) {
      return Set.of("ERROR.NO_API_TOKEN");
    }
    return null;
  }

  @Override
  public long getEntityServiceHashCode() {
    return getJsonDataHashCode("apiToken");
  }

  @Override
  public @NotNull Class<OpenWeatherMapService> getEntityServiceItemClass() {
    return OpenWeatherMapService.class;
  }

  @Override
  public @Nullable OpenWeatherMapService createService(@NotNull Context context) {
    return new OpenWeatherMapService(context, this);
  }

  @Override
  protected @NotNull String getDevicePrefix() {
    return "owm";
  }

  public enum WeatherUnit {
    metric, imperial
  }
}
