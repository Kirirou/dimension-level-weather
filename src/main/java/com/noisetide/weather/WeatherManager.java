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

    public boolean isAdvanceWeatherEnabled(ResourceKey<Level> dimension) {
        if (savedData == null) return true;
        return savedData.getAdvanceWeather(dimension);
    }

    public void setState(ResourceKey<Level> dimension, WeatherState state, ServerLevel level) {
        DimensionWeather weather = getOrCreate(dimension);

        // Persist regardless of state
        if (savedData != null) {
            savedData.setState(dimension, state);
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
            sendToAll(level, new ClientboundGameEventPacket(
                ClientboundGameEventPacket.STOP_RAINING, 0.0F));
            sendToAll(level, new ClientboundGameEventPacket(
                ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, weather.rainLevel));
            return;
        }

        weather.clearing = false;
        weather.state = state;

        applyingWeather = true;
        try {
            if (!weather.initialized) {
                weather.initialized = true;
                weather.rainLevel = 0.0F;
                level.setWeatherParameters(0, 6000,
                    true, state == WeatherState.THUNDER);
                sendToAll(level, new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.START_RAINING, 0.0F));
            } else {
                level.setWeatherParameters(0, 6000,
                    true, state == WeatherState.THUNDER);
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

        // Handle gradual ramp-down
        if (weather.clearing) {
            if (weather.rainLevel > 0.0F) {
                weather.rainLevel = Math.max(0.0F, weather.rainLevel - 0.02F);
                level.setRainLevel(weather.rainLevel);
                sendToAll(level, new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, weather.rainLevel));
            } else {
                // rainLevel is 0, clearing complete
                // do NOT send STOP_RAINING here, it would reset client to 1.0F
                weather.clearing = false;
                weather.initialized = false;
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

            if (weather.state == WeatherState.THUNDER) {
                level.setThunderLevel(weather.rainLevel); // add this line
                sendToAll(level, new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, weather.rainLevel));
            }
        }        
    }

    public void syncPlayerOnJoin(ServerPlayer player) {
        ResourceKey<Level> dimension = player.level().dimension();
        DimensionWeather weather = getOrCreate(dimension);

        if (!isRaining(dimension)) return;

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
