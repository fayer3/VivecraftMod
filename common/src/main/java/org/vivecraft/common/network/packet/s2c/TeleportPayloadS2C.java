package org.vivecraft.common.network.packet.s2c;

import org.vivecraft.common.network.packet.PayloadIdentifier;
import org.vivecraft.common.network.packet.VivecraftPayloadS2C;

/**
 * indicates that the server supports direct teleports
 */
public record TeleportPayloadS2C() implements VivecraftPayloadS2C {

    @Override
    public PayloadIdentifier payloadId() {
        return PayloadIdentifier.TELEPORT;
    }
}
