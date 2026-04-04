package com.noisetide.weather;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class WeatherManager {

    private net.minecraft.server.MinecraftServer server = null;
    private WeatherSavedData savedData = null;

    private static final org.apache.logging.log4j.Logger LOGGER =
        org.apache.logging.log4j.LogManager.getLogger("dimension-level-weather");

    public void setSavedData(WeatherSavedData savedData, net.minecraft.server.MinecraftServer server) {
        this.savedData = savedData;
        this.server = server;
    }

    public WeatherSavedData getSavedData() {
        return savedData;
    }

    public enum WeatherState {
        CLEAR, RAIN, THUNDER
    }

    private static final class DimensionWeather {
        WeatherState state = WeatherState.CLEAR;
        float rainLevel = 0.0F;
        boolean initialized = false;
        boolean clearing = false;
    }

    private final Map<ResourceKey<Level>, DimensionWeather> dimensions = new HashMap<>();
    private boolean applyingWeather = false;

    public boolean isApplyingWeather() {
        return applyingWeather;
    }

    private DimensionWeather getOrCreate(ResourceKey<Level> dimension) {
        return dimensions.computeIfAbsent(dimension, k -> new DimensionWeather());
    }

    public WeatherState getState(ResourceKey<Level> dimension) {
        return getOrCreate(dimension).state;
    }

    public float getRainLevel(ResourceKey<Level> dimension) {
        return getOrCreate(dimension).rainLevel;
    }

    public boolean isRaining(ResourceKey<Level> dimension) {
        DimensionWeather w = getOrCreate(dimension);
        return (w.state == WeatherState.RAIN || w.state == WeatherState.THUNDER)
            && !w.clearing;
    }

    public boolean isThundering(ResourceKey<Level> dimension) {
        DimensionWeather w = getOrCreate(dimension);
        return w.state == WeatherState.THUNDER && !w.clearing;
    }

    public boolean isClearing(ResourceKey<Level> dimension) {
        return getOrCreate(dimension).clearing;
    }

    public boolean isAdvanceWeatherEnabled(ResourceKey<Level> dimension) {
        if (savedData == null) return true;
        return savedData.getAdvanceWeather(dimension);
    }

    public void setState(ResourceKey<Level> dimension, WeatherState state, ServerLevel level) {
        DimensionWeather weather = getOrCreate(dimension);
        LOGGER.info("[WEATHER SET] {} -> {} (was={} clearing={} rainLevel={})",
            dimension.identifier(), state,
            weather.state, weather.clearing, weather.rainLevel);

        if (savedData != null) {
            savedData.setState(dimension, state);
            if (server != null) {
                server.overworld().getDataStorage().saveAndJoin();
            }
        }

        if (state == WeatherState.CLEAR) {
            weather.clearing = true;
            weather.state = WeatherState.CLEAR;
            applyingWeather = true;
            try {
                level.setWeatherParameters(6000, 0, false, false);
            } finally {
                applyingWeather = false;
            }
            // Do not send STOP_RAINING, it resets client to 1.0F
            // Just let the tick ramp down via RAIN_LEVEL_CHANGE
            return;
        }

        weather.clearing = false;
        weather.state = state;

        applyingWeather = true;
        try {
            if (!weather.initialized) {
                weather.initialized = true;
                weather.rainLevel = 0.0F;
                level.setWeatherParameters(0, 6000, true, state == WeatherState.THUNDER);
                sendToAll(level, new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.START_RAINING, 0.0F));
            } else {
                level.setWeatherParameters(0, 6000, true, state == WeatherState.THUNDER);
                // If already at full rain and switching to thunder,
                // ramp-up won't run so send thunder level immediately
                if (weather.rainLevel >= 1.0F && state == WeatherState.THUNDER) {
                    level.setThunderLevel(1.0F);
                    sendToAll(level, new ClientboundGameEventPacket(
                        ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 1.0F));
                }
                // If switching away from thunder back to rain, clear thunder level
                if (state == WeatherState.RAIN) {
                    level.setThunderLevel(0.0F);
                    sendToAll(level, new ClientboundGameEventPacket(
                        ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0.0F));
                }
            }
        } finally {
            applyingWeather = false;
        }
    }

    public void clearFromVanilla(ServerLevel level) {
        DimensionWeather weather = getOrCreate(level.dimension());
        weather.clearing = true;
        weather.state = WeatherState.CLEAR;
        if (savedData != null) {
            savedData.setState(level.dimension(), WeatherState.CLEAR);
        }
    }

    public void tick(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        DimensionWeather weather = getOrCreate(dimension);

        if (weather.clearing) {
            if (weather.rainLevel > 0.0F) {
                weather.rainLevel = Math.max(0.0F, weather.rainLevel - 0.02F);
                level.setRainLevel(weather.rainLevel);
                sendToAll(level, new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, weather.rainLevel));
                LOGGER.info("[SERVER] {} clearing rainLevel={}",
                    dimension.identifier(), weather.rainLevel);
            } else {
                weather.clearing = false;
                weather.initialized = false;
                sendToAll(level, new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 0.0F));
                sendToAll(level, new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0.0F));
            }
            return;
        }

        if (weather.state == WeatherState.CLEAR) return;

        applyingWeather = true;
        try {
            if (!level.getLevelData().isRaining()) {
                level.setWeatherParameters(0, 6000,
                    true, weather.state == WeatherState.THUNDER);
            }
        } finally {
            applyingWeather = false;
        }

        if (weather.rainLevel < 1.0F) {
            weather.rainLevel = Math.min(1.0F, weather.rainLevel + 0.02F);
            level.setRainLevel(weather.rainLevel);
            sendToAll(level, new ClientboundGameEventPacket(
                ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, weather.rainLevel));
            LOGGER.info("[SERVER] {} ramping rainLevel={}",
                dimension.identifier(), weather.rainLevel);
            if (weather.state == WeatherState.THUNDER) {
                level.setThunderLevel(weather.rainLevel);
                sendToAll(level, new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, weather.rainLevel));
            }
        }
    }

    public void syncPlayerOnJoin(ServerPlayer player) {
        ResourceKey<Level> dimension = player.level().dimension();
        DimensionWeather weather = getOrCreate(dimension);
        LOGGER.info("[SYNC JOIN] player={} dim={} isRaining={} rainLevel={} clearing={}",
            player.getScoreboardName(),
            dimension.identifier(),
            isRaining(dimension),
            weather.rainLevel,
            weather.clearing);

        if (!isRaining(dimension)) {
            // Reset client to zero without sending STOP_RAINING
            // which would incorrectly set client rainLevel to 1.0F
            player.connection.send(new ClientboundGameEventPacket(
                ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 0.0F));
            player.connection.send(new ClientboundGameEventPacket(
                ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0.0F));
            return;
        }

        player.connection.send(new ClientboundGameEventPacket(
            ClientboundGameEventPacket.START_RAINING, 0.0F));
        player.connection.send(new ClientboundGameEventPacket(
            ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, weather.rainLevel));
        if (weather.state == WeatherState.THUNDER) {
            player.connection.send(new ClientboundGameEventPacket(
                ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, weather.rainLevel));
        }
    }

    public Map<ResourceKey<Level>, WeatherState> getAllStates() {
        Map<ResourceKey<Level>, WeatherState> result = new HashMap<>();
        dimensions.forEach((key, value) -> result.put(key, value.state));
        return result;
    }

    private void sendToAll(ServerLevel level, ClientboundGameEventPacket packet) {
        for (ServerPlayer player : level.players()) {
            LOGGER.info("[PACKET] dim={} player={} type={} value={}",
                level.dimension().identifier(),
                player.getScoreboardName(),
                packet.getEvent(),
                packet.getParam());
            player.connection.send(packet);
        }
    }

    public void initFromSavedState(ResourceKey<Level> dimension, WeatherState state) {
        DimensionWeather weather = getOrCreate(dimension);
        weather.state = state;
        weather.rainLevel = 1.0F;
        weather.initialized = true;
        weather.clearing = false;
    }
}
