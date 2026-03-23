package com.noisetide.mixin;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TridentItem.class)
public abstract class TridentItemMixin {

    @Inject(
        method = "releaseUsing",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;startAutoSpinAttack(IFLnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.AFTER
        )
    )
    private void syncRiptideVelocity(ItemStack stack, Level level,
                                      LivingEntity entity, int remainingUseTicks,
                                      CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(
                new ClientboundSetEntityMotionPacket(serverPlayer));
        }
    }
}
