package com.noisetide;

import com.noisetide.command.WeatherCommand;
import com.noisetide.weather.WeatherManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class DimensionLevelWeather implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger("dimension-level-weather");
    public static final WeatherManager WEATHER = new WeatherManager();

    @Override
    public void onInitialize() {
        LOGGER.info("DimensionLevelWeather initializing");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            WeatherCommand.register(dispatcher));

        ServerTickEvents.END_WORLD_TICK.register(level -> WEATHER.tick(level));

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(
            (player, origin, destination) -> WEATHER.syncPlayerOnJoin(player));

        ServerPlayConnectionEvents.JOIN.register(
            (handler, sender, server) -> WEATHER.syncPlayerOnJoin(handler.player));
    }
}
