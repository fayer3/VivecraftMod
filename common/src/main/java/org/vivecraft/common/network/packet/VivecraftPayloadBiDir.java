package org.vivecraft.common.network.packet;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.vivecraft.common.network.packet.bidir.RawVivecraftPayload;

import javax.annotation.Nullable;

/**
 * Vivecraft network payload that holds a raw packet, has no specific flow direction
 * @param rawPayload raw payload with actual data
 */
public record VivecraftPayloadBiDir(@Nullable VivecraftPayloadC2S C2SPayload,
                                    @Nullable VivecraftPayloadS2C S2CPayload,
                                    @Nullable RawVivecraftPayload rawPayload) implements VivecraftPayload
{

    public VivecraftPayloadBiDir(FriendlyByteBuf buffer) {
        this(null, null, RawVivecraftPayload.read(buffer));
    }

    public VivecraftPayloadBiDir(VivecraftPayloadC2S C2SPayload) {
        this(C2SPayload, null, null);
    }

    public VivecraftPayloadBiDir(VivecraftPayloadS2C S2CPayload) {
        this(null, S2CPayload, null);
    }

    /**
     * writes the packet to {@code buffer}
     * @param buffer buffer to write to
     */
    @Override
    public void write(FriendlyByteBuf buffer) {
        if (this.C2SPayload != null){
            this.C2SPayload.write(buffer);
        } else if (this.S2CPayload != null) {
            this.S2CPayload.write(buffer);
        } else if (this.rawPayload != null) {
            this.rawPayload.write(buffer);
        }
    }

    @Override
    public PayloadIdentifier payloadId() {
        if (this.C2SPayload != null){
            return this.C2SPayload.payloadId();
        } else if (this.S2CPayload != null) {
            return this.S2CPayload.payloadId();
        } else if (this.rawPayload != null) {
            return this.rawPayload.payloadId();
        }
        throw new IllegalStateException("Vivecraft: BiDir packed has no data");
    }

    public VivecraftPayloadC2S getC2SPayload() {
        if (this.C2SPayload != null) {
            return this.C2SPayload;
        } else if (this.rawPayload != null) {
            FriendlyByteBuf buffer = this.rawPayload.asByteBuf();
            VivecraftPayloadC2S C2S = VivecraftPayloadC2S.readPacket(buffer);
            buffer.release();
            return C2S;
        }
        throw new IllegalStateException("Vivecraft: BiDir packed has no C2SPayload");
    }

    public VivecraftPayloadS2C getS2CPayload() {
        if (this.S2CPayload != null) {
            return this.S2CPayload;
        } else if (this.rawPayload != null) {
            FriendlyByteBuf buffer = this.rawPayload.asByteBuf();
            VivecraftPayloadS2C S2C = VivecraftPayloadS2C.readPacket(buffer);
            buffer.release();
            return S2C;
        }
        throw new IllegalStateException("Vivecraft: BiDir packed has no S2CPayload");
    }
}
