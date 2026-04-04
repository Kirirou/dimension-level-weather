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
        // Always cancel vanilla weather cycling for all dimensions.
        // AdvanceWeatherManager handles cycling based on the advance_weather flag.
        ci.cancel();
    }

    @Inject(
        method = "tickTime",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V"
        ),
        cancellable = true
    )
    private void blockDayTimeAdvance(CallbackInfo ci) {
        ServerLevel level = (ServerLevel)(Object)this;
        if (DimensionLevelWeather.WEATHER.getSavedData() == null) return;
        if (!DimensionLevelWeather.WEATHER.getSavedData()
                .getAdvanceTime(level.dimension())) {
            ci.cancel();
        }
    }
}
