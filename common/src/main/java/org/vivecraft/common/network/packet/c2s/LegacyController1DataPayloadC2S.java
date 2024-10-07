package org.vivecraft.common.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import org.vivecraft.common.network.Pose;
import org.vivecraft.common.network.packet.PayloadIdentifier;
import org.vivecraft.common.network.packet.VivecraftPayloadC2S;

/**
 * legacy packet, holds the reversed hand flag and the offhand controller pose
 * @param reverseHands if the player has reversed hands set
 * @param controller0Pose pose of the players main controller
 */
public record LegacyController1DataPayloadC2S(boolean reverseHands, Pose controller0Pose) implements VivecraftPayloadC2S {
    @Override
    public PayloadIdentifier payloadId() {
        return PayloadIdentifier.CONTROLLER1DATA;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeByte(payloadId().ordinal());
        buffer.writeBoolean(this.reverseHands);
        this.controller0Pose.serialize(buffer);
    }

    public static LegacyController1DataPayloadC2S read(FriendlyByteBuf buffer) {
        return new LegacyController1DataPayloadC2S(buffer.readBoolean(), Pose.deserialize(buffer));
    }
}
