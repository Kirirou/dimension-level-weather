package com.noisetide.weather;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;

public class WeatherSavedData extends SavedData {

    private final Map<ResourceKey<Level>, WeatherManager.WeatherState> states;

    public WeatherSavedData() {
        this.states = new HashMap<>();
    }

    public WeatherSavedData(Map<ResourceKey<Level>, WeatherManager.WeatherState> states) {
        this.states = new HashMap<>(states);
    }

    // Codec encodes dimension key as string, state as string
    private static final Codec<Map<ResourceKey<Level>, WeatherManager.WeatherState>> STATES_CODEC =
        Codec.unboundedMap(
            Identifier.CODEC.xmap(
                id -> ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id),
                ResourceKey::identifier
            ),
            Codec.STRING.xmap(
                WeatherManager.WeatherState::valueOf,
                WeatherManager.WeatherState::name
            )
        );

    public static final Codec<WeatherSavedData> CODEC = STATES_CODEC.xmap(
        WeatherSavedData::new,
        data -> data.states
    );

    public static final SavedDataType<WeatherSavedData> TYPE = new SavedDataType<>(
        "dimension_weather",
        WeatherSavedData::new,
        CODEC,
        DataFixTypes.LEVEL
    );

    public static WeatherSavedData getOrCreate(MinecraftServer server) {
        WeatherSavedData data = server.overworld().getDataStorage().computeIfAbsent(TYPE);
        System.out.println("WeatherSavedData.getOrCreate returned: " + data + " states=" + data.states);
        return data;
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
        // Temporary debug
        System.out.println("WeatherSavedData.setState called: " + dimension.identifier() + " -> " + state);
    }
}
