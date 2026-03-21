package com.noisetide;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DimensionLevelWeatherClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("biome-debug");

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::debugBiome);
    }

    private void debugBiome(Minecraft client) {
        if (client.level == null || client.player == null) return;

        long time = client.level.getGameTime();
        if (time % 100 != 0) return;

        BlockPos pos = client.player.blockPosition();
        Biome biome = client.level.getBiome(pos).value();

        boolean hasPrecip       = biome.hasPrecipitation();
        float baseTemp          = biome.getBaseTemperature();
        boolean warmEnough      = biome.warmEnoughToRain(pos, client.level.getSeaLevel());
        Biome.Precipitation p   = biome.getPrecipitationAt(pos, client.level.getSeaLevel());

        LOGGER.info(
            "dim={} pos={} | hasPrecipitation={} baseTemp={} warmEnoughToRain={} precipitationAt={}",
            client.level.dimension(),
            pos,
            hasPrecip,
            baseTemp,
            warmEnough,
            p
        );
    }
}
