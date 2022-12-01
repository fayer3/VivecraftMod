package org.vivecraft.modCompat.immersivePortals.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.ClientDataHolder;
import org.vivecraft.gameplay.VRPlayer;
import org.vivecraft.render.RenderPass;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.teleportation.ClientTeleportationManager;

@Mixin(ClientTeleportationManager.class)
public class ClientTeleportationManagerMixin {

    @Shadow
    private long lastTeleportGameTime;
    @Shadow
    public long tickTimeForTeleportation;

    @Inject(method = "manageTeleportation", at = @At("HEAD"), cancellable = true, remap = false)
    private void onlyOneTeleport(float tickDelta, CallbackInfo ci){
        if (ClientDataHolder.getInstance().currentPass != RenderPass.LEFT) {
            ci.cancel();
        }
    }

    @Inject(method = "teleportPlayer", at = @At(value = "INVOKE", target = "Lqouteall/imm_ptl/core/McHelper;updateBoundingBox(Lnet/minecraft/world/entity/Entity;)V", shift = At.Shift.AFTER), remap = false)
    private void moveRoomOrigin(Portal portal, CallbackInfo ci){
        Vec3 newPos = portal.transformPoint(VRPlayer.get().roomOrigin);
        Vec3 offset = newPos.subtract(VRPlayer.get().roomOrigin);
        // move all world vrdatas
        VRPlayer.get().vrdata_world_render.origin = VRPlayer.get().vrdata_world_render.origin.add(offset);
        VRPlayer.get().vrdata_world_pre.origin = VRPlayer.get().vrdata_world_pre.origin.add(offset);
        VRPlayer.get().vrdata_world_post.origin = VRPlayer.get().vrdata_world_post.origin.add(offset);
        VRPlayer.get().setRoomOrigin(newPos.x, newPos.y, newPos.z, true);
    }

    @Inject(method = "teleportPlayer", at = @At("HEAD"), remap = false, cancellable = true)
    private void onlyOneTpPerFrame(Portal portal, CallbackInfo ci){
        if (lastTeleportGameTime == tickTimeForTeleportation){
            ci.cancel();
        }
    }

    @Redirect(method = "tryTeleport", at = @At(value = "INVOKE", target = "Lqouteall/imm_ptl/core/teleportation/ClientTeleportationManager;getPlayerHeadPos(F)Lnet/minecraft/world/phys/Vec3;"), remap = false)
    private Vec3 redManageTeleportation1(float tickDelta){
        return VRPlayer.get().vrdata_world_render.eye0.getPosition().add(VRPlayer.get().vrdata_world_render.eye1.getPosition()).scale(0.5F);
    }
    @Redirect(method = "manageTeleportation", at = @At(value = "INVOKE", target = "Lqouteall/imm_ptl/core/teleportation/ClientTeleportationManager;getPlayerHeadPos(F)Lnet/minecraft/world/phys/Vec3;"), remap = false)
    private Vec3 redManageTeleportation2(float tickDelta){
        return VRPlayer.get().vrdata_world_render.eye0.getPosition().add(VRPlayer.get().vrdata_world_render.eye1.getPosition()).scale(0.5F);
    }
}
