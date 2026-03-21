package com.noisetide.mixin;

import com.noisetide.DimensionLevelWeather;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "isRaining", at = @At("RETURN"), cancellable = true)
    private void overrideRaining(CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level)(Object)this;
        if (DimensionLevelWeather.WEATHER.isRaining(level.dimension())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isThundering", at = @At("RETURN"), cancellable = true)
    private void overrideThundering(CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level)(Object)this;
        if (DimensionLevelWeather.WEATHER.isThundering(level.dimension())) {
            cir.setReturnValue(true);
        }
    }    

    @Inject(method = "precipitationAt", at = @At("RETURN"), cancellable = true)
    private void overridePrecipitationAt(BlockPos pos,
            CallbackInfoReturnable<Biome.Precipitation> cir) {
        Level level = (Level)(Object)this;

        // If vanilla already returned RAIN or SNOW, nothing to do
        if (cir.getReturnValue() != Biome.Precipitation.NONE) return;
        // Only act on dimensions we are managing
        if (!DimensionLevelWeather.WEATHER.isRaining(level.dimension())) return;

        // For dimensions with skylight, canSeeSky works correctly already.
        // For dimensions without skylight (Nether), use heightmap as proxy.
        if (level.dimensionType().hasSkyLight()) return;

        // Heightmap check: position must be at or above the surface
        if (level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY()
                > pos.getY()) return;

        Biome biome = level.getBiome(pos).value();
        cir.setReturnValue(biome.getPrecipitationAt(pos, level.getSeaLevel()));
    }
}
