package org.vivecraft.client_vr.gameplay.screenhandlers;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.phys.*;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.vivecraft.common.utils.MathUtils;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.MethodHolder;
import org.vivecraft.client_vr.VRData;
import org.vivecraft.client_vr.VRState;
import org.vivecraft.client_vr.extensions.WindowExtension;
import org.vivecraft.client_vr.gameplay.VRPlayer;
import org.vivecraft.client_vr.provider.ControllerType;
import org.vivecraft.client_vr.provider.HandedKeyBinding;
import org.vivecraft.client_vr.provider.InputSimulator;
import org.vivecraft.client_vr.provider.MCVR;
import org.vivecraft.client_vr.render.RenderPass;
import org.vivecraft.client_vr.render.helpers.RenderHelper;
import org.vivecraft.client_vr.settings.VRSettings;

public class GuiHandler {
    public static Minecraft mc = Minecraft.getInstance();
    public static ClientDataHolderVR dh = ClientDataHolderVR.getInstance();
    static boolean lastPressedLeftClick;
    static boolean lastPressedRightClick;
    static boolean lastPressedMiddleClick;
    static boolean lastPressedShift;
    static boolean lastPressedCtrl;
    static boolean lastPressedAlt;

    // For mouse menu emulation
    private static double controllerMouseX = -1.0D;
    private static double controllerMouseY = -1.0D;
    public static boolean controllerMouseValid;
    public static int controllerMouseTicks;
    public static boolean guiAppearOverBlockActive = false;
    public static float guiScale = 1.0F;
    public static float guiScaleApplied = 1.0F;
    public static Vector3f guiPos_room = null;
    public static Matrix4f guiRotation_room = null;
    public static final KeyMapping keyLeftClick = new KeyMapping("vivecraft.key.guiLeftClick", -1, "vivecraft.key.category.gui");
    public static final KeyMapping keyRightClick = new KeyMapping("vivecraft.key.guiRightClick", -1, "vivecraft.key.category.gui");
    public static final KeyMapping keyMiddleClick = new KeyMapping("vivecraft.key.guiMiddleClick", -1, "vivecraft.key.category.gui");
    public static final KeyMapping keyShift = new KeyMapping("vivecraft.key.guiShift", -1, "vivecraft.key.category.gui");
    public static final KeyMapping keyCtrl = new KeyMapping("vivecraft.key.guiCtrl", -1, "vivecraft.key.category.gui");
    public static final KeyMapping keyAlt = new KeyMapping("vivecraft.key.guiAlt", -1, "vivecraft.key.category.gui");
    public static final KeyMapping keyScrollUp = new KeyMapping("vivecraft.key.guiScrollUp", -1, "vivecraft.key.category.gui");
    public static final KeyMapping keyScrollDown = new KeyMapping("vivecraft.key.guiScrollDown", -1, "vivecraft.key.category.gui");
    public static final KeyMapping keyScrollAxis = new KeyMapping("vivecraft.key.guiScrollAxis", -1, "vivecraft.key.category.gui");
    public static final HandedKeyBinding keyKeyboardClick = new HandedKeyBinding("vivecraft.key.keyboardClick", -1, "vivecraft.key.category.keyboard") {
        @Override
        public boolean isPriorityOnController(ControllerType type) {
            if (KeyboardHandler.Showing && !GuiHandler.dh.vrSettings.physicalKeyboard) {
                return KeyboardHandler.isUsingController(type);
            } else {
                return RadialHandler.isShowing() && RadialHandler.isUsingController(type);
            }
        }
    };
    public static final HandedKeyBinding keyKeyboardShift = new HandedKeyBinding("vivecraft.key.keyboardShift", -1, "vivecraft.key.category.keyboard") {
        @Override
        public boolean isPriorityOnController(ControllerType type) {
            if (KeyboardHandler.Showing) {
                return GuiHandler.dh.vrSettings.physicalKeyboard || KeyboardHandler.isUsingController(type);
            } else {
                return RadialHandler.isShowing() && RadialHandler.isUsingController(type);
            }
        }
    };
    public static RenderTarget guiFramebuffer = null;

    // for GUI scale override
    public static int guiWidth = 1280;
    public static int guiHeight = 720;
    public static int guiScaleFactorMax;
    public static int guiScaleFactor = calculateScale(0, false, guiWidth, guiHeight);
    public static int scaledWidth;
    public static int scaledHeight;
    public static int scaledWidthMax;
    public static int scaledHeightMax;
    private static int prevGuiScale = -1;

    /**
     * copy of the vanilla method to calculate gui resolution and max scale
     */
    public static int calculateScale(int scaleIn, boolean forceUnicode, int framebufferWidth, int framebufferHeight) {
        int scale = 1;
        int maxScale = 1;

        while (maxScale < framebufferWidth &&
            maxScale < framebufferHeight &&
            framebufferWidth / (maxScale + 1) >= 320 &&
            framebufferHeight / (maxScale + 1) >= 240) {
            if (scale < scaleIn || scaleIn == 0) {
                scale++;
            }
            maxScale++;
        }

        if (forceUnicode) {
            if (scale % 2 != 0) {
                scale++;
            }
            if (maxScale % 2 != 0) {
                maxScale++;
            }
        }

        guiScaleFactorMax = maxScale;

        scaledWidth = Mth.ceil(framebufferWidth / (float) scale);
        scaledWidthMax = Mth.ceil(framebufferWidth / (float) maxScale);

        scaledHeight = Mth.ceil(framebufferHeight / (float) scale);
        scaledHeightMax = Mth.ceil(framebufferHeight / (float) maxScale);

        return scale;
    }

    /**
     * updates the gui resolution, and scales the cursor position
     * @return if the gui scale/size changed
     */
    public static boolean updateResolution() {
        int oldWidth = guiWidth;
        int oldHeight = guiHeight;
        int oldGuiScale = guiScaleFactor;
        guiWidth = dh.vrSettings.doubleGUIResolution ? 2560 : 1280;
        guiHeight = dh.vrSettings.doubleGUIResolution ? 1440 : 720;

        int newGuiScale = dh.vrSettings.doubleGUIResolution ?
            dh.vrSettings.guiScale : (int) Math.ceil(dh.vrSettings.guiScale * 0.5f);

        if (oldWidth != guiWidth || prevGuiScale != newGuiScale) {
            // only recalculate when scale or size changed
            guiScaleFactor = calculateScale(newGuiScale, false, guiWidth, guiHeight);
            prevGuiScale = newGuiScale;
        }
        if (oldWidth != guiWidth) {
            // move cursor to right position
            InputSimulator.setMousePos(
                mc.mouseHandler.xpos() * ((WindowExtension) (Object) mc.getWindow()).vivecraft$getActualScreenWidth() / oldWidth,
                mc.mouseHandler.ypos() * ((WindowExtension) (Object) mc.getWindow()).vivecraft$getActualScreenHeight() / oldHeight);
            controllerMouseX *= (double) guiWidth / oldWidth;
            controllerMouseY *= (double) guiHeight / oldHeight;
            return true;
        } else {
            return oldGuiScale != guiScaleFactor;
        }
    }

    /**
     * calculates and sets the cursor position
     */
    public static void processGui() {
        if (guiRotation_room == null) return;
        if (dh.vrSettings.seated) return;
        if (!MCVR.get().isControllerTracking(0)) return;
        // some mods ungrab the mouse when there is no screen
        if (mc.screen == null && mc.mouseHandler.isMouseGrabbed()) return;

        Vector2f tex = getTexCoordsForCursor(guiPos_room, guiRotation_room, guiScale, dh.vrPlayer.vrdata_room_pre.getController(0));
        float u = tex.x;
        float v = tex.y;

        if (u < 0 || v < 0 || u > 1 || v > 1) {
            // offscreen
            controllerMouseX = -1.0f;
            controllerMouseY = -1.0f;
            controllerMouseValid = false;
        } else if (!controllerMouseValid) {
            controllerMouseX = (int) (u * mc.getWindow().getWidth());
            controllerMouseY = (int) (v * mc.getWindow().getHeight());
            controllerMouseValid = true;
        } else {
            // apply some smoothing between mouse positions
            float newX = (int) (u * mc.getWindow().getWidth());
            float newY = (int) (v * mc.getWindow().getHeight());
            controllerMouseX = controllerMouseX * 0.7f + newX * 0.3f;
            controllerMouseY = controllerMouseY * 0.7f + newY * 0.3f;
            controllerMouseValid = true;
        }

        if (controllerMouseValid) {
            // mouse on screen
            InputSimulator.setMousePos(
                controllerMouseX * (((WindowExtension) (Object) mc.getWindow()).vivecraft$getActualScreenWidth() / (double) mc.getWindow().getScreenWidth()),
                controllerMouseY * (((WindowExtension) (Object) mc.getWindow()).vivecraft$getActualScreenHeight() / (double) mc.getWindow().getScreenHeight()));
        }
    }

    /**
     * calculates the relative cursor position on the gui
     * @param guiPos_room position of the gui
     * @param guiRotation_room orientation of the gui
     * @param guiScale size of the gui layer
     * @param controller device pose to get the cursor for, should be a room based one
     * @return relative position on the gui, anchored top left.<br>
     *  If offscreen returns Vec2(-1,-1)
     */
    public static Vector2f getTexCoordsForCursor(Vector3f guiPos_room, Matrix4f guiRotation_room, float guiScale, VRData.VRDevicePose controller) {
        Vector3f controllerPos = controller.getPositionF();
        Vector3f controllerDir = controller.getDirection();

        Vector3f guiNormal = guiRotation_room.transformDirection(MathUtils.FORWARD, new Vector3f());
        Vector3f guiRight = guiRotation_room.transformDirection(MathUtils.LEFT, new Vector3f());
        Vector3f guiUp = guiRotation_room.transformDirection(MathUtils.UP, new Vector3f());

        float guiDotController = guiNormal.dot(controllerDir);

        if (Math.abs(guiDotController) > 1.0E-5F) {
            // pointed normal to the GUI
            float guiHalfWidth = 0.5F;
            float guiHalfHeight = 0.5F;
            Vector3f guiPos = new Vector3f(guiPos_room);

            Vector3f guiTopLeft = guiPos
                .sub(guiUp.mul(guiHalfHeight, new Vector3f()))
                .sub(guiRight.mul(guiHalfWidth, new Vector3f()));

            float intersectDist = -guiNormal.dot(controllerPos.sub(guiTopLeft, new Vector3f())) / guiDotController;

            if (intersectDist > 0.0F) {
                Vector3f pointOnPlane = controllerPos.add(controllerDir.mul(intersectDist), new Vector3f());

                pointOnPlane.sub(guiTopLeft);

                float u = pointOnPlane.dot(guiRight);
                float v = pointOnPlane.dot(guiUp);

                float aspect = (float) mc.getWindow().getGuiScaledHeight() / (float) mc.getWindow().getGuiScaledWidth();
                u = (u - 0.5F) / 1.5F / guiScale + 0.5F;
                v = (v - 0.5F) / aspect / 1.5F / guiScale + 0.5F;
                v = 1.0F - v;
                return new Vector2f(u, v);
            }
        }

        return new Vector2f(-1.0F, -1.0F);
    }

    /**
     * processes key presses for the GUI
     */
    public static void processBindingsGui() {
        // only click mouse keys, when cursor is on screen
        boolean mouseValid = controllerMouseX >= 0.0D && controllerMouseX < mc.getWindow().getScreenWidth() &&
            controllerMouseY >= 0.0D && controllerMouseY < mc.getWindow().getScreenWidth();

        // LMB
        if (keyLeftClick.consumeClick() && mc.screen != null && mouseValid) {
            InputSimulator.pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
            lastPressedLeftClick = true;
        }
        if (!keyLeftClick.isDown() && lastPressedLeftClick) {
            InputSimulator.releaseMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
            lastPressedLeftClick = false;
        }

        // RMB
        if (keyRightClick.consumeClick() && mc.screen != null && mouseValid) {
            InputSimulator.pressMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            lastPressedRightClick = true;
        }
        if (!keyRightClick.isDown() && lastPressedRightClick) {
            InputSimulator.releaseMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            lastPressedRightClick = false;
        }

        // MMB
        if (keyMiddleClick.consumeClick() && mc.screen != null && mouseValid) {
            InputSimulator.pressMouse(GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
            lastPressedMiddleClick = true;
        }
        if (!keyMiddleClick.isDown() && lastPressedMiddleClick) {
            InputSimulator.releaseMouse(GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
            lastPressedMiddleClick = false;
        }

        // Shift
        if (keyShift.consumeClick() && mc.screen != null) {
            InputSimulator.pressKey(GLFW.GLFW_KEY_LEFT_SHIFT);
            lastPressedShift = true;
        }
        if (!keyShift.isDown() && lastPressedShift) {
            InputSimulator.releaseKey(GLFW.GLFW_KEY_LEFT_SHIFT);
            lastPressedShift = false;
        }

        // Crtl
        if (keyCtrl.consumeClick() && mc.screen != null) {
            InputSimulator.pressKey(GLFW.GLFW_KEY_LEFT_CONTROL);
            lastPressedCtrl = true;
        }
        if (!keyCtrl.isDown() && lastPressedCtrl) {
            InputSimulator.releaseKey(GLFW.GLFW_KEY_LEFT_CONTROL);
            lastPressedCtrl = false;
        }

        // Alt
        if (keyAlt.consumeClick() && mc.screen != null) {
            InputSimulator.pressKey(GLFW.GLFW_KEY_LEFT_ALT);
            lastPressedAlt = true;
        }
        if (!keyAlt.isDown() && lastPressedAlt) {
            InputSimulator.releaseKey(GLFW.GLFW_KEY_LEFT_ALT);
            lastPressedAlt = false;
        }

        // scroll mouse
        if (keyScrollUp.consumeClick() && mc.screen != null) {
            InputSimulator.scrollMouse(0.0D, 4.0D);
        }

        if (keyScrollDown.consumeClick() && mc.screen != null) {
            InputSimulator.scrollMouse(0.0D, -4.0D);
        }
    }

    public static void onScreenChanged(Screen previousGuiScreen, Screen newScreen, boolean unpressKeys) {
        onScreenChanged(previousGuiScreen, newScreen, unpressKeys, false);
    }

    public static void onScreenChanged(Screen previousGuiScreen, Screen newScreen, boolean unpressKeys, boolean infrontOfHand) {
        if (!VRState.vrRunning) {
            return;
        }

        if (unpressKeys) {
            dh.vr.ignorePressesNextFrame = true;
        }

        if (newScreen == null) {
            // just insurance
            guiPos_room = null;
            guiRotation_room = null;
            guiScale = 1.0F;

            if (KeyboardHandler.keyboardForGui && dh.vrSettings.autoCloseKeyboard) {
                KeyboardHandler.setOverlayShowing(false);
            }
        } else {
            RadialHandler.setOverlayShowing(false, null);
        }

        if (mc.level == null || newScreen instanceof WinScreen) {
            dh.vrSettings.worldRotationCached = dh.vrSettings.worldRotation;
            dh.vrSettings.worldRotation = 0.0F;
        } else {
            // these dont update when screen open.
            if (dh.vrSettings.worldRotationCached != 0.0F) {
                dh.vrSettings.worldRotation = dh.vrSettings.worldRotationCached;
                dh.vrSettings.worldRotationCached = 0.0F;
            }
        }

        // check if the new screen is meant to show the MenuRoom, instead of the current screen
        boolean staticScreen = MethodHolder.willBeInMenuRoom(newScreen);
        staticScreen &= !dh.vrSettings.seated && !dh.vrSettings.menuAlwaysFollowFace;

        if (staticScreen) {
            guiScale = 2.0F;
            Vector2fc playArea = MCVR.get().getPlayAreaSize();
            // slight offset to center of the room, to prevent z fighting
            guiPos_room = new Vector3f(0.02F, 1.3F, -Math.max(playArea != null ? playArea.y() * 0.5F : 0.0F, 1.5F));
            guiRotation_room = new Matrix4f();
            return;
        }
        if ((previousGuiScreen == null && newScreen != null) ||
            newScreen instanceof ChatScreen ||
            newScreen instanceof BookEditScreen ||
            newScreen instanceof AbstractSignEditScreen)
        {
            // check if screen is a container screen
            // and if the pointed at block is the same that was last interacted with
            boolean isBlockScreen = newScreen instanceof AbstractContainerScreen &&
                mc.hitResult != null &&
                mc.hitResult.getType() == HitResult.Type.BLOCK;

            // check if screen is a container screen
            // and if the pointed at entity is the same that was last interacted with
            boolean isEntityScreen = newScreen instanceof AbstractContainerScreen &&
                mc.hitResult instanceof EntityHitResult &&
                ((EntityHitResult) mc.hitResult).getEntity() instanceof ContainerEntity;

            VRData.VRDevicePose facingDevice = infrontOfHand ? dh.vrPlayer.vrdata_room_pre.getController(0) : dh.vrPlayer.vrdata_room_pre.hmd;

            if (guiAppearOverBlockActive && (isBlockScreen || isEntityScreen) && dh.vrSettings.guiAppearOverBlock) {
                // appear over block / entity
                Vec3 sourcePos;
                if (isEntityScreen) {
                    EntityHitResult entityHitResult = (EntityHitResult) mc.hitResult;
                    sourcePos = entityHitResult.getEntity().position();
                } else {
                    BlockHitResult blockHitResult = (BlockHitResult) mc.hitResult;
                    sourcePos = new Vec3(
                        blockHitResult.getBlockPos().getX() + 0.5D,
                        blockHitResult.getBlockPos().getY(),
                        blockHitResult.getBlockPos().getZ() + 0.5D);
                }

                Vector3f roomPos = VRPlayer.worldToRoomPos(sourcePos, dh.vrPlayer.vrdata_world_pre);
                Vector3f hmdPos = dh.vrPlayer.vrdata_room_pre.hmd.getPositionF();
                float distance = roomPos.sub(hmdPos).length();
                guiScale = (float) Math.sqrt(distance);

                Vec3 sourcePosWorld = new Vec3(sourcePos.x, sourcePos.y + 1.1F + guiScale * 0.25F, sourcePos.z);
                guiPos_room = VRPlayer.worldToRoomPos(sourcePosWorld, dh.vrPlayer.vrdata_world_pre);
            } else {
                // static screens like menu, inventory, and dead.
                Vector3f offset = new Vector3f(0.0F, 0.0F, -2.0F);

                if (newScreen instanceof ChatScreen) {
                    offset.set(0.0F, 0.5F, -2.0F);
                } else if (newScreen instanceof BookEditScreen || newScreen instanceof AbstractSignEditScreen) {
                    offset.set(0.0F, 0.25F, -2.0F);
                }

                Vector3f hmdPos = facingDevice.getPositionF();
                Vector3f look = facingDevice.getCustomVector(offset);
                guiPos_room = new Vector3f(
                    look.x * 0.5F + hmdPos.x,
                    look.y * 0.5F + hmdPos.y,
                    look.z * 0.5F + hmdPos.z);

                if (dh.vrSettings.physicalKeyboard && KeyboardHandler.Showing && guiPos_room.y < hmdPos.y + 0.2F) {
                    guiPos_room.set(guiPos_room.x, hmdPos.y + 0.2F, guiPos_room.z);
                }
            }

            // orient screen
            Vector3f hmdPos = facingDevice.getPositionF();
            Vector3f look = guiPos_room.sub(hmdPos, new Vector3f());
            float pitch = (float) Math.asin(look.y / look.length());
            float yaw = Mth.PI + (float) Math.atan2(look.x, look.z);
            guiRotation_room = new Matrix4f().rotationY(yaw);
            guiRotation_room.rotateX(pitch);
        }

        KeyboardHandler.orientOverlay(newScreen != null);
    }

    /**
     * sets upt he {@code poseStack} to render the gui, and returns the world position of the gui
     * @param currentPass renderpass to position the gui for
     * @param poseStack PoseStack to alter
     * @return
     */
    public static Vec3 applyGUIModelView(RenderPass currentPass, PoseStack poseStack) {
        mc.getProfiler().push("applyGUIModelView");

        if (mc.screen != null && guiPos_room == null) {
            //naughty mods!
            onScreenChanged(null, mc.screen, false);
        } else if (mc.screen == null && !mc.mouseHandler.isMouseGrabbed()) {
            // some mod want's to do a mouse selection overlay
            if (guiPos_room == null) {
                onScreenChanged(null, new Screen(Component.empty()) {
                }, false, true);
            }
        } else if (mc.screen == null && guiPos_room != null) {
            //even naughtier mods!
            // someone canceled the setScreen, so guiPos didn't get reset
            onScreenChanged(null, null, false);
        }

        Vec3 guipos = null;
        Matrix4f guirot = guiRotation_room;
        Vector3f guilocal = new Vector3f();
        float scale = guiScale;

        if (guiPos_room == null) {
            guirot = null;
            scale = 1.0F;

            if (mc.level != null && (mc.screen == null || !dh.vrSettings.floatInventory)) {
                // HUD view - attach to head or controller
                int i = 1;

                if (dh.vrSettings.reverseHands) {
                    i = -1;
                }

                if (dh.vrSettings.seated || dh.vrSettings.vrHudLockMode == VRSettings.HUDLock.HEAD) {
                    guirot = new Matrix4f().rotationY(dh.vrPlayer.vrdata_world_render.rotation_radians);

                    Vec3 position = dh.vrPlayer.vrdata_world_render.hmd.getPosition();
                    Vector3f direction = dh.vrPlayer.vrdata_world_render.hmd.getDirection();

                    if (dh.vrSettings.seated && dh.vrSettings.seatedHudAltMode) {
                        direction = dh.vrPlayer.vrdata_world_render.getController(0).getDirection();
                        guirot = guirot.mul(dh.vr.getAimRotation(0), guirot);
                    } else {
                        guirot = guirot.mul(dh.vr.hmdRotation, guirot);
                    }

                    guipos = new Vec3(
                        position.x + direction.x * dh.vrPlayer.vrdata_world_render.worldScale * dh.vrSettings.hudDistance,
                        position.y + direction.y * dh.vrPlayer.vrdata_world_render.worldScale * dh.vrSettings.hudDistance,
                        position.z + direction.z * dh.vrPlayer.vrdata_world_render.worldScale * dh.vrSettings.hudDistance);

                    scale = dh.vrSettings.hudScale;
                } else if (dh.vrSettings.vrHudLockMode == VRSettings.HUDLock.HAND) {
                    // hud on hand
                    guirot = new Matrix4f().rotationY(dh.vrPlayer.vrdata_world_render.rotation_radians);
                    guirot.mul(dh.vr.getAimRotation(1));

                    guirot.rotateX(Mth.PI * -0.2F);
                    guirot.rotateY(Mth.PI * 0.1F * i);
                    scale = 0.58823526F;

                    guilocal = new Vector3f(guilocal.x, 0.32F * dh.vrPlayer.vrdata_world_render.worldScale, guilocal.z);

                    guipos = RenderHelper.getControllerRenderPos(1);

                    dh.vr.hudPopup = true;
                } else if (dh.vrSettings.vrHudLockMode == VRSettings.HUDLock.WRIST) {
                    // hud on wrist
                    guirot = new Matrix4f().rotationY(dh.vrPlayer.vrdata_world_render.rotation_radians);
                    guirot.mul(dh.vr.getAimRotation(1));

                    guirot.rotateZ(Mth.HALF_PI * i);
                    guirot.rotateY(Mth.HALF_PI * i);

                    guipos = RenderHelper.getControllerRenderPos(1);
                    dh.vr.hudPopup = true;

                    boolean slim = mc.player.getSkin().model().id().equals("slim");
                    scale = 0.4F;
                    float offset = mc.player.getMainArm().getOpposite() == (dh.vrSettings.reverseHands ? HumanoidArm.LEFT : HumanoidArm.RIGHT) ? -0.166F : -0.136F;
                    guilocal = new Vector3f(
                        i * offset * dh.vrPlayer.vrdata_world_render.worldScale,
                        (slim ? 0.13F : 0.12F) * dh.vrPlayer.vrdata_world_render.worldScale,
                        0.06F * dh.vrPlayer.vrdata_world_render.worldScale);
                }
            }
        } else {
            // convert previously calculated coords to world coords
            guipos = VRPlayer.roomToWorldPos(guiPos_room, dh.vrPlayer.vrdata_world_render);
            guirot = new Matrix4f().rotationY(dh.vrPlayer.vrdata_world_render.rotation_radians).mul(guirot);
        }

        if ((dh.vrSettings.seated || dh.vrSettings.menuAlwaysFollowFace) && MethodHolder.isInMenuRoom()) {
            // main menu slow yaw tracking thing
            scale = 2.0F;
            Vector3f posAvg = new Vector3f();

            for (Vector3f sample : dh.vr.hmdPosSamples) {
                posAvg.add(sample);
            }

            posAvg.div(dh.vr.hmdPosSamples.size());

            float yawAvg = 0.0F;

            for (float sample : dh.vr.hmdYawSamples) {
                yawAvg += sample;
            }

            yawAvg /= dh.vr.hmdYawSamples.size();
            yawAvg = Mth.DEG_TO_RAD * yawAvg;

            Vector3f dir = new Vector3f((float) -Math.sin(yawAvg), 0.0F, (float) Math.cos(yawAvg));
            float dist = MethodHolder.isInMenuRoom() ?
                2.5F * dh.vrPlayer.vrdata_world_render.worldScale : dh.vrSettings.hudDistance;

            posAvg.add(dir.x * dist, dir.y * dist, dir.z * dist);

            Matrix4f guiRotation = new Matrix4f().rotationY(Mth.PI - yawAvg);
            guirot = guiRotation.rotateY(dh.vrPlayer.vrdata_world_render.rotation_radians, new Matrix4f());
            guipos = VRPlayer.roomToWorldPos(posAvg, dh.vrPlayer.vrdata_world_render);

            // for mouse control
            guiRotation_room = guiRotation;
            guiScale = 2.0F;
            guiPos_room = posAvg;
        }

        if (guipos == null) {
            VRSettings.logger.error("Vivecraft: guipos was null, how did that happen. vrRunning: {}: ", VRState.vrRunning, new RuntimeException());
            guiPos_room = new Vector3f();
            guipos = VRPlayer.roomToWorldPos(guiPos_room, dh.vrPlayer.vrdata_world_render);
            guiRotation_room = new Matrix4f();
            guirot = new Matrix4f();
            guiScale = 1.0F;
        }

        Vec3 eye = RenderHelper.getSmoothCameraPosition(currentPass, dh.vrPlayer.vrdata_world_render);

        Vec3 translation = guipos.subtract(eye);
        poseStack.translate(translation.x, translation.y, translation.z);

        // offset from eye to gui pos
        poseStack.mulPoseMatrix(guirot);
        poseStack.translate(guilocal.x, guilocal.y, guilocal.z);

        float thescale = scale * dh.vrPlayer.vrdata_world_render.worldScale;
        poseStack.scale(thescale, thescale, thescale);

        guiScaleApplied = thescale;

        mc.getProfiler().pop();

        return guipos;
    }
}
