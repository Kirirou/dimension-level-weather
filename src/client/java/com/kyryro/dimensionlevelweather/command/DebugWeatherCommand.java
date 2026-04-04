package com.kyryro.dimensionlevelweather.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.apache.logging.log4j.LogManager;

public class DebugWeatherCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            ClientCommandManager.literal("dimclientrainlevel")
                .executes(ctx -> {
                    ClientLevel level = Minecraft.getInstance().level;
                    if (level == null) return 0;

                    float rain = level.getRainLevel(1.0F);
                    float thunder = level.getThunderLevel(1.0F);
                    String dim = level.dimension().identifier().toString();

                    LogManager.getLogger("dimension-level-weather")
                        .info("[CLIENT DEBUG] dim={} rainLevel={} thunderLevel={}", dim, rain, thunder);

                    ctx.getSource().sendFeedback(
                        Component.empty()
                            .append(Component.literal("[").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(dim).withStyle(ChatFormatting.GOLD))
                            .append(Component.literal("] ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal("rainLevel: ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(String.format("%.4f", rain))
                                .withStyle(rain > 0.0F ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY))
                            .append(Component.literal("  thunderLevel: ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(String.format("%.4f", thunder))
                                .withStyle(thunder > 0.0F ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY))
                    );
                    
                    return 1;
                })
        );
    }
}
