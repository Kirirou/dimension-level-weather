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

    private static int setWeather(CommandSourceStack source, ServerLevel level,
                                   WeatherManager.WeatherState state) {
        DimensionLevelWeather.WEATHER.setState(level.dimension(), state, level);
        source.sendSuccess(() -> Component.literal(
            "Weather in " + level.dimension().identifier() + " set to "
                + state.name().toLowerCase()), true);
        return 1;
    }

    private static int setAllWeather(CommandSourceStack source, WeatherManager.WeatherState state) {
        for (ServerLevel level : source.getServer().getAllLevels()) {
            DimensionLevelWeather.WEATHER.setState(level.dimension(), state, level);
        }
        source.sendSuccess(() -> Component.literal(
            "Weather in all dimensions set to " + state.name().toLowerCase()), true);
        return 1;
    }

    private static int setAdvanceWeather(CommandSourceStack source, ServerLevel level, boolean value) {
        DimensionLevelWeather.WEATHER.getSavedData().setAdvanceWeather(level.dimension(), value);

        boolean globalAdvance = level.getGameRules().get(GameRules.ADVANCE_WEATHER);
        String warning = globalAdvance ? "" :
            " (warning: advance_weather gamerule is false)";

        source.sendSuccess(() -> Component.literal(
            "advance_weather for " + level.dimension().identifier()
            + " set to " + value + warning), true);
        return 1;
    }

    private static int setInfiniburn(CommandSourceStack source, ServerLevel level, boolean value) {
        DimensionLevelWeather.WEATHER.getSavedData().setInfiniburn(level.dimension(), value);
        source.sendSuccess(() -> Component.literal(
            "infiniburn for " + level.dimension().identifier() + " set to " + value), true);
        return 1;
    }

    private static int setFastLava(CommandSourceStack source, ServerLevel level, boolean value) {
        DimensionLevelWeather.WEATHER.getSavedData().setFastLava(level.dimension(), value);
        source.sendSuccess(() -> Component.literal(
            "fast_lava for " + level.dimension().identifier() + " set to " + value), true);
        return 1;
    }

    private static int setWaterEvaporates(CommandSourceStack source, ServerLevel level, boolean value) {
        DimensionLevelWeather.WEATHER.getSavedData().setWaterEvaporates(level.dimension(), value);
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            DimensionLevelWeather.WEATHER.sendWaterEvaporatesSync(player);
        }
        source.sendSuccess(() -> Component.literal(
            "water_evaporates for " + level.dimension().identifier() + " set to " + value), true);
        return 1;
    }

    private static String optionalBoolDisplay(Optional<Boolean> value, boolean defaultValue) {
        return value.map(Object::toString).orElse(defaultValue + " (vanilla default)");
    }

    private static MutableComponent formatDimWeather(ServerLevel level,
                                                     WeatherManager.WeatherState state,
                                                     boolean advance,
                                                     Optional<Boolean> infiniburn,
                                                     Optional<Boolean> fastLava,
                                                     Optional<Boolean> waterEvaporates) {
        boolean defaultInfiniburn = level.dimensionType().infiniburn().iterator().hasNext();
        boolean defaultFastLava = level.dimensionType().attributes()
            .applyModifier(EnvironmentAttributes.FAST_LAVA, false);
        boolean defaultWaterEvaporates = level.dimensionType().attributes()
            .applyModifier(EnvironmentAttributes.WATER_EVAPORATES, false);

        return Component.empty()
            .append(Component.literal("[").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(level.dimension().identifier().toString())
                .withStyle(ChatFormatting.GOLD))
            .append(Component.literal("]\n").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("  weather: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(state.name().toLowerCase()).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\n  advance_weather: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(advance + (advance == (level.dimension() == Level.OVERWORLD) ? " (vanilla default)" : ""))
                .withStyle(advance ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY))
            .append(Component.literal("\n  infiniburn: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(optionalBoolDisplay(infiniburn, defaultInfiniburn)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\n  fast_lava: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(optionalBoolDisplay(fastLava, defaultFastLava)).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\n  water_evaporates: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(optionalBoolDisplay(waterEvaporates, defaultWaterEvaporates)).withStyle(ChatFormatting.WHITE));
    }

    private static int queryAll(CommandSourceStack source) {
        MutableComponent output = Component.literal("Dimension weather:\n").withStyle(ChatFormatting.AQUA);
        for (ServerLevel level : source.getServer().getAllLevels()) {
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
        boolean advance = data == null || data.getAdvanceWeather(level.dimension());
        Optional<Boolean> infiniburn = data == null ? Optional.empty() : data.getInfiniburn(level.dimension());
        Optional<Boolean> fastLava = data == null ? Optional.empty() : data.getFastLava(level.dimension());
        Optional<Boolean> waterEvaporates = data == null ? Optional.empty() : data.getWaterEvaporates(level.dimension());
        return formatDimWeather(level, state, advance, infiniburn, fastLava, waterEvaporates);
    }
}
