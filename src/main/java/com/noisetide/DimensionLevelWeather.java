package com.noisetide;

import com.noisetide.command.WeatherCommand;
import com.noisetide.weather.WeatherManager;

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

public class DimensionLevelWeather implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger("dimension-level-weather");
    public static final WeatherManager WEATHER = new WeatherManager();

    @Override
    public void onInitialize() {
        LOGGER.info("DimensionLevelWeather initializing");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            WeatherCommand.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                LevelData data = level.getLevelData();
                if (data.isRaining()) {
                    WeatherManager.WeatherState state = data.isThundering()
                        ? WeatherManager.WeatherState.THUNDER
                        : WeatherManager.WeatherState.RAIN;
                    WEATHER.initFromSavedState(level.dimension(), state);
                    level.setRainLevel(1.0F);
                    if (state == WeatherManager.WeatherState.THUNDER) {
                        level.setThunderLevel(1.0F);
                    }
                    LOGGER.info("Restored weather state {} for {}",
                        state, level.dimension().identifier());
                }
            }
        });

        ServerTickEvents.END_WORLD_TICK.register(level -> WEATHER.tick(level));

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(
            (player, origin, destination) -> WEATHER.syncPlayerOnJoin(player));

        ServerPlayConnectionEvents.JOIN.register(
            (handler, sender, server) -> WEATHER.syncPlayerOnJoin(handler.player));
    }
}
