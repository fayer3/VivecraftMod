package org.vivecraft.mod_compat_vr.veil;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.impl.client.render.pipeline.VeilFirstPersonRenderer;
import org.vivecraft.Xloader;

public class VeilHelper {

    public static boolean isLoaded() {
        return Xloader.isModLoaded("veil");
    }

    public static void startFrame() {
        VeilRenderSystem.beginFrame();
    }

    public static void endFrame() {
        VeilRenderSystem.endFrame();
    }

    public static void beginFirstPerson() {
        VeilFirstPersonRenderer.bind();
    }

    public static void endFirstPerson() {
        VeilFirstPersonRenderer.unbind();
    }

    public static void resize(int width, int height) {
        VeilRenderSystem.resize(width, height);
    }
}
