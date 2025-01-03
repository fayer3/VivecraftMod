package org.vivecraft.client.gui.settings;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.vivecraft.client.gui.widgets.SettingsList;
import org.vivecraft.client_vr.ClientDataHolderVR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class GuiBlacklistEditor extends GuiListScreen {

    private List<String> elements;

    public GuiBlacklistEditor(Screen lastScreen) {
        super(Component.translatable("vivecraft.options.screen.blocklist"), lastScreen);
    }

    @Override
    protected void init() {
        clearWidgets();
        double scrollAmount = this.list != null ? this.list.getScrollAmount() : 0.0D;

        this.list = new SettingsList(this, this.minecraft, getEntries());
        this.list.setScrollAmount(scrollAmount);
        this.addWidget(this.list);

        this.addRenderableWidget(
            new Button(this.width / 2 - 155, this.height - 27, 150, 20,
                Component.translatable("vivecraft.gui.loaddefaults"), button -> {
                ClientDataHolderVR.getInstance().vrSettings.vrServerBlacklist =
                    ClientDataHolderVR.getInstance().vrSettings.getServerBlacklistDefault();
                ClientDataHolderVR.getInstance().vrSettings.saveOptions();
                this.elements = null;
                this.reinit = true;
            }));

        this.addRenderableWidget(
            new Button(this.width / 2 + 5, this.height - 27, 150, 20,
                Component.translatable("gui.back"), button -> this.onClose()));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        GuiEventListener focused = this.getFocused();
        if (focused != null) {
            focused.changeFocus(false);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        ClientDataHolderVR.getInstance().vrSettings.vrServerBlacklist = this.elements.toArray(new String[0]);
        ClientDataHolderVR.getInstance().vrSettings.saveOptions();
        super.onClose();
    }

    private List<String> getCurrentValues() {
        return this.list.children().stream().map(entry -> {
            if (entry instanceof ListValueEntry listValueEntry) {
                return listValueEntry.getString();
            } else {
                return "";
            }
        }).filter(string -> !string.isEmpty()).collect(Collectors.toList());
    }

    @Override
    protected List<SettingsList.BaseEntry> getEntries() {
        List<SettingsList.BaseEntry> entries = new LinkedList<>();
        if (this.elements == null) {
            this.elements = new ArrayList<>(
                Arrays.asList(ClientDataHolderVR.getInstance().vrSettings.vrServerBlacklist));
        }
        int i = 0;
        for (String item : this.elements) {
            EditBox box = new EditBox(Minecraft.getInstance().font, 0, 0, ListValueEntry.valueButtonWidth - 1, 20,
                Component.literal(item));
            box.setMaxLength(1000);
            box.setValue(item);
            int index = i++;
            box.setResponder(s -> this.elements.set(index, s));
            entries.add(new ListValueEntry(Component.empty(), box, button -> {
                this.elements.remove(index);
                this.reinit = true;
            }));
        }
        entries.add(new SettingsList.WidgetEntry(Component.translatable("vivecraft.options.addnew"),
            new Button(0, 0, 20, 20,
                Component.literal("+"), button -> {
                this.elements = getCurrentValues();
                this.elements.add("");
                this.reinit = true;
            })));
        return entries;
    }

    private static class ListValueEntry extends SettingsList.WidgetEntry {
        public static final int valueButtonWidth = 280;

        private final Button deleteButton;

        public ListValueEntry(Component name, EditBox valueWidget, Button.OnPress deleteAction) {
            super(name, valueWidget);

            this.deleteButton = new Button(0, 0, 20, 20,
                Component.literal("-"), deleteAction,
                (button, poseStack, x, y) ->
                    Minecraft.getInstance().screen.renderTooltip(poseStack,
                        Component.translatable("selectWorld.delete"), x, y));
        }

        @Override
        public void render(
            PoseStack poseStack, int index, int top, int left, int width, int height, int mouseX, int mouseY,
            boolean hovering, float partialTick)
        {
            this.valueWidget.x = left + -50;
            this.valueWidget.y = top;
            this.valueWidget.render(poseStack, mouseX, mouseY, partialTick);
            this.deleteButton.x = left + 230;
            this.deleteButton.y = top;
            this.deleteButton.render(poseStack, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return ImmutableList.of(this.valueWidget, this.deleteButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of(this.valueWidget, this.deleteButton);
        }

        public String getString() {
            return ((EditBox) this.valueWidget).getValue();
        }
    }
}
