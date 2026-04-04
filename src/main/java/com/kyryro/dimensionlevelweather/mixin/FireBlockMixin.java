package com.kyryro.dimensionlevelweather.mixin;

import com.kyryro.dimensionlevelweather.DimensionLevelWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
  @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
  private void extinguishInRain(BlockState state, ServerLevel level,
                                 BlockPos pos, RandomSource random,
                                 CallbackInfo ci) {
      if (!DimensionLevelWeather.WEATHER.isRaining(level.dimension())) return;
      if (!level.isRainingAt(pos)) return;

      int age = state.getValue(FireBlock.AGE);
      float chance = 0.2F + age * 0.03F;
      if (random.nextFloat() < chance) {
          level.removeBlock(pos, false);
          ci.cancel();
      }
  }
}
