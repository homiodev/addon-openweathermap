package org.touchhome.bundle.weather.workspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.api.state.DecimalType;
import org.touchhome.bundle.api.state.ObjectType;
import org.touchhome.bundle.api.state.State;
import org.touchhome.bundle.api.workspace.WorkspaceBlock;
import org.touchhome.bundle.api.workspace.scratch.ArgumentType;
import org.touchhome.bundle.api.workspace.scratch.Scratch3Block;
import org.touchhome.bundle.api.workspace.scratch.Scratch3ExtensionBlocks;
import org.touchhome.bundle.weather.WeatherBundleEntrypoint;
import org.touchhome.bundle.weather.setting.WeatherProviderSetting;

@Getter
@Component
public class Scratch3WeatherBlocks extends Scratch3ExtensionBlocks {

  public static final String CITY = "CITY";

  @JsonIgnore
  private final Scratch3Block weatherApi;

  @JsonIgnore
  private final Scratch3Block temperatureWeatherApi;
  private final Scratch3Block humidityWeatherApi;
  private final Scratch3Block pressureWeatherApi;

  public Scratch3WeatherBlocks(EntityContext entityContext, WeatherBundleEntrypoint weatherBundleEntrypoint) {
    super("#3B798C", entityContext, weatherBundleEntrypoint);

    this.weatherApi = blockReporter(11, "weather", "Weather of city [CITY] (JSON)", this::readWeather);
    this.weatherApi.addArgument(CITY, ArgumentType.string);

    this.temperatureWeatherApi =
        blockReporter(22, "weather_temp", "Weather temp of city [CITY]", this::readWeatherTemperature);
    this.temperatureWeatherApi.addArgument(CITY, ArgumentType.string);

    this.humidityWeatherApi =
        blockReporter(33, "weather_humidity", "Weather humidity of city [CITY]", this::readWeatherHumidity);
    this.humidityWeatherApi.addArgument(CITY, ArgumentType.string);

    this.pressureWeatherApi =
        blockReporter(44, "weather_pressure", "Weather pressure of city [CITY]", this::readWeatherPressure);
    this.pressureWeatherApi.addArgument(CITY, ArgumentType.string);
  }

  private State readWeatherTemperature(WorkspaceBlock workspaceBlock) {
    return new DecimalType(entityContext.setting().getValue(WeatherProviderSetting.class)
        .readWeatherTemperature(workspaceBlock.getInputString(CITY)));
  }

  private State readWeatherPressure(WorkspaceBlock workspaceBlock) {
    return new DecimalType(entityContext.setting().getValue(WeatherProviderSetting.class)
        .readWeatherPressure(workspaceBlock.getInputString(CITY)));
  }

  private State readWeatherHumidity(WorkspaceBlock workspaceBlock) {
    return new DecimalType(entityContext.setting().getValue(WeatherProviderSetting.class)
        .readWeatherHumidity(workspaceBlock.getInputString(CITY)));
  }

  private State readWeather(WorkspaceBlock workspaceBlock) {
    return new ObjectType(entityContext.setting().getValue(WeatherProviderSetting.class));
  }
}
