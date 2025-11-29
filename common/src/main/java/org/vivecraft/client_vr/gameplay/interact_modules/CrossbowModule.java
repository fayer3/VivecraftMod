package org.vivecraft.client_vr.gameplay.interact_modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.vivecraft.api.client.ItemInUseInteractModule;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.VRData;
import org.vivecraft.client_vr.gameplay.weapons.CrossbowWeapon;
import org.vivecraft.client_vr.render.helpers.DebugRenderHelper;
import org.vivecraft.common.utils.MathUtils;

import javax.annotation.Nullable;

public class CrossbowModule implements ItemInUseInteractModule, DebugRenderModule {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("vivecraft", "crossbow");

    private final ClientDataHolderVR dh;
    private final boolean[] isPressed = new boolean[2];
    private final int[] progress = new int[2];


    public CrossbowModule(ClientDataHolderVR dh) {
        this.dh = dh;
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public int getPriority() {
        // before default
        return 900;
    }

    private boolean isAtReloadPosition(InteractionHand hand) {
        VRData data = this.dh.vrPlayer.vrdata_room_pre;
        VRData.VRDevicePose otherController = data.getController(1 - hand.ordinal());
        return otherController.getPositionF().add(otherController.getCustomVector(CrossbowWeapon.RELOAD_POSITION))
            .distanceSquared(data.getController(hand.ordinal()).getPositionF()) < 0.01F;
    }

    private boolean isNonChargedCrossbow(ItemStack itemStack) {
        return itemStack.is(Items.CROSSBOW) && !CrossbowItem.isCharged(itemStack);
    }

    @Override
    public boolean onHoldTick(LocalPlayer player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(InteractionHand.values()[1 - hand.ordinal()]);
        if (itemStack.is(Items.CROSSBOW)) {
            if (!CrossbowItem.isCharged(itemStack)) {
                float progress = (itemStack.getUseDuration(player) - player.getUseItemRemainingTicks()) /
                    (float) CrossbowItem.getChargeDuration(itemStack, player);
                int stage = 0;
                if (progress >= 1F) {
                    stage = 2;
                } else if (progress >= 0.58F) {
                    stage = 1;
                }
                if (stage != this.progress[hand.ordinal()]) {
                    this.dh.vr.triggerHapticPulse(hand.ordinal(), 125);
                    if (stage == 2) {
                        this.dh.vr.triggerHapticPulse(1 - hand.ordinal(), 125);
                    }
                }
                this.progress[hand.ordinal()] = stage;
                // continue holding
                return true;
            }
            // fully charged
            //this.dh.vr.triggerHapticPulse(hand.ordinal(), 500);
            return false;
        }
        return false;
    }

    @Override
    public boolean isActive(LocalPlayer player, InteractionHand hand, Vec3 handPosition) {
        ItemStack itemStack = player.getItemInHand(InteractionHand.values()[1 - hand.ordinal()]);
        return isNonChargedCrossbow(itemStack) && isAtReloadPosition(hand);
    }

    @Override
    public void reset(@Nullable LocalPlayer player, InteractionHand hand) {
        this.isPressed[hand.ordinal()] = false;
        this.progress[hand.ordinal()] = 0;
    }

    @Override
    public boolean onPress(LocalPlayer player, InteractionHand hand) {
        this.isPressed[hand.ordinal()] = true;
        this.progress[hand.ordinal()] = 0;

        Minecraft.getInstance().gameMode.useItem(player, InteractionHand.values()[1 - hand.ordinal()]);
        return false;
    }

    @Override
    public void onRelease(@Nullable LocalPlayer player, InteractionHand hand) {
        reset(player, hand);
    }

    @Override
    public boolean swingsArm() {
        return false;
    }

    @Override
    public void renderDebug(boolean isActive) {
        for (int hand = 0; hand < 2; hand++) {
            if (isNonChargedCrossbow(Minecraft.getInstance().player.getItemInHand(InteractionHand.values()[hand]))) {
                VRData world = this.dh.vrPlayer.getVRDataWorld();
                // no origin offset, since the camera is world relative
                DebugRenderHelper.renderSphere(MathUtils.subtractToVector3f(
                        world.getController(hand).getPosition()
                            .add(new Vec3(world.getController(hand).getCustomVector(CrossbowWeapon.RELOAD_POSITION))),
                        world.getEye(this.dh.currentPass).getPosition()), 0.05F * world.worldScale,
                    isAtReloadPosition(InteractionHand.values()[1 - hand]) ? MathUtils.GREEN : MathUtils.RED);
            }
        }
    }
}
