package org.vivecraft.common.network.packet.c2s;

import org.vivecraft.common.network.packet.PayloadIdentifier;
import org.vivecraft.common.network.packet.VivecraftPayloadC2S;

/**
 * indicates that the client is currently climbing
 */
public record ClimbingPayloadC2S() implements VivecraftPayloadC2S {

    @Override
    public PayloadIdentifier payloadId() {
        return PayloadIdentifier.CLIMBING;
    }
}
