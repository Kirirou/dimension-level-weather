package com.kyryro.dimensionlevelweather.mixin;

import com.kyryro.dimensionlevelweather.DimensionLevelWeather;
import com.kyryro.dimensionlevelweather.weather.WeatherSavedData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.SpatialAttributeInterpolator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(EnvironmentAttributeSystem.class)
public abstract class EnvironmentAttributeSystemMixin {

    @Unique
    private static final ThreadLocal<ResourceKey<Level>> DLW_PENDING_DIMENSION = new ThreadLocal<>();

    @Unique
    private ResourceKey<Level> dlw$dimension;

    @Inject(
        method = "addDefaultLayers(Lnet/minecraft/world/attribute/EnvironmentAttributeSystem$Builder;Lnet/minecraft/world/level/Level;)V",
        at = @At("HEAD")
    )
    private static void dlw$captureLevel(EnvironmentAttributeSystem.Builder builder, Level level, CallbackInfo ci) {
        DLW_PENDING_DIMENSION.set(level.dimension());
    }

    @Inject(method = "<init>(Ljava/util/Map;)V", at = @At("TAIL"))
    private void dlw$storeDimension(Map<?, ?> map, CallbackInfo ci) {
        this.dlw$dimension = DLW_PENDING_DIMENSION.get();
        DLW_PENDING_DIMENSION.remove();
    }

    @Inject(method = "getDimensionValue", at = @At("RETURN"), cancellable = true)
    private void dlw$overrideDimensionValue(EnvironmentAttribute<?> attribute,
                                             CallbackInfoReturnable<Object> cir) {
        if (dlw$dimension == null) return;
        WeatherSavedData data = DimensionLevelWeather.WEATHER.getSavedData();
        if (data == null) return;
        if (attribute == EnvironmentAttributes.FAST_LAVA) {
            data.getFastLava(dlw$dimension).ifPresent(cir::setReturnValue);
        } else if (attribute == EnvironmentAttributes.WATER_EVAPORATES) {
            data.getWaterEvaporates(dlw$dimension).ifPresent(cir::setReturnValue);
        }
    }

    @Inject(
        method = "getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/attribute/SpatialAttributeInterpolator;)Ljava/lang/Object;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void dlw$overrideValue(EnvironmentAttribute<?> attribute, Vec3 pos,
                                    SpatialAttributeInterpolator interpolator,
                                    CallbackInfoReturnable<Object> cir) {
        if (dlw$dimension == null) return;
        WeatherSavedData data = DimensionLevelWeather.WEATHER.getSavedData();
        if (data == null) return;
        if (attribute == EnvironmentAttributes.WATER_EVAPORATES) {
            data.getWaterEvaporates(dlw$dimension).ifPresent(cir::setReturnValue);
        }
    }
}
