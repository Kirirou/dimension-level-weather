package com.kyryro.dimensionlevelweather.mixin;

import com.kyryro.dimensionlevelweather.DimensionLevelWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockAndTintGetter.class)
public interface BlockAndTintGetterMixin {

    @Inject(method = "canSeeSky", at = @At("RETURN"), cancellable = true)
    private void overrideCanSeeSky(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (!(((Object)this) instanceof Level level)) return;
        if (!DimensionLevelWeather.WEATHER.isRaining(level.dimension())) return;
        if (level.dimensionType().hasSkyLight()) return;

        if (level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY()
                <= pos.getY()) {
            cir.setReturnValue(true);
        }
    }
}
