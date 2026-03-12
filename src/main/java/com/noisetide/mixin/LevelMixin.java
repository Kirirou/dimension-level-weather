package com.noisetide.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "precipitationAt", at = @At("RETURN"), cancellable = true)
    private void overrideEndPrecipitation(BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {

        Level level = (Level)(Object)this;

        if (!level.isClientSide()
            && level.dimension() == Level.END
            && level.getLevelData().isRaining()) {

            cir.setReturnValue(Biome.Precipitation.RAIN);
        }
    }
}
