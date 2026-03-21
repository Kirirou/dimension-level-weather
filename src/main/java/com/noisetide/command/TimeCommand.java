package com.noisetide.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.noisetide.DimensionLevelWeather;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.gamerules.GameRules;

public class TimeCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("dimtime")
                .requires(source -> source.permissions()
                    .hasPermission(Permissions.COMMANDS_GAMEMASTER))

                // /dimtime set <dim> <time|day|noon|night|midnight>
                .then(Commands.literal("set")
                    .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .then(Commands.literal("day")
                            .executes(ctx -> setTime(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"), 1000)))
                        .then(Commands.literal("noon")
                            .executes(ctx -> setTime(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"), 6000)))
                        .then(Commands.literal("night")
                            .executes(ctx -> setTime(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"), 13000)))
                        .then(Commands.literal("midnight")
                            .executes(ctx -> setTime(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"), 18000)))
                        .then(Commands.argument("time", LongArgumentType.longArg(0))
                            .executes(ctx -> setTime(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"),
                                LongArgumentType.getLong(ctx, "time"))))))

                // /dimtime advance <dim> <true|false>
                .then(Commands.literal("advance")
                    .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .then(Commands.literal("true")
                            .executes(ctx -> setAdvanceTime(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"), true)))
                        .then(Commands.literal("false")
                            .executes(ctx -> setAdvanceTime(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"), false)))))

                // /dimtime query [dim]
                .then(Commands.literal("query")
                    .executes(ctx -> queryAll(ctx.getSource()))
                    .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .executes(ctx -> queryTime(ctx.getSource(),
                            DimensionArgument.getDimension(ctx, "dimension")))))
        );
    }

    private static int setTime(CommandSourceStack source, ServerLevel level, long time) {
        if (DimensionLevelWeather.WEATHER.getSavedData() != null
                && DimensionLevelWeather.WEATHER.getSavedData()
                    .hasFixedTime(level.dimension())) {
            source.sendFailure(Component.literal(
                level.dimension().identifier()
                + " has a fixed time set in config. Remove fixed_time from config first."));
            return 0;
        }
        level.setDayTime(time);
        source.sendSuccess(() -> Component.literal(
            "Set time in " + level.dimension().identifier() + " to " + time), true);
        return 1;
    }

    private static int setAdvanceTime(CommandSourceStack source,
                                       ServerLevel level, boolean value) {
        DimensionLevelWeather.WEATHER.getSavedData()
            .setAdvanceTime(level.dimension(), value);

        boolean globalAdvance = level.getGameRules().get(GameRules.ADVANCE_TIME);
        String warning = globalAdvance ? "" :
            " (warning: advance_time gamerule is false, this setting will take " +
            "effect when the gamerule is re-enabled)";

        source.sendSuccess(() -> Component.literal(
            "advance_time for " + level.dimension().identifier()
            + " set to " + value + warning), true);
        return 1;
    }

    private static int queryTime(CommandSourceStack source, ServerLevel level) {
        long time = level.getDayTime() % 24000;
        boolean advance = DimensionLevelWeather.WEATHER.getSavedData() == null
            || DimensionLevelWeather.WEATHER.getSavedData()
                .getAdvanceTime(level.dimension());
        boolean fixed = DimensionLevelWeather.WEATHER.getSavedData() != null
            && DimensionLevelWeather.WEATHER.getSavedData()
                .hasFixedTime(level.dimension());

        source.sendSuccess(() -> Component.literal(
            level.dimension().identifier()
            + " | time=" + time
            + " advance_time=" + advance
            + (fixed ? " [fixed by config]" : "")), false);
        return 1;
    }

    private static int queryAll(CommandSourceStack source) {
        StringBuilder sb = new StringBuilder("Dimension time:\n");
        for (ServerLevel level : source.getServer().getAllLevels()) {
            long time = level.getDayTime() % 24000;
            boolean advance = DimensionLevelWeather.WEATHER.getSavedData() == null
                || DimensionLevelWeather.WEATHER.getSavedData()
                    .getAdvanceTime(level.dimension());
            boolean fixed = DimensionLevelWeather.WEATHER.getSavedData() != null
                && DimensionLevelWeather.WEATHER.getSavedData()
                    .hasFixedTime(level.dimension());

            sb.append("  ")
              .append(level.dimension().identifier())
              .append(" | time=").append(time)
              .append(" advance_time=").append(advance)
              .append(fixed ? " [fixed]" : "")
              .append("\n");
        }
        source.sendSuccess(() -> Component.literal(sb.toString().trim()), false);
        return 1;
    }
}
