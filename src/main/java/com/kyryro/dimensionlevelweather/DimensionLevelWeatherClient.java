package com.kyryro.dimensionlevelweather;

import com.kyryro.dimensionlevelweather.network.WaterEvaporatesPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class DimensionLevelWeatherClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
            WaterEvaporatesPayload.TYPE,
            (payload, context) ->
                DimensionLevelWeather.WEATHER.setClientWaterEvaporates(payload.overrides()));
    }
}
