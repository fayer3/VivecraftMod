package org.vivecraft.client_vr.gameplay.screenhandlers;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.vivecraft.common.utils.MathUtils;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.gui.GuiKeyboard;
import org.vivecraft.client_vr.gui.PhysicalKeyboard;
import org.vivecraft.client_vr.provider.ControllerType;

public class KeyboardHandler {
    public static Minecraft mc = Minecraft.getInstance();
    public static ClientDataHolderVR dh = ClientDataHolderVR.getInstance();
    public static boolean Showing = false;
    public static GuiKeyboard UI = new GuiKeyboard();
    public static PhysicalKeyboard physicalKeyboard = new PhysicalKeyboard();
    public static Vector3f Pos_room = new Vector3f();
    public static Matrix4f Rotation_room = new Matrix4f();
    private static boolean PointedL;
    private static boolean PointedR;
    public static boolean keyboardForGui;
    public static RenderTarget Framebuffer = null;
    private static boolean lastPressedClickL;
    private static boolean lastPressedClickR;
    private static boolean lastPressedShift;

    public static boolean setOverlayShowing(boolean showingState) {
        if (ClientDataHolderVR.kiosk) return false;
        if (dh.vrSettings.seated) {
            showingState = false;
        }

        if (showingState) {
            if (dh.vrSettings.physicalKeyboard) {
                physicalKeyboard.show();
            } else {
                UI.init(Minecraft.getInstance(), GuiHandler.scaledWidthMax, GuiHandler.scaledHeightMax);
            }

            Showing = true;
            orientOverlay(mc.screen != null);
            RadialHandler.setOverlayShowing(false, null);

            if (dh.vrSettings.physicalKeyboard && mc.screen != null) {
                GuiHandler.onScreenChanged(mc.screen, mc.screen, false);
            }
        } else {
            Showing = false;
            if (dh.vrSettings.physicalKeyboard) {
                physicalKeyboard.unpressAllKeys();
            }
        }

        return Showing;
    }

    public static void processGui() {
        PointedL = false;
        PointedR = false;

        if (!Showing) return;
        if (dh.vrSettings.seated) return;
        if (Rotation_room == null) return;

        if (dh.vrSettings.physicalKeyboard) {
            physicalKeyboard.process();
            // Skip the rest of this
            return;
        }

        // process cursors
        PointedR = UI.processCursor(Pos_room, Rotation_room, false);
        PointedL = UI.processCursor(Pos_room, Rotation_room, true);
    }

    public static void orientOverlay(boolean guiRelative) {
        keyboardForGui = false;

        if (!Showing) return;

        keyboardForGui = guiRelative;

        if (dh.vrSettings.physicalKeyboard) {
            Vector3f pos = dh.vrPlayer.vrdata_room_pre.hmd.getPositionF();
            Vector3f offset = new Vector3f(0.0F, -0.5F, 0.3F);
            offset.rotateY(-dh.vrPlayer.vrdata_room_pre.hmd.getYawRad());

            Pos_room = pos.add(offset);
            float yaw = Mth.PI - dh.vrPlayer.vrdata_room_pre.hmd.getYawRad();

            Rotation_room.rotationY(yaw);
            Rotation_room.rotateX(Mth.PI * 0.8f);
        } else if (guiRelative && GuiHandler.guiRotation_room != null) {
            // put the keyboard below the current screen
            Matrix4fc guiRot = GuiHandler.guiRotation_room;
            Vector3f guiUp = guiRot.transformDirection(MathUtils.UP, new Vector3f())
                .mul(0.8F);
            Vector3f guiFwd = guiRot.transformDirection(MathUtils.FORWARD, new Vector3f())
                .mul(0.25F * GuiHandler.guiScale);

            Matrix4f roomRotation = new Matrix4f();
            roomRotation.translate(
                GuiHandler.guiPos_room.x - guiUp.x + guiFwd.x,
                GuiHandler.guiPos_room.y - guiUp.y + guiFwd.y,
                GuiHandler.guiPos_room.z - guiUp.z + guiFwd.z);

            roomRotation.mul(guiRot);
            roomRotation.rotateX(Mth.DEG_TO_RAD * -30.0F);

            Rotation_room = roomRotation;
            Pos_room = Rotation_room.getTranslation(Pos_room);
            Rotation_room.setTranslation(0F, 0F, 0F);
        } else {
            // copy from GuiHandler.onScreenChanged for static screens
            Vector3f offset = new Vector3f(0.0F, 0.0F, -2.0F);

            Vector3f hmdPos = dh.vrPlayer.vrdata_room_pre.hmd.getPositionF();
            Vector3f look = dh.vrPlayer.vrdata_room_pre.hmd.getCustomVector(offset).mul(0.5F);

            Pos_room = look.add(hmdPos, new Vector3f());

            // orient screen
            float yaw = Mth.PI + (float) Math.atan2(look.x, look.z);
            Rotation_room = new Matrix4f().rotationY(yaw);
        }
    }

    public static void processBindings() {
        if (!Showing) return;

        if (dh.vrSettings.physicalKeyboard) {
            physicalKeyboard.processBindings();
            return;
        }

        // scale virtual cursor coords to actual screen coords
        float uiScaleX = (float) UI.width / (float) GuiHandler.guiWidth;
        float uiScaleY = (float) UI.height / (float) GuiHandler.guiHeight;

        int x1 = (int) (Math.min(Math.max((int) UI.cursorX1, 0), GuiHandler.guiWidth) * uiScaleX);
        int y1 = (int) (Math.min(Math.max((int) UI.cursorY1, 0), GuiHandler.guiHeight) * uiScaleY);
        int x2 = (int) (Math.min(Math.max((int) UI.cursorX2, 0), GuiHandler.guiWidth) * uiScaleX);
        int y2 = (int) (Math.min(Math.max((int) UI.cursorY2, 0), GuiHandler.guiHeight) * uiScaleY);

        if (PointedL && GuiHandler.keyKeyboardClick.consumeClick(ControllerType.LEFT)) {
            UI.mouseClicked(x1, y1, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            lastPressedClickL = true;
        }

        if (!GuiHandler.keyKeyboardClick.isDown(ControllerType.LEFT) && lastPressedClickL) {
            UI.mouseReleased(x1, y1, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            lastPressedClickL = false;
        }

        if (PointedR && GuiHandler.keyKeyboardClick.consumeClick(ControllerType.RIGHT)) {
            UI.mouseClicked(x2, y2, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            lastPressedClickR = true;
        }

        if (!GuiHandler.keyKeyboardClick.isDown(ControllerType.RIGHT) && lastPressedClickR) {
            UI.mouseReleased(x2, y2, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            lastPressedClickR = false;
        }

        if (GuiHandler.keyKeyboardShift.consumeClick()) {
            UI.setShift(true);
            lastPressedShift = true;
        }

        if (!GuiHandler.keyKeyboardShift.isDown() && lastPressedShift) {
            UI.setShift(false);
            lastPressedShift = false;
        }
    }

    /**
     * checks if the given controller points at the keyboard
     * @param type controller to check
     */
    public static boolean isUsingController(ControllerType type) {
        return type == ControllerType.LEFT ? PointedL : PointedR;
    }
}
