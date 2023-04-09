package org.vivecraft.mixin.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vivecraft.ClientDataHolder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.vivecraft.gui.VivecraftClickEvent;

@Mixin(Screen.class)
public abstract class ScreenVRMixin extends AbstractContainerEventHandler implements Widget {

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;fillGradient(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V"), method = "renderBackground(Lcom/mojang/blaze3d/vertex/PoseStack;I)V")
    public void vrBackground(Screen instance, PoseStack poseStack, int i, int j, int k, int l, int m, int n) {
        if (ClientDataHolder.getInstance().vrSettings != null && !ClientDataHolder.getInstance().vrSettings.menuBackground) {
            this.fillGradient(poseStack, i, j, k, l, 0, 0);
        } else {
            this.fillGradient(poseStack, i, j, k, l, m, n);
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/ClickEvent;getAction()Lnet/minecraft/network/chat/ClickEvent$Action;", ordinal = 0), method = "handleComponentClicked(Lnet/minecraft/network/chat/Style;)Z", cancellable = true)
    public void handleVivecraftClickEvents(Style style, CallbackInfoReturnable<Boolean> cir) {
        if (style.getClickEvent() instanceof VivecraftClickEvent) {
            VivecraftClickEvent.VivecraftAction action = ((VivecraftClickEvent) style.getClickEvent()).getVivecraftAction();
            if (action == VivecraftClickEvent.VivecraftAction.OPEN_SCREEN) {
                Minecraft.getInstance().setScreen((Screen) ((VivecraftClickEvent) style.getClickEvent()).getVivecraftValue());
                cir.setReturnValue(true);
            }
        }
    }

}