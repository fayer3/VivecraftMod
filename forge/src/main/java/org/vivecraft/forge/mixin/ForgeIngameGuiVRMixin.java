package org.vivecraft.forge.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.IIngameOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vivecraft.client_vr.extensions.GuiExtension;
import org.vivecraft.client_xr.render_pass.RenderPassType;

@Mixin(ForgeIngameGui.class)
public abstract class ForgeIngameGuiVRMixin {
    @Inject(method = "pre(Lnet/minecraftforge/client/gui/IIngameOverlay;Lcom/mojang/blaze3d/vertex/PoseStack;)Z", at = @At("HEAD"), remap = false, cancellable = true)
    private void vivecraft$noOverlaysOnGui(
        IIngameOverlay overlay, PoseStack poseStack, CallbackInfoReturnable<Boolean> cir)
    {
        if (RenderPassType.isGuiOnly() &&
            (overlay == ForgeIngameGui.VIGNETTE_ELEMENT ||
                overlay == ForgeIngameGui.SPYGLASS_ELEMENT ||
                overlay == ForgeIngameGui.HELMET_ELEMENT ||
                overlay == ForgeIngameGui.FROSTBITE_ELEMENT ||
                overlay == ForgeIngameGui.PORTAL_ELEMENT
            ))
        {
            cir.setReturnValue(true);
        }
    }

    @ModifyExpressionValue(method = "renderPlayerList", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private boolean vivecraft$toggleableTabListForge(boolean original) {
        return original || ((GuiExtension) this).vivecraft$getShowPlayerList();
    }
}
