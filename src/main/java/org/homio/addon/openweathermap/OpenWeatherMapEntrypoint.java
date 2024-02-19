package org.homio.addon.openweathermap;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.homio.api.AddonEntrypoint;
import org.homio.api.Context;
import org.homio.api.ContextVar.VariableType;
import org.homio.api.model.ActionResponseModel;
import org.homio.api.model.Icon;
import org.homio.api.model.OptionModel;
import org.homio.api.service.WeatherEntity;
import org.homio.api.service.WeatherEntity.WeatherInfoType;
import org.homio.api.util.Lang;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class OpenWeatherMapEntrypoint implements AddonEntrypoint {

    private final Context context;

    @Override
  public void init() {
        String groupId = context.var().getMiscGroup();
        context.ui().addItemContextMenu(groupId, "weather",
            uiInputBuilder -> uiInputBuilder.addOpenDialogSelectableButton("ADD_WEATHER_VARIABLE", new Icon("fas fa-cloud"), null,
                (context1, params) -> {
                    String city = params.getString("city");
                    // ensure city exists
                    context.hardware().network().getCityGeolocation(city);
                    context.var().createVariable(groupId, String.valueOf(params.getString("city").hashCode()),
                        Lang.getServerMessage("OPEN_WEATHER_VAR_NAME", city),
                        VariableType.Float, builder -> builder
                            .setUnit(params.getString("unit"))
                            .set("weatherProvider", params.getString("provider"))
                            .set("city", city)
                            .set("type", params.getString("type"))
                            .setIcon(new Icon(params.getString("icon"),
                                StringUtils.defaultIfEmpty(params.optString("color"), "#999999")))
                            .setNumberRange(-50, 50));
                    return ActionResponseModel.success();
                }).editDialog(dialogBuilder -> dialogBuilder.addFlex("main", flex -> {
                flex.addIconPicker("icon", "fas fa-cloud");
                flex.addColorPicker("color", "#999999");
                flex.addSelectBox("type").setOptions(OptionModel.enumList(WeatherInfoType.class))
                    .setValue(WeatherInfoType.Temperature.name());
                flex.addTextInput("unit", "°C", false);
                flex.addTextInput("city", "London", true);
                flex.addSelectBox("provider").setLazyItemOptions(WeatherEntity.class).setRequired(true);
            })));

  }
}
