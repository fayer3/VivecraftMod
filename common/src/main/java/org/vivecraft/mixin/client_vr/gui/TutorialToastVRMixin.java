package org.vivecraft.mixin.client_vr.gui;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TutorialToast.class)
public abstract class TutorialToastVRMixin implements Toast {

    @Shadow
    @Final
    private Component title;

    @Shadow
    @Final
    private Component message;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiComponent;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V", shift = At.Shift.AFTER))
    private void vivecraft$extendToast(
        PoseStack poseStack, ToastComponent toastComponent, long timeSinceLastVisible,
        CallbackInfoReturnable<Visibility> cir, @Share("offset") LocalRef<Integer> offset)
    {
        int width = Math.max(toastComponent.getMinecraft().font.width(this.title),
            this.message != null ? toastComponent.getMinecraft().font.width(this.message) : 0) + 34;
        offset.set(Math.min(this.width() - width, 0));

        if (offset.get() < 0) {
            // draw a bigger toast from right to left, to override the left border
            for (int i = offset.get() - (this.width() - 8) * (offset.get() / (this.width() - 8));
                 i >= offset.get(); i -= this.width() - 8) {
                GuiComponent.blit(poseStack, i, 0, 0, 96, this.width() - 4, this.height());
            }
        }
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/toasts/TutorialToast$Icons;render(Lcom/mojang/blaze3d/vertex/PoseStack;II)V"), index = 1)
    private int vivecraft$offsetIcon(int x, @Share("offset") LocalRef<Integer> offset) {
        return x + offset.get();
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;draw(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;FFI)I"), index = 2)
    private float vivecraft$offsetText(float x, @Share("offset") LocalRef<Integer> offset) {
        return x + offset.get();
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiComponent;fill(Lcom/mojang/blaze3d/vertex/PoseStack;IIIII)V"), index = 1)
    private int vivecraft$offsetProgressStart(int x, @Share("offset") LocalRef<Integer> offset) {
        return x + offset.get();
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiComponent;fill(Lcom/mojang/blaze3d/vertex/PoseStack;IIIII)V", ordinal = 1), index = 3)
    private int vivecraft$offsetProgressEnd(int x, @Share("offset") LocalRef<Integer> offset) {
        return x + offset.get() - (int) ((float) x / TutorialToast.PROGRESS_BAR_WIDTH * offset.get());
    }
}
