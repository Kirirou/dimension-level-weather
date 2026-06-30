package com.kyryro.dimensionlevelweather.mixin;

import com.kyryro.dimensionlevelweather.DimensionLevelWeather;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin {

    /**
     * Overrides the WATER_EVAPORATES attribute value used in {@code emptyContents} so that
     * the dimension-level override is respected on both the server and the client.
     */
    @ModifyExpressionValue(
        method = "emptyContents",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/core/BlockPos;)Ljava/lang/Object;")
    )
    private Object dlw$overrideWaterEvaporates(Object original,
            @Local(argsOnly = true) Level level) {
        Optional<Boolean> override = DimensionLevelWeather.WEATHER.getWaterEvaporates(level.dimension());
        return override.isPresent() ? override.get() : original;
    }
}
