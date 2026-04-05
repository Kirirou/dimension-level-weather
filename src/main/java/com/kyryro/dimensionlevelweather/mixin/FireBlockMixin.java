package com.kyryro.dimensionlevelweather.mixin;

import com.kyryro.dimensionlevelweather.DimensionLevelWeather;
import com.kyryro.dimensionlevelweather.weather.WeatherSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin {

    @Unique
    private static final TagKey<Block> DLW_EMPTY_TAG =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("dimension_level_weather", "empty"));

    @Unique
    private ServerLevel dlw$level;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void extinguishInRain(BlockState state, ServerLevel level,
                                   BlockPos pos, RandomSource random, CallbackInfo ci) {
        this.dlw$level = level;

        if (!DimensionLevelWeather.WEATHER.isRaining(level.dimension())) return;
        if (!level.isRainingAt(pos)) return;

        int age = state.getValue(FireBlock.AGE);
        float chance = 0.2F + age * 0.03F;
        if (random.nextFloat() < chance) {
            level.removeBlock(pos, false);
            ci.cancel();
        }
    }

    @Redirect(
        method = "tick",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/dimension/DimensionType;infiniburn()Lnet/minecraft/tags/TagKey;")
    )
    private TagKey<Block> redirectInfiniburn(DimensionType instance) {
        if (dlw$level == null) return instance.infiniburn();
        WeatherSavedData data = DimensionLevelWeather.WEATHER.getSavedData();
        if (data == null) return instance.infiniburn();
        return data.getInfiniburn(dlw$level.dimension())
            .map(enabled -> enabled ? instance.infiniburn() : DLW_EMPTY_TAG)
            .orElse(instance.infiniburn());
    }
}
