package org.vivecraft.mixin.client_vr;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.vivecraft.client.VivecraftVRMod;
import org.vivecraft.client_vr.VRState;

@Mixin(Options.class)
public abstract class OptionsVRMixin {
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/apache/commons/lang3/ArrayUtils;addAll([Ljava/lang/Object;[Ljava/lang/Object;)[Ljava/lang/Object;", remap = false), remap = true)
    private Object[] vivecraft$processKeyMappings(Object[] array1, Object[] array2, Operation<Object[]> original) {
        return VivecraftVRMod.INSTANCE.initializeBindings((KeyMapping[]) original.call(array1, array2));
    }

    @WrapOperation(method = "broadcastOptions", at = @At(value = "NEW", target = "net/minecraft/network/protocol/game/ServerboundClientInformationPacket"))
    private ServerboundClientInformationPacket vivecraft$alwaysRightMain(
        String language, int viewDistance, ChatVisiblity chatVisibility, boolean chatColors, int modelCustomisation,
        HumanoidArm mainHand, boolean textFilteringEnabled, boolean allowsListing,
        Operation<ServerboundClientInformationPacket> original)
    {
        return original.call(language, viewDistance, chatVisibility, chatColors, modelCustomisation,
            VRState.VR_RUNNING ? HumanoidArm.RIGHT : mainHand,
            textFilteringEnabled, allowsListing);
    }
}
