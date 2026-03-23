package com.noisetide.mixin;

import com.noisetide.DimensionLevelWeather;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow
    protected net.minecraft.server.players.PlayerList playerList;

    @Inject(method = "synchronizeTime", at = @At("HEAD"), cancellable = true)
    private void overrideSynchronizeTime(ServerLevel serverLevel, CallbackInfo ci) {
        if (DimensionLevelWeather.WEATHER.getSavedData() == null) return;

        boolean advanceTime = DimensionLevelWeather.WEATHER.getSavedData()
            .getAdvanceTime(serverLevel.dimension());

        if (!advanceTime) {
            this.playerList.broadcastAll(
                new ClientboundSetTimePacket(
                    serverLevel.getGameTime(),
                    serverLevel.getDayTime(),
                    false
                ),
                serverLevel.dimension()
            );
            ci.cancel();
        }
    }
}
