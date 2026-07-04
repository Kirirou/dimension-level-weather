package com.kyryro.dimensionlevelweather;

import com.kyryro.dimensionlevelweather.command.TimeCommand;
import com.kyryro.dimensionlevelweather.command.WeatherCommand;
import com.kyryro.dimensionlevelweather.config.AdvanceTimeManager;
import com.kyryro.dimensionlevelweather.config.AdvanceWeatherManager;
import com.kyryro.dimensionlevelweather.config.DimensionConfig;
import com.kyryro.dimensionlevelweather.network.WaterEvaporatesPayload;
import com.kyryro.dimensionlevelweather.weather.WeatherManager;
import com.kyryro.dimensionlevelweather.weather.WeatherSavedData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.server.level.ServerLevel;

import java.nio.file.Path;

public class DimensionLevelWeather implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger("dimension-level-weather");
    public static final WeatherManager WEATHER = new WeatherManager();
    public static final AdvanceWeatherManager ADVANCE_WEATHER = new AdvanceWeatherManager();
    public static final AdvanceTimeManager ADVANCE_TIME = new AdvanceTimeManager();

    private static DimensionConfig config;

    public static DimensionConfig getConfig() {
        return config;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("DimensionLevelWeather initializing");

        PayloadTypeRegistry.clientboundPlay().register(WaterEvaporatesPayload.TYPE, WaterEvaporatesPayload.STREAM_CODEC);

        CommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess, environment) -> {
                WeatherCommand.register(dispatcher);
                TimeCommand.register(dispatcher);
            });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            Path gameDir = server.getServerDirectory();
            config = DimensionConfig.loadOrCreate(gameDir);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            WeatherSavedData savedData = WeatherSavedData.getOrCreate(server);
            WEATHER.setSavedData(savedData, server);

            for (ServerLevel level : server.getAllLevels()) {
                // Restore saved weather state
                WeatherManager.WeatherState savedState =
                    savedData.getState(level.dimension());

                if (savedState != WeatherManager.WeatherState.CLEAR) {
                    WEATHER.initFromSavedState(level.dimension(), savedState);
                    level.setRainLevel(1.0F);
                    net.minecraft.world.level.saveddata.WeatherData wd = level.getWeatherData();
                    wd.setRaining(true);
                    wd.setRainTime(6000);
                    wd.setClearWeatherTime(0);
                    if (savedState == WeatherManager.WeatherState.THUNDER) {
                        level.setThunderLevel(1.0F);
                        wd.setThundering(true);
                        wd.setThunderTime(6000);
                    }
                    wd.setDirty();
                    LOGGER.info("Restored weather state {} for {}",
                        savedState, level.dimension().identifier());
                }

                // Init cycle timer for all dimensions
                // AutoCycleManager will gate on advance_weather per tick
                DimensionConfig.DimensionEntry entry =
                    config.getEntry(level.dimension().identifier().toString());
                ADVANCE_WEATHER.initCycle(level.dimension(), entry);

                // Restore saved advance_time via clock manager — always set both states
                boolean shouldAdvance = savedData.getAdvanceTime(level.dimension());
                level.dimensionType().defaultClock().ifPresent(clock ->
                    level.clockManager().setPaused(clock, !shouldAdvance));
                LOGGER.info("Time advance {} for {}",
                    shouldAdvance ? "enabled" : "disabled",
                    level.dimension().identifier());
            }
        });

        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            WEATHER.tick(level);
            ADVANCE_WEATHER.tick(level);
            ADVANCE_TIME.tick(level);
        });

        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
            (player, origin, destination) -> WEATHER.syncPlayerOnJoin(player));

        ServerPlayConnectionEvents.JOIN.register(
            (handler, sender, server) -> {
                WEATHER.syncPlayerOnJoin(handler.player);
                WEATHER.sendWaterEvaporatesSync(handler.player);
            });
    }
}
