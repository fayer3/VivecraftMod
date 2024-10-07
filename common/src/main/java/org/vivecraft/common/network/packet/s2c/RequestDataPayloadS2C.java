package org.vivecraft.common.network.packet.s2c;

import org.vivecraft.common.network.packet.PayloadIdentifier;
import org.vivecraft.common.network.packet.VivecraftPayloadS2C;

/**
 * indicates that the server wants vr data from the client
 */
public record RequestDataPayloadS2C() implements VivecraftPayloadS2C {

    @Override
    public PayloadIdentifier payloadId() {
        return PayloadIdentifier.REQUESTDATA;
    }
}
