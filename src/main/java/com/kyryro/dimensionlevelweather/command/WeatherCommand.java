package com.kyryro.dimensionlevelweather.command;

import com.mojang.brigadier.CommandDispatcher;
import com.kyryro.dimensionlevelweather.DimensionLevelWeather;
import com.kyryro.dimensionlevelweather.weather.WeatherManager;
import com.kyryro.dimensionlevelweather.weather.WeatherSavedData;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class WeatherCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
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
                    .executes(ctx -> setAllWeather(ctx.getSource(), WeatherManager.WeatherState.RAIN)))
                .then(Commands.literal("thunder")
                    .executes(ctx -> setAllWeather(ctx.getSource(), WeatherManager.WeatherState.THUNDER)))
                .then(Commands.literal("clear")
                    .executes(ctx -> setAllWeather(ctx.getSource(), WeatherManager.WeatherState.CLEAR)))
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
                .then(Commands.literal("infiniburn")
                    .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .then(Commands.literal("true")
                            .executes(ctx -> setInfiniburn(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"), true)))
                        .then(Commands.literal("false")
                            .executes(ctx -> setInfiniburn(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"), false)))))
                .then(Commands.literal("fast_lava")
                    .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .then(Commands.literal("true")
                            .executes(ctx -> setFastLava(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"), true)))
                        .then(Commands.literal("false")
                            .executes(ctx -> setFastLava(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"), false)))))
                .then(Commands.literal("water_evaporates")
                    .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .then(Commands.literal("true")
                            .executes(ctx -> setWaterEvaporates(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"), true)))
                        .then(Commands.literal("false"
                            ).executes(ctx -> setWaterEvaporates(ctx.getSource(),
                                DimensionArgument.getDimension(ctx, "dimension"), false)))))
        );
    }

    private static ChatFormatting weatherColor(WeatherManager.WeatherState state) {
        return switch (state) {
            case CLEAR -> ChatFormatting.WHITE;
            case RAIN -> ChatFormatting.AQUA;
            case THUNDER -> ChatFormatting.YELLOW;
        };
    }

    static int dimOrder(ServerLevel level) {
        if (level.dimension() == Level.OVERWORLD) return 0;
        if (level.dimension() == Level.NETHER)    return 1;
        if (level.dimension() == Level.END)        return 2;
        return 3;
    }

    static ChatFormatting dimColor(ServerLevel level) {
        if (level.dimension() == Level.OVERWORLD) return ChatFormatting.DARK_GREEN;
        if (level.dimension() == Level.NETHER)    return ChatFormatting.DARK_RED;
        if (level.dimension() == Level.END)        return ChatFormatting.LIGHT_PURPLE;
        return ChatFormatting.GOLD;
    }

    static MutableComponent dimComponent(ServerLevel level) {
        return Component.literal(level.dimension().identifier().toString())
            .withStyle(dimColor(level));
    }

    private static int setWeather(CommandSourceStack source, ServerLevel level,
                                   WeatherManager.WeatherState state) {
        DimensionLevelWeather.WEATHER.setState(level.dimension(), state, level);
        source.sendSuccess(() -> Component.empty()
            .append(Component.literal("Weather in ").withStyle(ChatFormatting.GRAY))
            .append(dimComponent(level))
            .append(Component.literal(" set to ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(state.name().toLowerCase()).withStyle(weatherColor(state))), true);
        return 1;
    }

    private static int setAllWeather(CommandSourceStack source, WeatherManager.WeatherState state) {
        for (ServerLevel level : source.getServer().getAllLevels()) {
            DimensionLevelWeather.WEATHER.setState(level.dimension(), state, level);
        }
        source.sendSuccess(() -> Component.empty()
            .append(Component.literal("Weather in all dimensions set to ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(state.name().toLowerCase()).withStyle(weatherColor(state))), true);
        return 1;
    }

    private static int setAdvanceWeather(CommandSourceStack source, ServerLevel level, boolean value) {
        DimensionLevelWeather.WEATHER.getSavedData().setAdvanceWeather(level.dimension(), value);
        boolean defaultValue = level.dimension() == Level.OVERWORLD;
        boolean globalAdvance = level.getGameRules().get(GameRules.ADVANCE_WEATHER);
        source.sendSuccess(() -> Component.empty()
            .append(Component.literal("advance_weather for ").withStyle(ChatFormatting.GRAY))
            .append(dimComponent(level))
            .append(Component.literal(" set to ").withStyle(ChatFormatting.GRAY))
            .append(optionalBoolDisplay(Optional.of(value), defaultValue))
            .append(globalAdvance ? Component.empty()
                : Component.literal(" (warning: advance_weather gamerule is false)")
                    .withStyle(ChatFormatting.YELLOW)), true);
        return 1;
    }

    private static int setInfiniburn(CommandSourceStack source, ServerLevel level, boolean value) {
        DimensionLevelWeather.WEATHER.getSavedData().setInfiniburn(level.dimension(), value);
        boolean defaultValue = level.dimensionType().infiniburn().iterator().hasNext();
        source.sendSuccess(() -> Component.empty()
            .append(Component.literal("infiniburn for ").withStyle(ChatFormatting.GRAY))
            .append(dimComponent(level))
            .append(Component.literal(" set to ").withStyle(ChatFormatting.GRAY))
            .append(optionalBoolDisplay(Optional.of(value), defaultValue)), true);
        return 1;
    }

    private static int setFastLava(CommandSourceStack source, ServerLevel level, boolean value) {
        DimensionLevelWeather.WEATHER.getSavedData().setFastLava(level.dimension(), value);
        boolean defaultValue = level.dimensionType().attributes()
            .applyModifier(EnvironmentAttributes.FAST_LAVA, false);
        source.sendSuccess(() -> Component.empty()
            .append(Component.literal("fast_lava for ").withStyle(ChatFormatting.GRAY))
            .append(dimComponent(level))
            .append(Component.literal(" set to ").withStyle(ChatFormatting.GRAY))
            .append(optionalBoolDisplay(Optional.of(value), defaultValue)), true);
        return 1;
    }

    private static int setWaterEvaporates(CommandSourceStack source, ServerLevel level, boolean value) {
        DimensionLevelWeather.WEATHER.getSavedData().setWaterEvaporates(level.dimension(), value);
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            DimensionLevelWeather.WEATHER.sendWaterEvaporatesSync(player);
        }
        boolean defaultValue = level.dimensionType().attributes()
            .applyModifier(EnvironmentAttributes.WATER_EVAPORATES, false);
        source.sendSuccess(() -> Component.empty()
            .append(Component.literal("water_evaporates for ").withStyle(ChatFormatting.GRAY))
            .append(dimComponent(level))
            .append(Component.literal(" set to ").withStyle(ChatFormatting.GRAY))
            .append(optionalBoolDisplay(Optional.of(value), defaultValue)), true);
        return 1;
    }

    static MutableComponent vanillaDefault() {
        return Component.literal(" [vanilla default]").withStyle(ChatFormatting.GRAY);
    }

    static MutableComponent optionalBoolDisplay(Optional<Boolean> value, boolean defaultValue) {
        boolean effective = value.orElse(defaultValue);
        ChatFormatting style = effective == defaultValue
            ? (effective ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY)
            : (effective ? ChatFormatting.GREEN : ChatFormatting.RED);
        MutableComponent result = Component.literal(String.valueOf(effective)).withStyle(style);
        if (effective == defaultValue) result = result.append(vanillaDefault());
        return result;
    }

    private static MutableComponent formatDimWeather(ServerLevel level,
                                                     WeatherManager.WeatherState state,
                                                     Optional<Boolean> advance,
                                                     Optional<Boolean> infiniburn,
                                                     Optional<Boolean> fastLava,
                                                     Optional<Boolean> waterEvaporates) {
        boolean defaultAdvance = level.dimension() == Level.OVERWORLD;
        boolean defaultInfiniburn = level.dimensionType().infiniburn().iterator().hasNext();
        boolean defaultFastLava = level.dimensionType().attributes()
            .applyModifier(EnvironmentAttributes.FAST_LAVA, false);
        boolean defaultWaterEvaporates = level.dimensionType().attributes()
            .applyModifier(EnvironmentAttributes.WATER_EVAPORATES, false);

        return Component.empty()
            .append(Component.literal("[").withStyle(ChatFormatting.GRAY))
            .append(dimComponent(level))
            .append(Component.literal("]\n").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("  weather: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(state.name().toLowerCase()).withStyle(weatherColor(state)))
            .append(Component.literal("\n  advance_weather: ").withStyle(ChatFormatting.GRAY))
            .append(optionalBoolDisplay(advance, defaultAdvance))
            .append(Component.literal("\n  infiniburn: ").withStyle(ChatFormatting.GRAY))
            .append(optionalBoolDisplay(infiniburn, defaultInfiniburn))
            .append(Component.literal("\n  fast_lava: ").withStyle(ChatFormatting.GRAY))
            .append(optionalBoolDisplay(fastLava, defaultFastLava))
            .append(Component.literal("\n  water_evaporates: ").withStyle(ChatFormatting.GRAY))
            .append(optionalBoolDisplay(waterEvaporates, defaultWaterEvaporates));
    }

    private static int queryAll(CommandSourceStack source) {
        List<ServerLevel> levels = new ArrayList<>();
        source.getServer().getAllLevels().forEach(levels::add);
        levels.sort(Comparator.comparingInt(WeatherCommand::dimOrder));
        MutableComponent output = Component.literal("Dimension weather:\n").withStyle(ChatFormatting.AQUA);
        for (ServerLevel level : levels) {
            output = output.append(buildQueryComponent(level)).append(Component.literal("\n"));
        }
        final MutableComponent finalOutput = output;
        source.sendSuccess(() -> finalOutput, false);
        return 1;
    }

    private static int queryWeather(CommandSourceStack source, ServerLevel level) {
        source.sendSuccess(() -> buildQueryComponent(level), false);
        return 1;
    }

    private static MutableComponent buildQueryComponent(ServerLevel level) {
        WeatherSavedData data = DimensionLevelWeather.WEATHER.getSavedData();
        WeatherManager.WeatherState state = DimensionLevelWeather.WEATHER.getState(level.dimension());
        Optional<Boolean> advance = data == null ? Optional.empty() : data.getAdvanceWeatherOptional(level.dimension());
        Optional<Boolean> infiniburn = data == null ? Optional.empty() : data.getInfiniburn(level.dimension());
        Optional<Boolean> fastLava = data == null ? Optional.empty() : data.getFastLava(level.dimension());
        Optional<Boolean> waterEvaporates = data == null ? Optional.empty() : data.getWaterEvaporates(level.dimension());
        return formatDimWeather(level, state, advance, infiniburn, fastLava, waterEvaporates);
    }
}
