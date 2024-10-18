package org.vivecraft.client_xr.render_pass;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.vivecraft.client_vr.render.RenderPass;

import java.io.IOException;

public class WorldRenderPass implements AutoCloseable {

    public static WorldRenderPass stereoXR;
    public static WorldRenderPass center;
    public static WorldRenderPass mixedReality;
    public static WorldRenderPass leftTelescope;
    public static WorldRenderPass rightTelescope;
    public static WorldRenderPass camera;

    public final RenderTarget target;

    /**
     * creates a WorldRenderPass that writes to {@code target}
     * @param target RenderTarget for this pass
     * @throws IOException when an error occurs during shader loading
     */
    public WorldRenderPass(RenderTarget target) throws IOException {
        this.target = target;
    }

    /**
     * @param pass RenderPass to get the WorldRenderPass for
     * @return the WorldRenderPass object corresponding to the given {@code pass}
     */
    public static WorldRenderPass getByRenderPass(RenderPass pass) {
        return switch (pass) {
            case CENTER -> center;
            case THIRD -> mixedReality;
            case SCOPEL -> leftTelescope;
            case SCOPER -> rightTelescope;
            case CAMERA -> camera;
            default -> stereoXR;
        };
    }

    /**
     * resizes the RenderTarget of this pass to the given size
     * @param width new width
     * @param height new height
     */
    public void resize(int width, int height) {
        this.target.resize(width, height, Minecraft.ON_OSX);
    }

    /**
     * releases all buffers hold by this pass
     */
    @Override
    public void close() {
        this.target.destroyBuffers();
    }
}
