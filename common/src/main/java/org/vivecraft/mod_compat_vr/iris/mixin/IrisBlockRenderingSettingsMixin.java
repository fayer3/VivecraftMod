package org.vivecraft.mod_compat_vr.iris.mixin;

import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vivecraft.client_vr.ClientDataHolderVR;

@Mixin(BlockRenderingSettings.class)
public class IrisBlockRenderingSettingsMixin {
    @Inject(at = @At("HEAD"), method = "getAmbientOcclusionLevel", remap = false, cancellable = true)
    private void vivecrat$defaulktAOforMenu(CallbackInfoReturnable<Float> cir) {
        if (ClientDataHolderVR.getInstance().menuWorldRenderer != null && ClientDataHolderVR.getInstance().menuWorldRenderer.isOnBuilderThread()) {
            cir.setReturnValue(1.0F);
        }
    }
}
