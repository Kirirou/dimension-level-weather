package com.kyryro.dimensionlevelweather.config;

import com.kyryro.dimensionlevelweather.DimensionLevelWeather;
import com.kyryro.dimensionlevelweather.weather.WeatherManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AdvanceWeatherManager {

    private static final Logger LOGGER = LogManager.getLogger("dimension-level-weather");
    private static final Random RANDOM = new Random();

    private static final class CycleState {
        int ticksUntilChange = 0;
    }

    private final Map<ResourceKey<Level>, CycleState> states = new HashMap<>();

    private CycleState getOrCreate(ResourceKey<Level> dimension) {
        return states.computeIfAbsent(dimension, k -> new CycleState());
    }

    public void tick(ServerLevel level) {
        if (!level.getGameRules().get(GameRules.ADVANCE_WEATHER)) return;
        if (DimensionLevelWeather.WEATHER.getSavedData() == null) return;
        if (!DimensionLevelWeather.WEATHER.getSavedData()
                .getAdvanceWeather(level.dimension())) return;

        if (DimensionLevelWeather.WEATHER.isClearing(level.dimension())) return;

        ResourceKey<Level> dimension = level.dimension();
        DimensionConfig.DimensionEntry entry =
            DimensionLevelWeather.getConfig().getEntry(
                dimension.identifier().toString());

        CycleState state = getOrCreate(dimension);
        state.ticksUntilChange--;
        if (state.ticksUntilChange > 0) return;

        WeatherManager.WeatherState current =
            DimensionLevelWeather.WEATHER.getState(dimension);

        if (current == WeatherManager.WeatherState.CLEAR) {
            int duration = entry.min_rain_duration + RANDOM.nextInt(
                Math.max(1, entry.max_rain_duration - entry.min_rain_duration));
            state.ticksUntilChange = duration;

            boolean thunder = RANDOM.nextDouble() < entry.thunder_chance;
            WeatherManager.WeatherState next = thunder
                ? WeatherManager.WeatherState.THUNDER
                : WeatherManager.WeatherState.RAIN;

            LOGGER.info("[WEATHER CHANGE] {} CLEAR -> {} (auto cycle, duration={}t)",
                dimension.identifier(), next, duration);
            DimensionLevelWeather.WEATHER.setState(dimension, next, level);

        } else {
            int duration = entry.min_clear_duration + RANDOM.nextInt(
                Math.max(1, entry.max_clear_duration - entry.min_clear_duration));
            state.ticksUntilChange = duration;

            LOGGER.info("[WEATHER CHANGE] {} {} -> CLEAR (auto cycle, duration={}t)",
                dimension.identifier(), current, duration);
            DimensionLevelWeather.WEATHER.setState(
                dimension, WeatherManager.WeatherState.CLEAR, level);
        }
    }

    public void initCycle(ResourceKey<Level> dimension,
                           DimensionConfig.DimensionEntry entry) {
        CycleState state = getOrCreate(dimension);
        state.ticksUntilChange = RANDOM.nextInt(
            Math.max(1, entry.max_clear_duration));
        LOGGER.info("[WEATHER CYCLE INIT] {} ticksUntilChange={}",
            dimension.identifier(), state.ticksUntilChange);
    }
}
