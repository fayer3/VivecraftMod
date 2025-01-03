package org.vivecraft.mixin.client_vr.gui.screens.inventory;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenVRMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

    @Shadow
    private EditBox searchBox;

    @Shadow
    private static int selectedTab;

    public CreativeModeInventoryScreenVRMixin(
        CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
    }

    @Inject(method = "refreshSearchResults", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen;scrollOffs:F"))
    private void vivecraft$addVivecraftItemsSearch(CallbackInfo ci) {
        // only add to actual search
        if (selectedTab == CreativeModeTab.TAB_SEARCH.getId()) {
            vivecraft$addCreativeSearch(this.searchBox.getValue(), this.menu.items);
        }
    }

    @Inject(method = "selectTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTab;fillItemList(Lnet/minecraft/core/NonNullList;)V", shift = At.Shift.AFTER))
    private void vivecraft$addVivecraftItemsCategory(CreativeModeTab tab, CallbackInfo ci) {
        vivecraft$addCreativeItems(tab, this.menu.items);
    }

    @Unique
    private void vivecraft$addCreativeItems(CreativeModeTab tab, NonNullList<ItemStack> items) {
        if (tab == CreativeModeTab.TAB_FOOD || tab == null) {
            ItemStack eatMeCake = new ItemStack(Items.PUMPKIN_PIE);
            eatMeCake.setHoverName(Component.literal("EAT ME"));

            ItemStack drinkMePotion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
            drinkMePotion.setHoverName(Component.literal("DRINK ME"));
            drinkMePotion.getOrCreateTag().putInt("HideFlags", 32);

            items.add(eatMeCake);
            items.add(drinkMePotion);
        }

        if (tab == CreativeModeTab.TAB_TOOLS || tab == null) {
            ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
            boots.setHoverName(Component.translatable("vivecraft.item.jumpboots"));
            boots.getOrCreateTag().putBoolean("Unbreakable", true);
            boots.getOrCreateTag().putInt("HideFlags", 4);
            boots.getOrCreateTagElement(ItemStack.TAG_DISPLAY).putInt(ItemStack.TAG_COLOR, 0x8CE56F);

            ItemStack claws = new ItemStack(Items.SHEARS);
            claws.setHoverName(Component.translatable("vivecraft.item.climbclaws"));
            claws.getOrCreateTag().putBoolean("Unbreakable", true);
            claws.getOrCreateTag().putInt("HideFlags", 4);

            items.add(boots);
            items.add(claws);
        }
    }

    @Unique
    private void vivecraft$addCreativeSearch(String query, NonNullList<ItemStack> itmes) {
        NonNullList<ItemStack> vivecraftItems = NonNullList.create();
        vivecraft$addCreativeItems(null, vivecraftItems);

        for (ItemStack item : vivecraftItems) {
            if (query.isEmpty() || item.getHoverName().toString().toLowerCase().contains(query.toLowerCase())) {
                itmes.add(item);
            }
        }
    }
}
