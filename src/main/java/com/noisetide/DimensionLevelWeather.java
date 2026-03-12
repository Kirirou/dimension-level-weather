package com.noisetide;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class DimensionLevelWeather implements ModInitializer {
    private float endRainLevel = 0.0F;
    private boolean endRaining = false;

    private static final Logger LOGGER = LogManager.getLogger("dimension-level-weather");

    @Override
    public void onInitialize() {
        LOGGER.info("DimensionLevelWeather initializing");

        ServerTickEvents.END_WORLD_TICK.register(this::logWorldWeather);
    }
    private void logWorldWeather(ServerLevel level) {
        if (level.dimension() != Level.END) return;

        long time = level.getGameTime();
        LevelData data = level.getLevelData();

        // Force server rain if not raining
        if (!data.isRaining()) {
            level.setWeatherParameters(0, 6000, true, false);
        }

        // Transition start
        if (!endRaining) {
            endRaining = true;
            endRainLevel = 0.0F;

            ClientboundGameEventPacket start =
                new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.START_RAINING,
                    0.0F
                );

            for (ServerPlayer player : level.players()) {
                player.connection.send(start);
            }
        }

        // Gradual ramp every tick
        if (endRainLevel < 1.0F) {
            endRainLevel += 0.02F; // controls speed
            if (endRainLevel > 1.0F) {
                endRainLevel = 1.0F;
            }

            ClientboundGameEventPacket levelChange =
                new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,
                    endRainLevel
                );

            for (ServerPlayer player : level.players()) {
                player.connection.send(levelChange);
            }
        }
    }
}
