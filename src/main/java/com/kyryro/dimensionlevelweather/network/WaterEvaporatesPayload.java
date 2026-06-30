package com.kyryro.dimensionlevelweather.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public record WaterEvaporatesPayload(Map<ResourceKey<Level>, Boolean> overrides)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<WaterEvaporatesPayload> TYPE =
        new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath("dimension-level-weather", "water_evaporates"));

    private static final StreamCodec<ByteBuf, ResourceKey<Level>> DIMENSION_KEY_CODEC =
        ResourceKey.streamCodec(Registries.DIMENSION);

    public static final StreamCodec<RegistryFriendlyByteBuf, WaterEvaporatesPayload> STREAM_CODEC =
        StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.overrides().size());
                p.overrides().forEach((key, val) -> {
                    DIMENSION_KEY_CODEC.encode(buf, key);
                    buf.writeBoolean(val);
                });
            },
            buf -> {
                int size = buf.readVarInt();
                Map<ResourceKey<Level>, Boolean> map = new HashMap<>(size);
                for (int i = 0; i < size; i++) {
                    ResourceKey<Level> key = DIMENSION_KEY_CODEC.decode(buf);
                    boolean val = buf.readBoolean();
                    map.put(key, val);
                }
                return new WaterEvaporatesPayload(map);
            }
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
