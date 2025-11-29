package org.vivecraft.api.client;

import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;

/**
 * Defines a weapon that needs to be hold with both hands, and is shot with a trigger pull.
 * Aim is calculated in the direction from the trigger hand to the grip hand
 */
public interface DualHandedWeapon {

    /**
     * checks if the give ItemStack corresponds to this weapon
     * @param itemStack ItemStack to check
     * @return true if the ItemStack is applicable for this weapon
     */
    boolean isWeapon(ItemStack itemStack);

    /**
     * gets the position for the hand supporting the weapon
     * @return the position the grip hand is supposed to be at
     */
    Vector3fc getGripPosition();

    /**
     * gets the position for the hand that pulls the trigger of the weapon
     * @return the position the trigger hand is supposed to be at
     */
    Vector3fc getTriggerPosition();
}
