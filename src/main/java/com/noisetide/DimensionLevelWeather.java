package com.noisetide;

import com.noisetide.command.TimeCommand;
import com.noisetide.command.WeatherCommand;
import com.noisetide.config.AutoCycleManager;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;

import java.nio.file.Path;

public class DimensionLevelWeather implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger("dimension-level-weather");
    public static final WeatherManager WEATHER = new WeatherManager();
    public static final AutoCycleManager AUTO_CYCLE = new AutoCycleManager();

    private static DimensionConfig config;

    public static DimensionConfig getConfig() {
        return config;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("DimensionLevelWeather initializing");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
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
                String dimId = level.dimension().identifier().toString();
                DimensionConfig.DimensionEntry entry = config.getEntry(dimId);

                // Apply fixed time if configured
                if (entry != null && entry.enabled && entry.fixed_time >= 0) {
                    level.setDayTime(entry.fixed_time);
                }

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

                } else if (entry != null && entry.enabled && !entry.auto_cycle) {
                    // Apply config default only if no saved state exists
                    WeatherManager.WeatherState defaultState;
                    try {
                        defaultState = WeatherManager.WeatherState
                            .valueOf(entry.default_state.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        defaultState = WeatherManager.WeatherState.CLEAR;
                    }

                    if (defaultState != WeatherManager.WeatherState.CLEAR) {
                        WEATHER.setState(level.dimension(), defaultState, level);
                        LOGGER.info("Applied config default {} for {}",
                            defaultState, level.dimension().identifier());
                    }

                } else if (entry != null && entry.enabled && entry.auto_cycle) {
                    AUTO_CYCLE.initCycle(level.dimension(), entry);
                }
            }
        });

        ServerTickEvents.END_WORLD_TICK.register(level -> {
            WEATHER.tick(level);

            if (config == null) return;
            String dimId = level.dimension().identifier().toString();
            DimensionConfig.DimensionEntry entry = config.getEntry(dimId);

            if (entry != null && entry.enabled && entry.auto_cycle) {
                AUTO_CYCLE.tick(level, entry);
            }

            // Enforce fixed time every tick
            if (entry != null && entry.enabled && entry.fixed_time >= 0) {
                level.setDayTime(entry.fixed_time);
            }
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(
            (player, origin, destination) -> WEATHER.syncPlayerOnJoin(player));

        ServerPlayConnectionEvents.JOIN.register(
            (handler, sender, server) -> WEATHER.syncPlayerOnJoin(handler.player));
    }
}
