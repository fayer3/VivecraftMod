package org.vivecraft.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.vivecraft.common.network.CommonNetworkHelper;
import org.vivecraft.common.network.packet.bidirectional.RawVivecraftPayload;

/**
 * Vivecraft network payload that holds a raw packet, has no specific flow direction
 * @param rawPayload raw payload with actual data
 */
public record VivecraftPacketBiDir(RawVivecraftPayload rawPayload) implements CustomPacketPayload {

    public VivecraftPacketBiDir(FriendlyByteBuf buffer) {
        this(RawVivecraftPayload.read(buffer));
    }

    /**
     * writes the packet to {@code buffer}
     * @param buffer buffer to write to
     */
    @Override
    public void write(FriendlyByteBuf buffer) {
        this.rawPayload.write(buffer);
    }

    /**
     * @return ResourceLocation identifying this packet
     */
    @Override
    public ResourceLocation id() {
        return CommonNetworkHelper.CHANNEL;
    }
}
