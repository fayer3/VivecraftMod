package org.vivecraft.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.vivecraft.common.network.CommonNetworkHelper;
import org.vivecraft.common.network.packet.s2c.VivecraftPayloadS2C;

/**
 * Vivecraft network packet that holds a server to client payload
 * @param payload payload with actual data
 */
public record VivecraftPacketS2C(VivecraftPayloadS2C payload) implements CustomPacketPayload {

    public VivecraftPacketS2C(FriendlyByteBuf buffer) {
        this(VivecraftPayloadS2C.readPacket(buffer));
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
