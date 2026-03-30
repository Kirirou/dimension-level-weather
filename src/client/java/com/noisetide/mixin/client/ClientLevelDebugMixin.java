package com.noisetide.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mixin(ClientLevel.class)
public abstract class ClientLevelDebugMixin {

    private static final Logger LOGGER = LogManager.getLogger("dimension-level-weather");

    @Inject(method = "tickTime", at = @At("HEAD"))
    private void logRainLevel(CallbackInfo ci) {
        ClientLevel level = (ClientLevel)(Object)this;
        float rain = level.getRainLevel(1.0F);
        float thunder = level.getThunderLevel(1.0F);
        if (rain > 0.0F || thunder > 0.0F) {
            LOGGER.info("[CLIENT] dim={} rainLevel={} thunderLevel={}",
                level.dimension().identifier(), rain, thunder);
        }
    }
}
