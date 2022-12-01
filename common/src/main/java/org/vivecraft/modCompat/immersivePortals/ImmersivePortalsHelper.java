package org.vivecraft.modCompat.immersivePortals;

import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

import java.util.List;

public class ImmersivePortalsHelper {
    public static boolean isRenderingPortal(){
        return PortalRendering.isRendering();
    }

    public static boolean shouldRenderSelf(){
        return IPGlobal.renderYourselfInPortal && isRenderingPortal();
    }

    public static BlockHitResult raytrace(BlockHitResult blockhitresult, Level level, Vec3 from, Vec3 to, Player player, boolean includeGlobalPortals){
        // portals have a collider but not an outline
        Tuple<BlockHitResult, List<Portal>> hitPortals =  IPMcHelper.rayTrace(level, new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player), includeGlobalPortals);
        // check if the raytrace hit any portals
        if (!hitPortals.getB().isEmpty()) {

            // only bother about the first portal
            Portal portal = hitPortals.getB().get(0);

            // get the portal crossing point, and move a bit further in
            Vec3 hitPos = portal.getNearestPointInPortal(to).subtract(portal.getNormal().scale(0.1));

            // check for block to stand on from the portal
            blockhitresult = level.clip(new ClipContext(hitPos, hitPos.subtract(0.0,10.0,0.0), ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY , player));

            // check if the player can use that portal
            // if no block is there, treat the teleport as invalid
            if (!portal.canTeleportEntity(player) || (blockhitresult != null && blockhitresult.getType() == HitResult.Type.MISS)) {
                return null;
            }
        }
        return blockhitresult;
    }

    public static boolean isBlockPortal(Block block){
         return block == PortalPlaceholderBlock.instance;
    }

}
