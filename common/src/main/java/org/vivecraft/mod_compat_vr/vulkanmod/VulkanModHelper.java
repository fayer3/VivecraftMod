package org.vivecraft.mod_compat_vr.vulkanmod;

import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.texture.SamplerManager;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.vivecraft.client.Xplat;

public class VulkanModHelper {

    private static final int VK_IMAGE_USAGE_TRANSFER_SRC_BIT  = 0x00000001;
    private static final int VK_IMAGE_USAGE_SAMPLED_BIT = 0x00000004;
    private static final int VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT = 0x00000010;

    private static final int VK_FORMAT_R8G8B8A8_UNORM = 37;

    private static final int VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL = 6;

    public static boolean isLoaded() {
        return Xplat.isModLoaded("vulkanmod");
    }

    public static void genAndSetOpenVRImage(int texId, int width, int height) {
        GlTexture texture = GlTexture.getTexture(texId);
        VulkanImage image = new VulkanImage.Builder(width, height)
            .setMipLevels(1)
            .setFormat(VK_FORMAT_R8G8B8A8_UNORM)
            .addUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_SAMPLED_BIT)
            .createVulkanImage();
        image.updateTextureSampler(0, SamplerManager.LINEAR_FILTERING_BIT);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            image.transitionImageLayout(stack, DeviceManager.getGraphicsQueue().beginCommands().getHandle(), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
        }

        texture.setVulkanImage(image);
    }

    public static long getVulkanImageId(int texId) {
        return GlTexture.getTexture(texId).getVulkanImage().getId();
    }
}
