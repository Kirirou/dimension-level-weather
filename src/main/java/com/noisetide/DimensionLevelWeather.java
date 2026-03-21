package com.noisetide;

import com.noisetide.command.TimeCommand;
import com.noisetide.command.WeatherCommand;
import com.noisetide.config.AdvanceWeatherManager;
import com.noisetide.config.DimensionConfig;
import com.noisetide.weather.WeatherManager;
import com.noisetide.weather.WeatherSavedData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;

import java.nio.file.Path;

public class DimensionLevelWeather implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger("dimension-level-weather");
    public static final WeatherManager WEATHER = new WeatherManager();
    public static final AdvanceWeatherManager ADVANCE_WEATHER = new AdvanceWeatherManager();

    private static DimensionConfig config;

    public static DimensionConfig getConfig() {
        return config;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("DimensionLevelWeather initializing");

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
                LevelData data = level.getLevelData();

                // Restore saved weather state
                WeatherManager.WeatherState savedState =
                    savedData.getState(level.dimension());

                if (savedState != WeatherManager.WeatherState.CLEAR) {
                    WEATHER.initFromSavedState(level.dimension(), savedState);
                    level.setRainLevel(1.0F);
                    level.setWeatherParameters(0, 6000,
                        true, savedState == WeatherManager.WeatherState.THUNDER);
                    if (savedState == WeatherManager.WeatherState.THUNDER) {
                        level.setThunderLevel(1.0F);
                    }
                    LOGGER.info("Restored weather state {} for {}",
                        savedState, level.dimension().identifier());
                }

                // Init cycle timer for all dimensions
                // AutoCycleManager will gate on advance_weather per tick
                DimensionConfig.DimensionEntry entry =
                    config.getEntry(level.dimension().identifier().toString());
                ADVANCE_WEATHER.initCycle(level.dimension(), entry);

                // Restore saved advance_time
                if (!savedData.getAdvanceTime(level.dimension())) {
                    LOGGER.info("Time advance disabled for {}",
                        level.dimension().identifier());
                }
            }
        });

        ServerTickEvents.END_WORLD_TICK.register(level -> {
            WEATHER.tick(level);
            ADVANCE_WEATHER.tick(level);

            // Enforce time freeze
            if (WEATHER.getSavedData() == null) return;
            if (!WEATHER.getSavedData().getAdvanceTime(level.dimension())) {
                // Time is managed by ServerLevelMixin blockTimeAdvance
            }
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(
            (player, origin, destination) -> WEATHER.syncPlayerOnJoin(player));

        ServerPlayConnectionEvents.JOIN.register(
            (handler, sender, server) -> WEATHER.syncPlayerOnJoin(handler.player));
    }
}
