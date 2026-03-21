package com.noisetide.mixin;

import com.noisetide.DimensionLevelWeather;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "advanceWeatherCycle", at = @At("HEAD"), cancellable = true)
    private void blockWeatherCycle(CallbackInfo ci) {
        ServerLevel level = (ServerLevel)(Object)this;
        if (!DimensionLevelWeather.WEATHER.isAdvanceWeatherEnabled(level.dimension())) {
            ci.cancel();
        }
    }

    @Inject(method = "tickTime", at = @At("HEAD"), cancellable = true)
    private void blockTimeAdvance(CallbackInfo ci) {
        ServerLevel level = (ServerLevel)(Object)this;
        if (!level.getGameRules().get(GameRules.ADVANCE_TIME)) return;
        if (DimensionLevelWeather.WEATHER.getSavedData() == null) return;
        if (!DimensionLevelWeather.WEATHER.getSavedData()
                .getAdvanceTime(level.dimension())) {
            ci.cancel();
        }
    }
}
