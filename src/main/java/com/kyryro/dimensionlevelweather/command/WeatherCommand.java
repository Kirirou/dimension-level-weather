package com.kyryro.dimensionlevelweather.command;

import com.mojang.brigadier.CommandDispatcher;
import com.kyryro.dimensionlevelweather.DimensionLevelWeather;
import com.kyryro.dimensionlevelweather.weather.WeatherManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.gamerules.GameRules;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;

public class WeatherCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Intercept vanilla /weather clear to also clear all custom dimensions
        dispatcher.register(
            Commands.literal("weather")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("clear")
                    .executes(ctx -> {
                        for (ServerLevel level : ctx.getSource().getServer().getAllLevels()) {
                            DimensionLevelWeather.WEATHER.setState(
                                level.dimension(), WeatherManager.WeatherState.CLEAR, level);
                        }
                        return 0;
                    }))
                .then(Commands.literal("rain")
                    .executes(ctx -> {
                        ServerLevel overworld = ctx.getSource().getServer().overworld();
                        DimensionLevelWeather.WEATHER.setState(
                            overworld.dimension(), WeatherManager.WeatherState.RAIN, overworld);
                        return 0;
                    }))
                .then(Commands.literal("thunder")
                    .executes(ctx -> {
                        ServerLevel overworld = ctx.getSource().getServer().overworld();
                        DimensionLevelWeather.WEATHER.setState(
                            overworld.dimension(), WeatherManager.WeatherState.THUNDER, overworld);
                        return 0;
                    }))
        );

        // Custom /dimweather commands
        dispatcher.register(
            Commands.literal("dimweather")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("set")
                    .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .then(Commands.literal("rain")
                            .executes(ctx -> setWeather(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"),
                                WeatherManager.WeatherState.RAIN)))
                        .then(Commands.literal("thunder")
                            .executes(ctx -> setWeather(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"),
                                WeatherManager.WeatherState.THUNDER)))
                        .then(Commands.literal("clear")
                            .executes(ctx -> setWeather(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"),
                                WeatherManager.WeatherState.CLEAR)))))
                .then(Commands.literal("rain")
                    .executes(ctx -> setAllWeather(ctx.getSource(),
                        WeatherManager.WeatherState.RAIN)))
                .then(Commands.literal("thunder")
                    .executes(ctx -> setAllWeather(ctx.getSource(),
                        WeatherManager.WeatherState.THUNDER)))
                .then(Commands.literal("clear")
                    .executes(ctx -> setAllWeather(ctx.getSource(),
                        WeatherManager.WeatherState.CLEAR)))
                .then(Commands.literal("query")
                    .executes(ctx -> queryAll(ctx.getSource()))
                    .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .executes(ctx -> queryWeather(ctx.getSource(),
                            DimensionArgument.getDimension(ctx, "dimension")))))
                .then(Commands.literal("advance")
                    .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .then(Commands.literal("true")
                            .executes(ctx -> setAdvanceWeather(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"), true)))
                        .then(Commands.literal("false")
                            .executes(ctx -> setAdvanceWeather(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"), false)))))
        );
    }

    private static int setAdvanceWeather(CommandSourceStack source,
                                          ServerLevel level, boolean value) {
        DimensionLevelWeather.WEATHER.getSavedData()
            .setAdvanceWeather(level.dimension(), value);

        boolean globalAdvance = level.getGameRules().get(GameRules.ADVANCE_WEATHER);
        String warning = globalAdvance ? "" :
            " (warning: advance_weather gamerule is false, this setting will take " +
            "effect when the gamerule is re-enabled)";

        source.sendSuccess(() -> Component.literal(
            "advance_weather for " + level.dimension().identifier()
            + " set to " + value + warning), true);
        return 1;
    }

    private static MutableComponent formatDimWeather(ServerLevel level,
                                                   WeatherManager.WeatherState state,
                                                   boolean advance) {
        return Component.empty()
            .append(Component.literal("[").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(level.dimension().identifier().toString())
                .withStyle(ChatFormatting.GOLD))
            .append(Component.literal("]\n").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("  weather: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(state.name().toLowerCase())
                .withStyle(ChatFormatting.WHITE))
            .append(Component.literal("  advance_weather: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.valueOf(advance))
                .withStyle(advance ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
    }

    private static int setWeather(CommandSourceStack source, ServerLevel level,
                                   WeatherManager.WeatherState state) {
        DimensionLevelWeather.WEATHER.setState(level.dimension(), state, level);
        source.sendSuccess(() -> Component.literal(
            "Weather in " + level.dimension().identifier() + " set to "
                + state.name().toLowerCase()), true);
        return 1;
    }

    private static int setAllWeather(CommandSourceStack source,
                                      WeatherManager.WeatherState state) {
        for (ServerLevel level : source.getServer().getAllLevels()) {
            DimensionLevelWeather.WEATHER.setState(level.dimension(), state, level);
        }
        source.sendSuccess(() -> Component.literal(
            "Weather in all dimensions set to " + state.name().toLowerCase()), true);
        return 1;
    }

    private static int queryAll(CommandSourceStack source) {
        MutableComponent output = Component.literal("Dimension weather:\n")
            .withStyle(ChatFormatting.AQUA);
        for (ServerLevel level : source.getServer().getAllLevels()) {
            WeatherManager.WeatherState state =
                DimensionLevelWeather.WEATHER.getState(level.dimension());
            boolean advance = DimensionLevelWeather.WEATHER.getSavedData() == null
                || DimensionLevelWeather.WEATHER.getSavedData()
                    .getAdvanceWeather(level.dimension());
            output = output.append(formatDimWeather(level, state, advance))
                           .append(Component.literal("\n"));
        }
        final MutableComponent finalOutput = output;
        source.sendSuccess(() -> finalOutput, false);
        return 1;
    }

    private static int queryWeather(CommandSourceStack source, ServerLevel level) {
        WeatherManager.WeatherState state =
            DimensionLevelWeather.WEATHER.getState(level.dimension());
        boolean advance = DimensionLevelWeather.WEATHER.getSavedData() == null
            || DimensionLevelWeather.WEATHER.getSavedData()
                .getAdvanceWeather(level.dimension());
        source.sendSuccess(() -> formatDimWeather(level, state, advance), false);
        return 1;
    }
}
