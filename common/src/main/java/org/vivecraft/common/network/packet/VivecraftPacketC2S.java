package org.vivecraft.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.vivecraft.common.network.CommonNetworkHelper;
import org.vivecraft.common.network.packet.c2s.VivecraftPayloadC2S;

/**
 * Vivecraft network packet that holds a client to server payload
 * @param payload payload with actual data
 */
public record VivecraftPacketC2S(VivecraftPayloadC2S payload) implements CustomPacketPayload {

    public VivecraftPacketC2S(FriendlyByteBuf buffer) {
        this(VivecraftPayloadC2S.readPacket(buffer));
    }

    /**
     * writes the payload to {@code buffer}
     * @param buffer buffer to write to
     */
    @Override
    public void write(FriendlyByteBuf buffer) {
        this.payload.write(buffer);
    }

    /**
     * @return ResourceLocation identifying this packet
     */
    @Override
    public ResourceLocation id() {
        return CommonNetworkHelper.CHANNEL;
    }
}
