package org.homio.addon.openweathermap.widget;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.homio.addon.openweathermap.OpenWeatherMapEntity;
import org.homio.api.Context;
import org.homio.api.entity.BaseEntity;
import org.homio.api.entity.types.CommunicationEntity;
import org.homio.api.ui.field.UIField;
import org.homio.api.ui.field.UIFieldLinkToEntity;
import org.homio.api.ui.field.UIFieldNumber;
import org.homio.api.ui.field.UIFieldType;
import org.homio.api.ui.field.selection.UIFieldEntityByClassSelection;
import org.homio.api.util.DataSourceUtil;
import org.homio.api.util.DataSourceUtil.SelectionSource;
import org.homio.api.widget.JavaScriptBuilder;
import org.homio.api.widget.JavaScriptBuilder.JSWindow;
import org.homio.hquery.hardware.network.NetworkHardwareRepository;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
//@Component // NOT WORKS
@RequiredArgsConstructor
public class OpenWeatherWidgetTemplate {

  private final Context context;
  private final NetworkHardwareRepository networkHardwareRepository;
  @UIField(order = 3, required = true)
  @UIFieldEntityByClassSelection(OpenWeatherMapEntity.class)
  @UIFieldLinkToEntity(value = CommunicationEntity.class, applyTitle = true)
  public String weatherEntity;
  @UIField(order = 1, type = UIFieldType.Slider, label = "weatherType")
  @UIFieldNumber(min = 1, max = 24)
  private int id = 15;
  @UIField(order = 2)
  private String city;

 /* @Override
  public @NotNull Icon getIcon() {
    return new Icon("fas fa-snowflake");
  }*/

  /*@Override
  public @NotNull ParentWidget getParent() {
    return ParentWidget.Weather;
  }*/

  // @Override
  /*public void entityUpdated(ObjectNode params) {
    javaScriptBuilder.jsonParam()
  }*/

  @SneakyThrows
  public void createWidget(JavaScriptBuilder javaScriptBuilder) {
    javaScriptBuilder.css("widget-left", "margin: 0 !important;");
    javaScriptBuilder.setJsonReadOnly();
    String containerId = "cow-" + System.currentTimeMillis();
    javaScriptBuilder
        .jsonParam("id", "15")
        .jsonParam("city_name", networkHardwareRepository.getIpGeoLocation(
            networkHardwareRepository.getOuterIpAddress()).get("city").asText());

    BaseEntity baseEntity;
    if (StringUtils.isNotEmpty(weatherEntity)) {
      SelectionSource selection = DataSourceUtil.getSelection(weatherEntity);
      baseEntity = selection.getValue(context);
    } else {baseEntity = null;}
    javaScriptBuilder.readyOnClient().window(window -> configureWindow(baseEntity, window, containerId))
                     .addGlobalScript("//openweathermap.org/themes/openweathermap/assets/vendor/owm/js/weather-widget-generator.js");

    javaScriptBuilder.jsContent().div(style -> style.id(containerId), div -> {
    });
  }

  private static void configureWindow(@Nullable BaseEntity entity, JSWindow window, String containerId) {
    window.array("myWidgetParam")
          .value("id", "${id}")
          .value("city_name", "${city_name}")
          .value("containerid", containerId)
          .value("units", (JavaScriptBuilder.ProxyEntityContextValue) entityContext -> {
            if (entity instanceof OpenWeatherMapEntity owm) {
              owm.getUnit();
            }
          })
          .value("appid", (JavaScriptBuilder.ProxyEntityContextValue) entityContext -> {
            if (entity instanceof OpenWeatherMapEntity owm) {
              owm.getApiToken();
            }
          });
  }
}
