package com.kyryro.dimensionlevelweather.weather;

import com.kyryro.dimensionlevelweather.network.WaterEvaporatesPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.world.level.Level;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class WeatherManager {

    private static final Logger LOGGER = LogManager.getLogger("dimension-level-weather");
    private static final boolean DEBUG = false;

    private MinecraftServer server = null;
    private WeatherSavedData savedData = null;
    private final Map<ResourceKey<Level>, Boolean> clientWaterEvaporates = new HashMap<>();

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

    public void setSavedData(WeatherSavedData savedData, MinecraftServer server) {
        this.savedData = savedData;
        this.server = server;
    }

    public WeatherSavedData getSavedData() {
        return savedData;
    }

    public void setClientWaterEvaporates(Map<ResourceKey<Level>, Boolean> map) {
        clientWaterEvaporates.clear();
        clientWaterEvaporates.putAll(map);
    }

    public Optional<Boolean> getWaterEvaporates(ResourceKey<Level> dimension) {
        if (clientWaterEvaporates.containsKey(dimension)) {
            return Optional.of(clientWaterEvaporates.get(dimension));
        }
        if (savedData != null) {
            return savedData.getWaterEvaporates(dimension);
        }
        return Optional.empty();
    }

    public void sendWaterEvaporatesSync(ServerPlayer player) {
        if (savedData == null) return;
        ServerPlayNetworking.send(player, new WaterEvaporatesPayload(savedData.getWaterEvaporatesSnapshot()));
    }

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
        return (w.state == WeatherState.RAIN || w.state == WeatherState.THUNDER) && !w.clearing;
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
        if (DEBUG) LOGGER.info("[WEATHER SET] {} -> {} (was={} clearing={} rainLevel={})",
            dimension.identifier(), state, weather.state, weather.clearing, weather.rainLevel);

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
                WeatherData wd = level.getWeatherData();
                wd.setClearWeatherTime(6000);
                wd.setRainTime(0);
                wd.setRaining(false);
                wd.setThundering(false);
                wd.setDirty();
            } finally {
                applyingWeather = false;
            }
            return;
        }

        weather.clearing = false;
        weather.state = state;

        applyingWeather = true;
        try {
            WeatherData wd = level.getWeatherData();
            wd.setClearWeatherTime(0);
            wd.setRainTime(6000);
            wd.setRaining(true);
            wd.setThundering(state == WeatherState.THUNDER);
            if (state == WeatherState.THUNDER) wd.setThunderTime(6000);
            wd.setDirty();
            if (!weather.initialized) {
                weather.initialized = true;
                weather.rainLevel = 0.0F;
                sendToAll(level, new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.START_RAINING, 0.0F));
            } else {
                if (weather.rainLevel >= 1.0F && state == WeatherState.THUNDER) {
                    level.setThunderLevel(1.0F);
                    sendToAll(level, new ClientboundGameEventPacket(
                        ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 1.0F));
                }
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
                float prev = weather.rainLevel;
                weather.rainLevel = Math.max(0.0F, weather.rainLevel - 0.02F);
                level.setRainLevel(weather.rainLevel);
                sendToAll(level, new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, weather.rainLevel));
                if (prev >= 1.0F) {
                    LOGGER.info("{} clearing rainLevel={}", dimension.identifier(), weather.rainLevel);
                }
            } else {
                weather.clearing = false;
                weather.initialized = false;
                LOGGER.info("{} cleared", dimension.identifier());
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
            WeatherData wd = level.getWeatherData();
            if (!wd.isRaining()) {
                wd.setRaining(true);
                wd.setRainTime(6000);
                wd.setClearWeatherTime(0);
                wd.setThundering(weather.state == WeatherState.THUNDER);
                if (weather.state == WeatherState.THUNDER) wd.setThunderTime(6000);
                wd.setDirty();
            }
        } finally {
            applyingWeather = false;
        }

        if (weather.rainLevel < 1.0F) {
            float prev = weather.rainLevel;
            weather.rainLevel = Math.min(1.0F, weather.rainLevel + 0.02F);
            level.setRainLevel(weather.rainLevel);
            sendToAll(level, new ClientboundGameEventPacket(
                ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, weather.rainLevel));
            if (prev == 0.0F) {
                LOGGER.info("{} ramping rainLevel={}", dimension.identifier(), weather.rainLevel);
            } else if (weather.rainLevel >= 1.0F) {
                LOGGER.info("{} ramping done rainLevel={}", dimension.identifier(), weather.rainLevel);
            }
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
            player.getScoreboardName(), dimension.identifier(),
            isRaining(dimension), weather.rainLevel, weather.clearing);

        if (!isRaining(dimension)) {
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
            if (DEBUG) {
                LOGGER.info("[PACKET] dim={} player={} type={} value={}",
                    level.dimension().identifier(), player.getScoreboardName(),
                    packet.getEvent(), packet.getParam());
            }
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
