package com.noisetide.mixin.client;

import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin {

    @Inject(
        method = "getPrecipitationAt",
        at = @At("RETURN"),
        cancellable = true
    )
    private void overrideEndPrecipitation(
        Level level,
        BlockPos pos,
        CallbackInfoReturnable<Biome.Precipitation> cir
    ) {
        if (level.dimension() == Level.END
            && level.getLevelData().isRaining()) {

            cir.setReturnValue(Biome.Precipitation.RAIN);
        }
    }
}
