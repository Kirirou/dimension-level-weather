package com.noisetide.mixin;

import com.noisetide.DimensionLevelWeather;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "isRaining", at = @At("RETURN"), cancellable = true)
    private void overrideWeather(CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level)(Object)this;
        if (DimensionLevelWeather.WEATHER.isRaining(level.dimension())) {
            cir.setReturnValue(true);
        }
    }
}
