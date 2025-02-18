package org.homio.addon.openweathermap;

import static org.homio.api.ui.field.action.v1.item.UITextInputItemBuilder.InputType.Text;

import jakarta.persistence.Entity;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.homio.api.Context;
import org.homio.api.ContextVar;
import org.homio.api.entity.BaseEntity;
import org.homio.api.entity.HasJsonData;
import org.homio.api.model.ActionResponseModel;
import org.homio.api.model.Icon;
import org.homio.api.model.JSON;
import org.homio.api.model.OptionModel;
import org.homio.api.model.UpdatableValue;
import org.homio.api.service.WeatherEntity;
import org.homio.api.ui.UISidebarChildren;
import org.homio.api.ui.field.UIField;
import org.homio.api.ui.field.UIFieldGroup;
import org.homio.api.ui.field.UIFieldSlider;
import org.homio.api.ui.field.action.HasDynamicContextMenuActions;
import org.homio.api.ui.field.action.HasDynamicUIFields;
import org.homio.api.ui.field.action.v1.UIInputBuilder;
import org.homio.api.ui.field.action.v1.layout.dialog.UIDialogLayoutBuilder;
import org.homio.api.ui.field.selection.dynamic.DynamicOptionLoader;
import org.homio.api.util.CommonUtils;
import org.homio.api.util.Lang;
import org.homio.api.util.SecureString;
import org.homio.api.widget.CustomWidgetDataStore;
import org.homio.api.widget.HasCustomWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

@SuppressWarnings({"JpaAttributeTypeInspection", "JpaAttributeMemberSignatureInspection", "unused"})
@Entity
@UISidebarChildren(icon = "fas fa-cloud-bolt", color = "#84B380", maxAllowCreateItem = 1)
public class OpenWeatherEntity extends WeatherEntity<OpenWeatherService>
    implements HasDynamicContextMenuActions, HasCustomWidget {

  public static final String WEATHER_PROVIDER = "weatherProvider";

  private static void configureDialog(UIDialogLayoutBuilder dialogBuilder) {
    dialogBuilder.addFlex(
        "main",
        flex -> {
          flex.addInput("name", "OpenWeather " + WeatherInfoType.Temperature.name(), Text, true);
          flex.addIconPicker("icon", "fas fa-cloud");
          flex.addColorPicker("color", "#999999");
          flex.addSelectBox("type")
              .setOptions(OptionModel.enumList(WeatherInfoType.class))
              .setValue(WeatherInfoType.Temperature.name());
          flex.addTextInput("unit", "°C", false);
          flex.addTextInput("city", "London", true);
          flex.addSelectBox("group").setLazyVariableGroup().setRequired(true);
        });
  }

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

  @UIField(order = 2)
  @UIFieldGroup("MISC")
  public WeatherUnit getUnit() {
    return getJsonDataEnum("unit", WeatherUnit.metric);
  }

  public void setUnit(String value) {
    setJsonData("unit", value);
  }

  @UIField(order = 4)
  @UIFieldGroup("MISC")
  @UIFieldSlider(min = 1, max = 60, header = "min")
  public int getRefreshRate() {
    return getJsonData("rate", 10);
  }

  public void setRefreshRate(int value) {
    setJsonData("rate", value);
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
  public @NotNull Class<OpenWeatherService> getEntityServiceItemClass() {
    return OpenWeatherService.class;
  }

  @Override
  public @Nullable OpenWeatherService createService(@NotNull Context context) {
    return new OpenWeatherService(context, this);
  }

  @Override
  protected @NotNull String getDevicePrefix() {
    return "owm";
  }

  @Override
  public void assembleActions(UIInputBuilder uiInputBuilder) {
    uiInputBuilder
        .addOpenDialogSelectableButton(
            "ADD_WEATHER_VARIABLE",
            new Icon("fas fa-square-root-variable", "#479BB3"),
            (context, params) -> {
              String city = params.getString("city");
              String groupId = params.getString("group");
              // ensure city exists
              context.network().getCityGeolocation(city);
              createVariables(params, context, groupId, city);
              return ActionResponseModel.success();
            })
        .editDialog(OpenWeatherEntity::configureDialog);

    uiInputBuilder
        .addOpenDialogSelectableButton(
            "CREATE_WEATHER_WIDGET", new Icon("fas fa-sun", "#B3A729"), this::createWeatherWidget)
        .editDialog(
            builder ->
                builder.addFlex(
                    "main",
                    flex -> {
                      flex.addInput("city", "London", Text, true);
                      flex.addSelectBoxWidgetTab(context());
                    }));
  }

  private @NotNull ActionResponseModel createWeatherWidget(Context context, JSONObject params) {
    createWeatherWidget(context, params.getString("tab"), params.getString("city"));
    getOrCreateService(context()).ifPresent(ServiceInstance::restartService);
    return ActionResponseModel.success();
  }

  private void createVariables(JSONObject params, Context context, String groupId, String city) {
    context
        .var()
        .createVariable(
            groupId,
            String.valueOf(params.getString("city").hashCode()),
            params.getString("name"),
            ContextVar.VariableType.Float,
            builder ->
                builder
                    .setUnit(params.getString("unit"))
                    .set(WEATHER_PROVIDER, getEntityID())
                    .set("city", city)
                    .set("type", params.getString("type"))
                    .setIcon(
                        new Icon(
                            params.getString("icon"),
                            StringUtils.defaultIfEmpty(params.optString("color"), "#999999")))
                    .setNumberRange(-50, 50));
  }

  @Override
  public void assembleUIFields(
      HasDynamicUIFields.@NotNull UIFieldBuilder uiFieldBuilder,
      @NotNull HasJsonData sourceEntity) {
    var city = UpdatableValue.wrap(sourceEntity, "London", "city");
    uiFieldBuilder.addInput(1, city);
  }

  @Override
  public void setWidgetDataStore(
      @NotNull CustomWidgetDataStore customWidgetDataStore,
      @NotNull String widgetEntityID,
      @NotNull JSON widgetData) {
    getService().setWidgetDataStore(customWidgetDataStore, widgetEntityID, widgetData);
  }

  @Override
  public void removeWidgetDataStore(@NotNull String widgetEntityID) {
    getService().removeWidgetDataStore(widgetEntityID);
  }

  @Override
  public @NotNull BaseEntity createWidget(@NotNull Context context, @NotNull String name, @NotNull String tabId, int width, int height) {
    return createWeatherWidget(context(), tabId, name);
  }

  @Override
  public @Nullable Map<String, Icon> getAvailableWidgets() {
    return Map.of("OpenWeather", new Icon("fas fa-sun", "#B3A729"));
  }

  private @NotNull BaseEntity createWeatherWidget(Context context, String tab, String city) {
    return context
      .widget()
      .createCustomWidget(
        getEntityID(),
        tab,
        builder ->
          builder
            .setValue("city", city)
            .code(CommonUtils.readFile("code.js"))
            .css(CommonUtils.readFile("style.css"))
            .parameterEntity(getEntityID()));
  }

  public enum WeatherUnit {
    metric,
    imperial
  }

  public static class SelectTab implements DynamicOptionLoader {

    @Override
    public List<OptionModel> loadOptions(DynamicOptionLoaderParameters parameters) {
      return parameters.context().widget().getDashboardTabs();
    }
  }
}
