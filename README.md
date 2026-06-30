# Dimension Level Weather

A Fabric mod that adds per-dimension weather and time control. Set rain, thunder, or clear skies independently in the Nether, End, and any custom dimension — without touching other dimensions.


## Requirements

- Minecraft 26.2
- Fabric Loader ≥ 0.19.3
- Fabric API
- Java 25

## Features

- Weather state (clear/rain/thunder) per dimension
- Rain particles and fire extinguishing work correctly in Nether and End
- Per-dimension control over whether weather and time advance naturally
- Per-dimension infiniburn, fast lava, and water evaporation rules
- All settings persist across server restarts
- Works on dedicated servers — no client mod required

## Commands

All commands require gamemaster permission level.

### dimweather

```
dimweather set <dimension> clear|rain|thunder
dimweather clear|rain|thunder
dimweather query [dimension]
dimweather advance <dimension> true|false
dimweather infiniburn <dimension> true|false
dimweather fast_lava <dimension> true|false
dimweather water_evaporates <dimension> true|false
dimweather reset <dimension> [advance_weather|advance_time|infiniburn|fast_lava|water_evaporates]
dimweather reset all
```

### dimtime

```
dimtime set <dimension> day|noon|night|midnight|<ticks>
dimtime advance <dimension> true|false
dimtime query [dimension]
dimtime reset <dimension>
dimtime reset all
```

## Configuration

A config file is created at `config/dimension-level-weather.json` on first launch. It controls weather cycle behaviour per dimension:

```json
{
  "dimensions": {
    "minecraft:overworld": {
      "thunder_chance": 0.01,
      "min_clear_duration": 12000,
      "max_clear_duration": 180000,
      "min_rain_duration": 12000,
      "max_rain_duration": 24000
    }
  }
}
```

Add entries for any dimension using its full ID (e.g. `minecraft:the_nether`).

## Vanilla defaults

| Rule             | Overworld | Nether | End   |
|------------------|-----------|--------|-------|
| advance_weather  | true      | false  | false |
| advance_time     | true      | false  | false |
| infiniburn       | true      | true   | true  |
| fast_lava        | false     | true   | false |
| water_evaporates | false     | true   | false |

## Screenshots

![Thunder in the Nether](screenshots/preview.png)
*Thunder in the Nether*

![Rain in the End](screenshots/preview2.png)
*Rain in the End*

## License

CC0-1.0 — public domain.
