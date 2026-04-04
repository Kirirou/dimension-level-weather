package com.kyryro.dimensionlevelweather.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.kyryro.dimensionlevelweather.DimensionLevelWeather;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DimensionConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "config/dimension-level-weather.json";

    public static class DimensionEntry {
        public double thunder_chance = 0.01;
        public int min_clear_duration = 12000;
        public int max_clear_duration = 180000;
        public int min_rain_duration = 12000;
        public int max_rain_duration = 24000;
    }

    private static final DimensionEntry DEFAULTS = new DimensionEntry();

    private final Map<String, DimensionEntry> dimensions = new HashMap<>();

    public DimensionEntry getEntry(String dimensionId) {
        return dimensions.getOrDefault(dimensionId, DEFAULTS);
    }

    public static DimensionConfig loadOrCreate(Path gameDir) {
        Path configPath = gameDir.resolve(CONFIG_FILE);
        DimensionConfig config = new DimensionConfig();

        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root != null && root.has("dimensions")) {
                    JsonObject dims = root.getAsJsonObject("dimensions");
                    dims.entrySet().forEach(entry -> {
                        DimensionEntry de = GSON.fromJson(
                            entry.getValue(), DimensionEntry.class);
                        config.dimensions.put(entry.getKey(), de);
                    });
                }
                DimensionLevelWeather.LOGGER.info(
                    "Loaded config with {} dimension entries",
                    config.dimensions.size());
            } catch (IOException e) {
                DimensionLevelWeather.LOGGER.error("Failed to load config", e);
            }
        } else {
            config.createDefaults();
            config.save(configPath);
            DimensionLevelWeather.LOGGER.info(
                "Created default dimension-level-weather config");
        }

        return config;
    }

    private void createDefaults() {
        dimensions.put("minecraft:overworld", new DimensionEntry());
        dimensions.put("minecraft:the_nether", new DimensionEntry());
        dimensions.put("minecraft:the_end", new DimensionEntry());
    }

    public void save(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());
            JsonObject root = new JsonObject();
            JsonObject dims = new JsonObject();
            dimensions.forEach((id, entry) ->
                dims.add(id, GSON.toJsonTree(entry)));
            root.add("dimensions", dims);
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            DimensionLevelWeather.LOGGER.error("Failed to save config", e);
        }
    }
}
