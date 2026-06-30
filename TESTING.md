# Testing Checklist

## `/dimweather query`
- [ ] `/dimweather query` — all three dimensions listed
- [ ] Overworld shows `advance_weather: true (default)`, `advance_time: true (default)`
- [ ] Nether/End show `advance_weather: false (default)`, `advance_time: false (default)`
- [ ] After `/dimweather advance minecraft:the_nether true` → nether shows `advance_weather: true` (no "(default)")
- [ ] `/dimweather query minecraft:the_nether` — single dimension output

## Weather — visual
- [ ] `/dimweather set minecraft:the_nether rain` → enter nether, rain particles fall
- [ ] `/dimweather set minecraft:the_end thunder` → enter end, rain + lightning
- [ ] `/dimweather set minecraft:the_nether clear` → rain stops in nether
- [ ] `/dimweather thunder` (no dimension) → sets thunder on overworld only
- [ ] `/weather clear` (vanilla command) → clears overworld weather

## Weather — fire extinguishing
- [ ] Nether with rain set: place fire on netherrack → extinguishes within a few ticks

## Weather — infiniburn
- [ ] `/dimweather infiniburn minecraft:the_nether false` → fire on netherrack does NOT persist
- [ ] `/dimweather infiniburn minecraft:the_nether true` → fire on netherrack persists again

## Weather — advance_weather
- [ ] `/dimweather advance minecraft:the_nether false` → set rain, wait several minutes → weather does not clear on its own
- [ ] `/dimweather advance minecraft:the_nether true` → weather cycles naturally again

## Fast lava
- [ ] `/dimweather fast_lava minecraft:overworld true` → lava in overworld flows fast
- [ ] `/dimweather fast_lava minecraft:overworld false` → lava flows slow
- [ ] `/dimweather query minecraft:overworld` → fast_lava reflects set value

## Water evaporates
- [ ] `/dimweather water_evaporates minecraft:the_nether false` → water bucket places water in nether
- [ ] `/dimweather water_evaporates minecraft:the_nether true` → water evaporates again
- [ ] Water evaporation particle visual works correctly on client side

## Time — set
- [ ] `/dimtime set minecraft:the_nether night` → `/dimtime query minecraft:the_nether` shows ~13000
- [ ] `/dimtime set minecraft:overworld 6000` → overworld shows noon
- [ ] `/dimtime set minecraft:the_end 0` → end time = 0

## Time — advance
- [ ] `/dimtime query` — overworld shows `advance_time: true (default)`, nether/end show `false (default)`
- [ ] `/dimtime advance minecraft:overworld false` → sun stops moving; query shows `advance_time: false` (no "(default)")
- [ ] `/dimtime set minecraft:overworld 1000` while frozen → time jumps to 1000, stays frozen
- [ ] `/dimtime advance minecraft:overworld true` → time resumes from 1000

## Persistence (server restart)
- [ ] Set nether to RAIN, freeze nether time, set `water_evaporates=false` in nether
- [ ] Restart server
- [ ] Nether still raining after restart
- [ ] Nether time still frozen at saved value after restart
- [ ] Water still placeable in nether after restart

## Player join sync
- [ ] Set `water_evaporates=false` in nether before player joins
- [ ] Fresh player joins → water bucket works in nether immediately (received `WaterEvaporatesPayload` on join)

## Dimension change sync
- [ ] Nether rain active → player enters nether from overworld → rain starts immediately

## Live update sync
- [ ] Player online in nether → run `/dimweather water_evaporates minecraft:the_nether false` → water bucket works immediately without reconnect

## Riptide trident
- [ ] Use riptide trident while in rain → launches without kick/velocity desync
