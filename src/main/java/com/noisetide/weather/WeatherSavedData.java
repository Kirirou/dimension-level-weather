package com.noisetide.weather;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

public class WeatherSavedData extends SavedData {

    private final Map<ResourceKey<Level>, WeatherManager.WeatherState> states;
    private final Map<ResourceKey<Level>, Boolean> advanceWeather;
    private final Map<ResourceKey<Level>, Boolean> advanceTime;
    private final Map<ResourceKey<Level>, Long> fixedTime;

    public WeatherSavedData() {
        this.states = new HashMap<>();
        this.advanceWeather = new HashMap<>();
        this.advanceTime = new HashMap<>();
        this.fixedTime = new HashMap<>();
    }

    public WeatherSavedData(
            Map<ResourceKey<Level>, WeatherManager.WeatherState> states,
            Map<ResourceKey<Level>, Boolean> advanceWeather,
            Map<ResourceKey<Level>, Boolean> advanceTime,
            Map<ResourceKey<Level>, Long> fixedTime) {
        this.states = new HashMap<>(states);
        this.advanceWeather = new HashMap<>(advanceWeather);
        this.advanceTime = new HashMap<>(advanceTime);
        this.fixedTime = new HashMap<>(fixedTime);
    }

    private static final Codec<Map<ResourceKey<Level>, WeatherManager.WeatherState>> STATES_CODEC =
        Codec.unboundedMap(
            Identifier.CODEC.xmap(
                id -> ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id),
                ResourceKey::identifier),
            Codec.STRING.xmap(
                WeatherManager.WeatherState::valueOf,
                WeatherManager.WeatherState::name));

    private static final Codec<Map<ResourceKey<Level>, Boolean>> BOOL_MAP_CODEC =
        Codec.unboundedMap(
            Identifier.CODEC.xmap(
                id -> ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id),
                ResourceKey::identifier),
            Codec.BOOL);

    private static final Codec<Map<ResourceKey<Level>, Long>> LONG_MAP_CODEC =
        Codec.unboundedMap(
            Identifier.CODEC.xmap(
                id -> ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id),
                ResourceKey::identifier),
            Codec.LONG);

    public static final Codec<WeatherSavedData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            STATES_CODEC.optionalFieldOf("states", new HashMap<>())
                .forGetter(d -> d.states),
            BOOL_MAP_CODEC.optionalFieldOf("advance_weather", new HashMap<>())
                .forGetter(d -> d.advanceWeather),
            BOOL_MAP_CODEC.optionalFieldOf("advance_time", new HashMap<>())
                .forGetter(d -> d.advanceTime),
            LONG_MAP_CODEC.optionalFieldOf("fixed_time", new HashMap<>())
                .forGetter(d -> d.fixedTime)
        ).apply(instance, WeatherSavedData::new)
    );

    public static final SavedDataType<WeatherSavedData> TYPE = new SavedDataType<>(
        "dimension_weather",
        WeatherSavedData::new,
        CODEC,
        DataFixTypes.LEVEL
    );

    public static WeatherSavedData getOrCreate(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    // Weather state
    public WeatherManager.WeatherState getState(ResourceKey<Level> dimension) {
        return states.getOrDefault(dimension, WeatherManager.WeatherState.CLEAR);
    }

    public void setState(ResourceKey<Level> dimension, WeatherManager.WeatherState state) {
        if (state == WeatherManager.WeatherState.CLEAR) {
            states.remove(dimension);
        } else {
            states.put(dimension, state);
        }
        setDirty();
    }

    // Advance weather
    public boolean getAdvanceWeather(ResourceKey<Level> dimension) {
        return advanceWeather.getOrDefault(dimension, true);
    }

    public void setAdvanceWeather(ResourceKey<Level> dimension, boolean value) {
        advanceWeather.put(dimension, value);
        setDirty();
    }

    // Advance time
    public boolean getAdvanceTime(ResourceKey<Level> dimension) {
        return advanceTime.getOrDefault(dimension, true);
    }

    public void setAdvanceTime(ResourceKey<Level> dimension, boolean value) {
        advanceTime.put(dimension, value);
        setDirty();
    }

    // Fixed time
    public long getFixedTime(ResourceKey<Level> dimension) {
        return fixedTime.getOrDefault(dimension, -1L);
    }

    public void setFixedTime(ResourceKey<Level> dimension, long time) {
        if (time < 0) {
            fixedTime.remove(dimension);
        } else {
            fixedTime.put(dimension, time);
        }
        setDirty();
    }

    public boolean hasFixedTime(ResourceKey<Level> dimension) {
        return fixedTime.containsKey(dimension);
    }
}
