package org.vivecraft.client_vr.gameplay.weapons;

import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.vivecraft.api.client.DualHandedWeapon;

public class CrossbowWeapon implements DualHandedWeapon {

    public static final Vector3fc GRIP_POSITION = new Vector3f(0.0f, 0.0f, -0.25f);
    public static final Vector3fc TRIGGER_POSITION = new Vector3f(0.0f, 0.0f, 0.0f);
    public static final Vector3fc RELOAD_POSITION = new Vector3f(0.0f, 0.1f, -0.08f);

    @Override
    public boolean isWeapon(ItemStack itemStack) {
        return itemStack.is(Items.CROSSBOW) && CrossbowItem.isCharged(itemStack);
    }

    @Override
    public Vector3fc getGripPosition() {
        return GRIP_POSITION;
    }

    @Override
    public Vector3fc getTriggerPosition() {
        return TRIGGER_POSITION;
    }
}
