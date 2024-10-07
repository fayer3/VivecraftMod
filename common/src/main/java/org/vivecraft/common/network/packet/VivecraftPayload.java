package org.vivecraft.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;

public interface VivecraftPayload {

    /**
     * writes this data packet to the given buffer
     * @param buffer Buffer to write to
     */
    default void write(FriendlyByteBuf buffer) {
        buffer.writeByte(id().ordinal());
    }

    /**
     * returns the PacketIdentifier associated with this packet
     */
    PayloadIdentifier id();
}
