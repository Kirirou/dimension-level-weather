package com.kyryro.dimensionlevelweather.weather;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class WeatherSavedData extends SavedData {

    private final Map<ResourceKey<Level>, WeatherManager.WeatherState> states;
    private final Map<ResourceKey<Level>, Boolean> advanceWeather;
    private final Map<ResourceKey<Level>, Boolean> advanceTime;
    private final Map<ResourceKey<Level>, Boolean> infiniburn;
    private final Map<ResourceKey<Level>, Boolean> fastLava;
    private final Map<ResourceKey<Level>, Boolean> waterEvaporates;

    public WeatherSavedData() {
        this.states = new HashMap<>();
        this.advanceWeather = new HashMap<>();
        this.advanceTime = new HashMap<>();
        this.infiniburn = new HashMap<>();
        this.fastLava = new HashMap<>();
        this.waterEvaporates = new HashMap<>();
    }

    public WeatherSavedData(
            Map<ResourceKey<Level>, WeatherManager.WeatherState> states,
            Map<ResourceKey<Level>, Boolean> advanceWeather,
            Map<ResourceKey<Level>, Boolean> advanceTime,
            Map<ResourceKey<Level>, Boolean> infiniburn,
            Map<ResourceKey<Level>, Boolean> fastLava,
            Map<ResourceKey<Level>, Boolean> waterEvaporates) {
        this.states = new HashMap<>(states);
        this.advanceWeather = new HashMap<>(advanceWeather);
        this.advanceTime = new HashMap<>(advanceTime);
        this.infiniburn = new HashMap<>(infiniburn);
        this.fastLava = new HashMap<>(fastLava);
        this.waterEvaporates = new HashMap<>(waterEvaporates);
    }

    private static final Codec<Map<ResourceKey<Level>, WeatherManager.WeatherState>> STATES_CODEC =
        Codec.unboundedMap(
            Identifier.CODEC.xmap(
                id -> ResourceKey.create(Registries.DIMENSION, id),
                ResourceKey::identifier),
            Codec.STRING.xmap(
                WeatherManager.WeatherState::valueOf,
                WeatherManager.WeatherState::name));

    private static final Codec<Map<ResourceKey<Level>, Boolean>> BOOL_MAP_CODEC =
        Codec.unboundedMap(
            Identifier.CODEC.xmap(
                id -> ResourceKey.create(Registries.DIMENSION, id),
                ResourceKey::identifier),
            Codec.BOOL);

    public static final Codec<WeatherSavedData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            STATES_CODEC.optionalFieldOf("states", new HashMap<>()).forGetter(d -> d.states),
            BOOL_MAP_CODEC.optionalFieldOf("advance_weather", new HashMap<>()).forGetter(d -> d.advanceWeather),
            BOOL_MAP_CODEC.optionalFieldOf("advance_time", new HashMap<>()).forGetter(d -> d.advanceTime),
            BOOL_MAP_CODEC.optionalFieldOf("infiniburn", new HashMap<>()).forGetter(d -> d.infiniburn),
            BOOL_MAP_CODEC.optionalFieldOf("fast_lava", new HashMap<>()).forGetter(d -> d.fastLava),
            BOOL_MAP_CODEC.optionalFieldOf("water_evaporates", new HashMap<>()).forGetter(d -> d.waterEvaporates)
        ).apply(instance, WeatherSavedData::new)
    );

    public static final SavedDataType<WeatherSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("dimension-level-weather", "dimension_weather"),
        WeatherSavedData::new, CODEC, DataFixTypes.LEVEL);

    public static WeatherSavedData getOrCreate(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

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

    public boolean getAdvanceWeather(ResourceKey<Level> dimension) {
        return advanceWeather.containsKey(dimension)
            ? advanceWeather.get(dimension)
            : dimension == Level.OVERWORLD;
    }

    public Optional<Boolean> getAdvanceWeatherOptional(ResourceKey<Level> dimension) {
        return Optional.ofNullable(advanceWeather.get(dimension));
    }

    public void setAdvanceWeather(ResourceKey<Level> dimension, boolean value) {
        advanceWeather.put(dimension, value);
        setDirty();
    }

    public boolean getAdvanceTime(ResourceKey<Level> dimension) {
        return advanceTime.containsKey(dimension)
            ? advanceTime.get(dimension)
            : dimension == Level.OVERWORLD;
    }

    public Optional<Boolean> getAdvanceTimeOptional(ResourceKey<Level> dimension) {
        return Optional.ofNullable(advanceTime.get(dimension));
    }

    public void setAdvanceTime(ResourceKey<Level> dimension, boolean value) {
        advanceTime.put(dimension, value);
        setDirty();
    }

    public Optional<Boolean> getInfiniburn(ResourceKey<Level> dimension) {
        return Optional.ofNullable(infiniburn.get(dimension));
    }

    public void setInfiniburn(ResourceKey<Level> dimension, boolean value) {
        infiniburn.put(dimension, value);
        setDirty();
    }

    public void removeInfiniburn(ResourceKey<Level> dimension) {
        infiniburn.remove(dimension);
        setDirty();
    }

    public Optional<Boolean> getFastLava(ResourceKey<Level> dimension) {
        return Optional.ofNullable(fastLava.get(dimension));
    }

    public void setFastLava(ResourceKey<Level> dimension, boolean value) {
        fastLava.put(dimension, value);
        setDirty();
    }

    public void removeFastLava(ResourceKey<Level> dimension) {
        fastLava.remove(dimension);
        setDirty();
    }

    public Optional<Boolean> getWaterEvaporates(ResourceKey<Level> dimension) {
        return Optional.ofNullable(waterEvaporates.get(dimension));
    }

    public Map<ResourceKey<Level>, Boolean> getWaterEvaporatesSnapshot() {
        return Collections.unmodifiableMap(waterEvaporates);
    }

    public void setWaterEvaporates(ResourceKey<Level> dimension, boolean value) {
        waterEvaporates.put(dimension, value);
        setDirty();
    }

    public void removeWaterEvaporates(ResourceKey<Level> dimension) {
        waterEvaporates.remove(dimension);
        setDirty();
    }

    public void removeAdvanceWeather(ResourceKey<Level> dimension) {
        advanceWeather.remove(dimension);
        setDirty();
    }

    public void removeAdvanceTime(ResourceKey<Level> dimension) {
        advanceTime.remove(dimension);
        setDirty();
    }
}
