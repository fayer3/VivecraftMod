package org.vivecraft.mod_compat_vr.optifine.mixin;

import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.mod_compat_vr.optifine.OptifineHelper;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LiquidBlockRenderer.class)
public class OptifineLiquidBlockRendererMixin {
    // needed for menuworlds water rendering
    @Redirect(method = "tesselate", at = @At(value = "INVOKE", target = "Lnet/optifine/Config;isRenderRegions()Z"))
    private boolean vivecraft$optifineChunkClipping() {
        return OptifineHelper.isRenderRegions() && (ClientDataHolderVR.getInstance().menuWorldRenderer == null || !ClientDataHolderVR.getInstance().menuWorldRenderer.isOnBuilderThread());
    }
}
