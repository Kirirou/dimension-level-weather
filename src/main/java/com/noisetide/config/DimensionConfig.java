package com.noisetide.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.noisetide.DimensionLevelWeather;
import com.noisetide.weather.WeatherManager;

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
        public boolean enabled = false;
        public boolean auto_cycle = false;
        public String default_state = "CLEAR";
        public long fixed_time = -1; // -1 means no fixed time
        public double thunder_chance = 0.01;
        public int min_clear_duration = 12000;
        public int max_clear_duration = 180000;
        public int min_rain_duration = 12000;
        public int max_rain_duration = 24000;
    }

    private final Map<String, DimensionEntry> dimensions = new HashMap<>();

    public Map<String, DimensionEntry> getDimensions() {
        return dimensions;
    }

    public DimensionEntry getEntry(String dimensionId) {
        return dimensions.get(dimensionId);
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
                        DimensionEntry de = GSON.fromJson(entry.getValue(), DimensionEntry.class);
                        config.dimensions.put(entry.getKey(), de);
                    });
                }
                DimensionLevelWeather.LOGGER.info(
                    "Loaded dimension-level-weather config with {} entries",
                    config.dimensions.size());
            } catch (IOException e) {
                DimensionLevelWeather.LOGGER.error("Failed to load config", e);
            }
        } else {
            config.createDefaults();
            config.save(configPath);
            DimensionLevelWeather.LOGGER.info("Created default dimension-level-weather config");
        }

        return config;
    }

    private void createDefaults() {
        DimensionEntry overworld = new DimensionEntry();
        overworld.enabled = true;
        overworld.auto_cycle = true;
        overworld.thunder_chance = 0.01;
        overworld.min_clear_duration = 12000;
        overworld.max_clear_duration = 180000;
        overworld.min_rain_duration = 12000;
        overworld.max_rain_duration = 24000;
        dimensions.put("minecraft:overworld", overworld);

        DimensionEntry end = new DimensionEntry();
        end.enabled = true;
        end.auto_cycle = false;
        end.default_state = "RAIN";
        end.fixed_time = 18000;
        dimensions.put("minecraft:the_end", end);

        DimensionEntry nether = new DimensionEntry();
        nether.enabled = true;
        nether.auto_cycle = false;
        nether.default_state = "RAIN";
        dimensions.put("minecraft:the_nether", nether);
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
