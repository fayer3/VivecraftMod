package org.vivecraft.common.network.packet.c2s;

import net.minecraft.network.FriendlyByteBuf;
import org.vivecraft.common.network.packet.PayloadIdentifier;

/**
 * packet that holds the Vivecraft client version, mode and network version support
 * @param version Version String of the client
 * @param vr if the client was in vr when they connected
 * @param maxVersion maximum supported network protocol version
 * @param minVersion minimum supported network protocol version
 * @param legacy if the client is a legacy client, before the network protocol version was added
 */
public record VersionPayloadC2S(String version, boolean vr, int maxVersion, int minVersion,
                                boolean legacy) implements VivecraftPayloadC2S
{

    public VersionPayloadC2S(String version, boolean vr, int maxVersion, int minVersion) {
        this(version, vr, maxVersion, minVersion, false);
    }

    @Override
    public PayloadIdentifier payloadId() {
        return PayloadIdentifier.VERSION;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeByte(payloadId().ordinal());
        buffer.writeUtf(
            String.format("%s %s\n%d\n%d", this.version, this.vr ? "VR" : "NONVR", this.maxVersion, this.minVersion));
    }

    public static VersionPayloadC2S read(FriendlyByteBuf buffer) {
        String[] parts = buffer.readUtf().split("\\n");
        boolean vr = !parts[0]. contains("NONVR");
        if (parts.length >= 3) {
            return new VersionPayloadC2S(parts[0], vr, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), false);
        } else {
            return new VersionPayloadC2S(parts[0], vr, -1, -1, true);
        }
    }
}
