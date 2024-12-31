package org.vivecraft.mixin.client_vr.renderer;

import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.render.RenderPass;
import org.vivecraft.client_xr.render_pass.RenderPassType;

@Mixin(WeatherEffectRenderer.class)
public class WeatherEffectRendererVRMixin {
    @ModifyVariable(method = "collectColumnInstances", at = @At("HEAD"), argsOnly = true)
    private Vec3 vivecraft$rainCenterPos(Vec3 cameraPos) {
        if (!RenderPassType.isVanilla() && (ClientDataHolderVR.getInstance().currentPass == RenderPass.LEFT ||
            ClientDataHolderVR.getInstance().currentPass == RenderPass.RIGHT
        ))
        {
            return ClientDataHolderVR.getInstance().vrPlayer.vrdata_world_render.getEye(RenderPass.CENTER)
                .getPosition();
        } else {
            return cameraPos;
        }
    }
}
