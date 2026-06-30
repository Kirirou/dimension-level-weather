package com.kyryro.dimensionlevelweather.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.kyryro.dimensionlevelweather.DimensionLevelWeather;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class TimeCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("dimtime")
                .requires(source -> source.permissions()
                    .hasPermission(Permissions.COMMANDS_GAMEMASTER))

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

                .then(Commands.literal("advance")
                    .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .then(Commands.literal("true")
                            .executes(ctx -> setAdvanceTime(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"), true)))
                        .then(Commands.literal("false")
                            .executes(ctx -> setAdvanceTime(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"), false)))))

                .then(Commands.literal("query")
                    .executes(ctx -> queryAll(ctx.getSource()))
                    .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .executes(ctx -> queryTime(ctx.getSource(),
                            DimensionArgument.getDimension(ctx, "dimension")))))
                .then(Commands.literal("reset")
                    .then(Commands.literal("all")
                        .executes(ctx -> resetAllAdvanceTime(ctx.getSource())))
                    .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .executes(ctx -> resetAllAdvanceTimeDim(ctx.getSource(),
                            DimensionArgument.getDimension(ctx, "dimension")))))
        );
    }

    private static long getLevelDayTime(ServerLevel level) {
        return level.dimensionType().defaultClock()
            .map(clock -> level.clockManager().getTotalTicks(clock) % 24000)
            .orElse(0L);
    }

    private static void setLevelDayTime(ServerLevel level, long time) {
        level.dimensionType().defaultClock().ifPresent(clock -> {
            ServerClockManager mgr = level.clockManager();
            long current = mgr.getTotalTicks(clock);
            long dayStart = current - (current % 24000);
            mgr.setTotalTicks(clock, dayStart + time);
        });
    }

    private static int setTime(CommandSourceStack source, ServerLevel level, long time) {
        setLevelDayTime(level, time);
        source.sendSuccess(() -> Component.empty()
            .append(Component.literal("Set time in ").withStyle(ChatFormatting.GRAY))
            .append(WeatherCommand.dimComponent(level))
            .append(Component.literal(" to ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.valueOf(time)).withStyle(ChatFormatting.WHITE)), true);
        return 1;
    }

    private static int setAdvanceTime(CommandSourceStack source,
                                       ServerLevel level, boolean value) {
        DimensionLevelWeather.WEATHER.getSavedData()
            .setAdvanceTime(level.dimension(), value);
        level.dimensionType().defaultClock().ifPresent(clock ->
            level.clockManager().setPaused(clock, !value));
        boolean defaultValue = level.dimension() == Level.OVERWORLD;
        boolean globalAdvance = level.getGameRules().get(GameRules.ADVANCE_TIME);
        source.sendSuccess(() -> Component.empty()
            .append(Component.literal("advance_time for ").withStyle(ChatFormatting.GRAY))
            .append(WeatherCommand.dimComponent(level))
            .append(Component.literal(" set to ").withStyle(ChatFormatting.GRAY))
            .append(WeatherCommand.optionalBoolDisplay(Optional.of(value), defaultValue))
            .append(globalAdvance ? Component.empty()
                : Component.literal(" (warning: advance_time gamerule is false, this setting will take effect when the gamerule is re-enabled)")
                    .withStyle(ChatFormatting.YELLOW)), true);
        return 1;
    }

    private static MutableComponent formatDimTime(ServerLevel level,
                                                   long time,
                                                   Optional<Boolean> advance) {
        boolean defaultAdvance = level.dimension() == Level.OVERWORLD;
        return Component.empty()
            .append(Component.literal("[").withStyle(ChatFormatting.GRAY))
            .append(WeatherCommand.dimComponent(level))
            .append(Component.literal("]\n").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("  time: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.valueOf(time))
                .withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\n  advance_time: ").withStyle(ChatFormatting.GRAY))
            .append(WeatherCommand.optionalBoolDisplay(advance, defaultAdvance));
    }

    private static int queryTime(CommandSourceStack source, ServerLevel level) {
        var data = DimensionLevelWeather.WEATHER.getSavedData();
        long time = getLevelDayTime(level);
        Optional<Boolean> advance = data == null ? Optional.empty() : data.getAdvanceTimeOptional(level.dimension());
        source.sendSuccess(() -> formatDimTime(level, time, advance), false);
        return 1;
    }

    private static int resetAllAdvanceTimeDim(CommandSourceStack source, ServerLevel level) {
        DimensionLevelWeather.WEATHER.getSavedData().removeAdvanceTime(level.dimension());
        boolean def = level.dimension() == Level.OVERWORLD;
        level.dimensionType().defaultClock().ifPresent(clock ->
            level.clockManager().setPaused(clock, !def));
        source.sendSuccess(() -> Component.empty()
            .append(Component.literal("All time rules for ").withStyle(ChatFormatting.GRAY))
            .append(WeatherCommand.dimComponent(level))
            .append(Component.literal(" reset to vanilla defaults").withStyle(ChatFormatting.GRAY)), true);
        return 1;
    }

    private static int resetAllAdvanceTime(CommandSourceStack source) {
        List<ServerLevel> levels = new ArrayList<>();
        source.getServer().getAllLevels().forEach(levels::add);
        var data = DimensionLevelWeather.WEATHER.getSavedData();
        for (ServerLevel level : levels) {
            data.removeAdvanceTime(level.dimension());
            boolean def = level.dimension() == Level.OVERWORLD;
            level.dimensionType().defaultClock().ifPresent(clock ->
                level.clockManager().setPaused(clock, !def));
        }
        source.sendSuccess(() -> Component.literal("All time rules reset to vanilla defaults")
            .withStyle(ChatFormatting.GRAY), true);
        return 1;
    }

    private static int queryAll(CommandSourceStack source) {
        MutableComponent output = Component.literal("Dimension time:\n")
            .withStyle(ChatFormatting.AQUA);
        var data = DimensionLevelWeather.WEATHER.getSavedData();
        List<ServerLevel> levels = new ArrayList<>();
        source.getServer().getAllLevels().forEach(levels::add);
        levels.sort(Comparator.comparingInt(WeatherCommand::dimOrder));
        for (ServerLevel level : levels) {
            long time = getLevelDayTime(level);
            Optional<Boolean> advance = data == null ? Optional.empty() : data.getAdvanceTimeOptional(level.dimension());
            output = output.append(formatDimTime(level, time, advance))
                           .append(Component.literal("\n"));
        }
        final MutableComponent finalOutput = output;
        source.sendSuccess(() -> finalOutput, false);
        return 1;
    }
}
