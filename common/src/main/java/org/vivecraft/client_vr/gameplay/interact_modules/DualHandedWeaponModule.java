package org.vivecraft.client_vr.gameplay.interact_modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.vivecraft.api.client.DualHandedWeapon;
import org.vivecraft.api.client.HeldInteractModule;
import org.vivecraft.api.data.VRBodyPart;
import org.vivecraft.client.network.ClientNetworking;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.VRData;
import org.vivecraft.client_vr.gameplay.weapons.CrossbowWeapon;
import org.vivecraft.client_vr.render.helpers.DebugRenderHelper;
import org.vivecraft.common.network.packet.c2s.DrawPayloadC2S;
import org.vivecraft.common.utils.MathUtils;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class DualHandedWeaponModule implements HeldInteractModule, DebugRenderModule {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("vivecraft", "dual_handed_weapon");

    private final ClientDataHolderVR dh;
    private final boolean[] isLatchable = new boolean[2];
    private final boolean[] isLatched = new boolean[2];

    private final Set<DualHandedWeapon> registeredWeapons = new HashSet<>();


    public DualHandedWeaponModule(ClientDataHolderVR dh) {
        this.dh = dh;
        this.registeredWeapons.add(new CrossbowWeapon());
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

    @Nullable
    private DualHandedWeapon getActiveWeapon(LocalPlayer player, InteractionHand hand) {
        return this.registeredWeapons.stream().filter(weapon -> weapon.isWeapon(player.getItemInHand(hand))).findFirst()
            .orElse(null);
    }

    private boolean isAtGripPosition(@Nullable DualHandedWeapon weapon, InteractionHand hand) {
        if (weapon != null) {
            VRData data = this.dh.vrPlayer.vrdata_room_pre;
            VRData.VRDevicePose otherController = data.getController(1 - hand.ordinal());
            return otherController.getPositionF().add(otherController.getCustomVector(weapon.getGripPosition()))
                .distanceSquared(data.getController(hand.ordinal()).getPositionF()) < 0.01F;
        } else {
            return false;
        }
    }

    @Override
    public boolean onHoldTick(LocalPlayer player, InteractionHand hand) {
        return this.isLatched[hand.ordinal()] || getActiveWeapon(player, hand) != null;
    }

    @Override
    public boolean isActive(LocalPlayer player, InteractionHand hand, Vec3 handPosition) {
        InteractionHand otherHand =
            hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        DualHandedWeapon otherWeapon = getActiveWeapon(player, otherHand);
        // offhand can grab the weapon if one is equipped
        this.isLatchable[hand.ordinal()] =
            (hand == InteractionHand.OFF_HAND || ClientNetworking.supportsReversedBow()) &&
                isAtGripPosition(otherWeapon, hand);
        // main hand can shoot when the offhand grabs the weapon
        return this.isLatchable[hand.ordinal()] || this.isLatched[otherHand.ordinal()];
    }

    @Override
    public void reset(@Nullable LocalPlayer player, InteractionHand hand) {
        this.isLatchable[hand.ordinal()] = false;
        this.isLatched[hand.ordinal()] = false;
    }

    @Override
    public boolean onPress(LocalPlayer player, InteractionHand hand) {
        if (this.isLatchable[hand.ordinal()]) {
            this.isLatched[hand.ordinal()] = true;
            return true;
        } else if (this.isLatched[1 - hand.ordinal()]) {
            this.dh.vr.triggerHapticPulse(hand.ordinal(), 500);
            this.dh.vr.triggerHapticPulse(1 - hand.ordinal(), 3000);
            ClientNetworking.sendServerPacket(new DrawPayloadC2S(1F));
            ClientNetworking.sendActiveBodyPart(VRBodyPart.fromInteractionHand(hand), true);

            Minecraft.getInstance().gameMode.useItem(player, hand);

            // reset to 0, in case user switches modes.
            ClientNetworking.sendServerPacket(new DrawPayloadC2S(0.0F));
            ClientNetworking.resetActiveBodyPart();
            return false;
        }
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

    public boolean isLatched() {
        return this.isLatched[0] || this.isLatched[1];
    }

    public boolean isLatched(InteractionHand hand) {
        return this.isLatched[hand.ordinal()];
    }

    public int getLatchedHand() {
        for (int i = 0; i < 2; i++) {
            if (this.isLatched[i]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void renderDebug(boolean isActive) {
        for (int hand = 0; hand < 2; hand++) {
            DualHandedWeapon weapon = getActiveWeapon(Minecraft.getInstance().player, InteractionHand.values()[hand]);
            if (weapon != null) {
                VRData world = this.dh.vrPlayer.getVRDataWorld();
                // no origin offset, since the camera is world relative
                DebugRenderHelper.renderSphere(
                    MathUtils.subtractToVector3f(world.getController(hand).getPosition()
                            .add(new Vec3(world.getController(hand).getCustomVector(weapon.getGripPosition()))),
                        world.getEye(this.dh.currentPass).getPosition()),
                    0.05F * world.worldScale, this.isLatchable[1 - hand] ? MathUtils.GREEN : MathUtils.RED);
            }
        }
    }
}
