package com.kyryro.dimensionlevelweather.config;

import com.kyryro.dimensionlevelweather.DimensionLevelWeather;
import com.kyryro.dimensionlevelweather.weather.WeatherSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Advances time for dimensions that have no WorldClock (e.g. the vanilla nether).
 * Dimensions with a real defaultClock() are handled by ServerClockManager; this
 * class only steps in for clockless dimensions where advance_time is true.
 */
public class AdvanceTimeManager {

    public void tick(ServerLevel level) {
        if (level.dimensionType().defaultClock().isPresent()) return;

        WeatherSavedData data = DimensionLevelWeather.WEATHER.getSavedData();
        if (data == null) return;
        if (!data.getAdvanceTime(level.dimension())) return;
        if (!level.getGameRules().get(GameRules.ADVANCE_TIME)) return;

        data.addCustomTime(level.dimension(), 1L);
    }
}
