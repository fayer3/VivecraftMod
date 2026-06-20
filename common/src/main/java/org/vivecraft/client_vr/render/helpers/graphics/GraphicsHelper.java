package org.vivecraft.client_vr.render.helpers.graphics;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import org.vivecraft.Xloader;
import org.vivecraft.client_vr.VRTextureTarget;
import org.vivecraft.mod_compat_vr.vulkanmod.VulkanModVulkanHelper;

public interface GraphicsHelper {

    GraphicsHelper INSTANCE = getHelper();

    private static GraphicsHelper getHelper() {
        if (RenderSystem.getDevice().backend instanceof GlDevice) {
            return new OpenGLHelper();
        } else if (Xloader.INSTANCE.isModLoaded("vulkanmod")) {
            return new VulkanModVulkanHelper();
        } else {
            throw new IllegalStateException(
                "Vivecraft: Unsupported backend: " + RenderSystem.getDevice().backend.getBackendName() +
                    " with class: " + RenderSystem.getDevice().backend.getClass().getName());
        }
    }

    /**
     * creates a raw texture of the given size and format
     *
     * @param name   internal name of the texture
     * @param width  width of the texture
     * @param height height of the texture
     * @param format texture format that should be used
     * @return the created texture
     */
    RawTexture createTexture(String name, int width, int height, RawTexture.Format format);

    /**
     * copies the RenderTargets color images to the given RawTextures, need to be the same count
     *
     * @param sources source RenderTargets
     * @param targets target RawTextures
     */
    void blitTextures(VRTextureTarget[] sources, RawTexture[] targets);

    /**
     * queries whiuch formats are supported by the gpu
     *
     * @return array of all supported formats
     */
    RawTexture.Format[] supportedTextureFormats();

    /**
     * Generates api texture handle for the given GpuTexture
     *
     * @param texture GpuTexture to get the texture handle for
     */
    long getTextureHandle(GpuTexture texture);

    /**
     * Generates mipmaps for the given GpuTexture
     *
     * @param texture GpuTexture to generate mipmaps for
     */
    void genMipmaps(GpuTexture texture);

    String checkError(String errorSection);

    boolean isStencil();

    void setStencil(boolean state);

    void flush();

    /**
     * @return if the eye buffer needs to be flipped vertically
     */
    default boolean flipEyeVertically() {
        return false;
    }
}
