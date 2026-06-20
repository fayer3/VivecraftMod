package org.vivecraft.client_vr.render.helpers.graphics;

import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.vivecraft.client_vr.VRTextureTarget;
import org.vivecraft.client_vr.render.RenderConfigException;
import org.vivecraft.client_vr.settings.VRSettings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class VulkanHelper implements GraphicsHelper {

    private RawTexture.Format[] supportedFormats = null;

    protected abstract VkCommandBuffer allocateAndBeginCommandBuffer();

    protected abstract void endCommandBuffer(VkCommandBuffer commandBuffer);

    @Override
    public abstract long getTextureHandle(GpuTexture texture);

    protected abstract int getImageLayout(GpuTexture texture);

    protected abstract void setImageLayout(GpuTexture texture, int newLayout);

    @Override
    public RawTexture createTexture(String name, int width, int height, RawTexture.Format format) {
        return new VulkanRawTexture(name, width, height, format);
    }

    @Override
    public void blitTextures(VRTextureTarget[] sources, RawTexture[] targets) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBuffer blitCommandBuffer = this.allocateAndBeginCommandBuffer();
            for (int i = 0; i < sources.length; ++i) {
                VulkanRawTexture target = ((VulkanRawTexture) targets[i]);

                // transition image layout
                target.transitionLayoutTo(blitCommandBuffer, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK10.VK_ACCESS_TRANSFER_READ_BIT, VK10.VK_ACCESS_TRANSFER_WRITE_BIT);

                VkOffset3D.Buffer offsets = VkOffset3D.calloc(2, stack);
                offsets.x(0)
                    .y(0)
                    .z(0);
                offsets.position(1);
                offsets.x(sources[i].getColorTexture().getWidth(0))
                    .y(sources[i].getColorTexture().getHeight(0))
                    .z(1);
                offsets.position(0);

                VkImageSubresourceLayers subresource = VkImageSubresourceLayers.calloc(stack);
                subresource.aspectMask(1);
                subresource.mipLevel(0);
                subresource.baseArrayLayer(0);
                subresource.layerCount(1);

                VkImageBlit.Buffer blitRegion = VkImageBlit.calloc(1, stack);
                blitRegion.srcSubresource(subresource);
                blitRegion.srcOffsets(offsets);
                blitRegion.dstSubresource(subresource);
                blitRegion.dstOffsets(offsets);
                VK12.vkCmdBlitImage(blitCommandBuffer,
                    getTextureHandle(sources[i].getColorTexture()), getImageLayout(sources[i].getColorTexture()),
                    target.getHandle(), target.currentLayout,
                    blitRegion, VK10.VK_FILTER_NEAREST);

                // transition image layout
                target.transitionLayoutTo(blitCommandBuffer, VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    VK10.VK_ACCESS_TRANSFER_WRITE_BIT, VK10.VK_ACCESS_TRANSFER_READ_BIT);
            }
            endCommandBuffer(blitCommandBuffer);
        }
    }

    @Override
    public RawTexture.Format[] supportedTextureFormats() {
        if (this.supportedFormats == null) {
            List<RawTexture.Format> supported = new ArrayList<>();
            for (RawTexture.Format format : RawTexture.Format.values()) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    VkPhysicalDeviceImageFormatInfo2 formatInfo = VkPhysicalDeviceImageFormatInfo2.calloc(stack)
                        .sType$Default();
                    VkImageFormatProperties2 supportedProperties = VkImageFormatProperties2.calloc(stack)
                        .sType$Default();

                    // set needed properties
                    formatInfo.format(getVkFormat(format));
                    formatInfo.type(VK10.VK_IMAGE_TYPE_2D);
                    formatInfo.tiling(VK10.VK_IMAGE_TILING_OPTIMAL);
                    formatInfo.usage(VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT |
                        VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT |
                        VK10.VK_IMAGE_USAGE_SAMPLED_BIT);
                    formatInfo.flags(0);

                    int res = VK11.vkGetPhysicalDeviceImageFormatProperties2(this.getPhysicalDevice(), formatInfo,
                        supportedProperties);
                    if (res == VK10.VK_SUCCESS) {
                        supported.add(format);
                    } else {
                        VRSettings.LOGGER.error("Vivecraft: format {} not supported", format);
                    }
                }
            }
            this.supportedFormats = supported.toArray(new RawTexture.Format[0]);
        }

        return this.supportedFormats;
    }

    @Override
    public void genMipmaps(GpuTexture texture) {
        long vkImage = getTextureHandle(texture);

        VkCommandBuffer blitCommandBuffer = this.allocateAndBeginCommandBuffer();

        int oldLayout = getImageLayout(texture);

        // transfer base level to src optimal
        transitionImageLayoutTo(blitCommandBuffer, vkImage,
            0, 1,
            oldLayout, VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
            VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT, VK10.VK_ACCESS_TRANSFER_READ_BIT,
            VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, VK10.VK_PIPELINE_STAGE_TRANSFER_BIT);

        for (int i = 1; i < texture.getMipLevels(); i++) {
            // transition the target layer to dst optimal
            transitionImageLayoutTo(blitCommandBuffer, vkImage,
                i, 1,
                oldLayout, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT, VK10.VK_ACCESS_TRANSFER_WRITE_BIT,
                VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, VK10.VK_PIPELINE_STAGE_TRANSFER_BIT);

            // blit
            blitTexture(blitCommandBuffer,
                vkImage, i - 1, 0, 0, texture.getWidth(i - 1), texture.getHeight(i - 1),
                vkImage, i, 0, 0, texture.getWidth(i), texture.getHeight(i));

            // transition the source layer to src optimal for next layer
            transitionImageLayoutTo(blitCommandBuffer, vkImage,
                i, 1,
                VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                VK10.VK_ACCESS_TRANSFER_WRITE_BIT, VK10.VK_ACCESS_TRANSFER_READ_BIT,
                VK10.VK_PIPELINE_STAGE_TRANSFER_BIT, VK10.VK_PIPELINE_STAGE_TRANSFER_BIT);
        }

        // every mip is now in src optimal, transfer all mips at once back into the genreal layout
        if (oldLayout != VK10.VK_IMAGE_LAYOUT_UNDEFINED) {
            transitionImageLayoutTo(blitCommandBuffer, vkImage,
                0, texture.getMipLevels(),
                VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, oldLayout,
                VK10.VK_ACCESS_TRANSFER_READ_BIT, VK10.VK_ACCESS_SHADER_READ_BIT,
                VK10.VK_PIPELINE_STAGE_TRANSFER_BIT, VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT);
        } else {
            setImageLayout(texture, VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
        }

        this.endCommandBuffer(blitCommandBuffer);
    }

    /**
     * blits the source image/mip rectangle to the target image/mip rectangle, with linear interpolation
     *
     * @param commandBuffer commandbuffer to submit the calls to
     * @param sourceImage   source image handle
     * @param sourceMip     mip level of the source image to copy frome
     * @param sourceX       source X position to copy from
     * @param sourceY       source Y position to copy from
     * @param sourceWidth   width of the source rectangle to copy from
     * @param sourceHeight  height of the source rectangle to copy from
     * @param targetImage   target image handle
     * @param targetMip     mip level of the target image to copy to
     * @param targetX       target X position to copy to
     * @param targetY       target Y position to copy to
     * @param targetWidth   width of the target rectangle to copy to
     * @param targetHeight  height of the target rectangle to copy to
     */
    private void blitTexture(
        VkCommandBuffer commandBuffer,
        long sourceImage, int sourceMip, int sourceX, int sourceY, int sourceWidth, int sourceHeight,
        long targetImage, int targetMip, int targetX, int targetY, int targetWidth, int targetHeight)
    {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkOffset3D.Buffer srcOffsets = VkOffset3D.calloc(2, stack);
            srcOffsets.x(sourceX)
                .y(sourceY)
                .z(0);
            srcOffsets.position(1);
            srcOffsets.x(sourceX + sourceWidth)
                .y(sourceY + sourceHeight)
                .z(1);
            srcOffsets.position(0);

            VkOffset3D.Buffer dstOffsets = VkOffset3D.calloc(2, stack);
            dstOffsets.x(targetX)
                .y(targetY)
                .z(0);
            dstOffsets.position(1);
            dstOffsets.x(targetX + targetWidth)
                .y(targetY + targetHeight)
                .z(1);
            dstOffsets.position(0);

            VkImageSubresourceLayers srcSubresource = VkImageSubresourceLayers.calloc(stack);
            srcSubresource.aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT);
            srcSubresource.mipLevel(sourceMip);
            srcSubresource.baseArrayLayer(0);
            srcSubresource.layerCount(1);

            VkImageSubresourceLayers dstSubresource = VkImageSubresourceLayers.calloc(stack);
            dstSubresource.aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT);
            dstSubresource.mipLevel(targetMip);
            dstSubresource.baseArrayLayer(0);
            dstSubresource.layerCount(1);

            VkImageBlit.Buffer blitRegion = VkImageBlit.calloc(1, stack);
            blitRegion.srcSubresource(srcSubresource);
            blitRegion.srcOffsets(srcOffsets);
            blitRegion.dstSubresource(dstSubresource);
            blitRegion.dstOffsets(dstOffsets);

            VK12.vkCmdBlitImage(commandBuffer,
                sourceImage, VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                targetImage, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                blitRegion, VK10.VK_FILTER_LINEAR);
        }
    }

    /**
     * transitions the layout of the given image to the specified one
     *
     * @param commandBuffer command buffer to add the layout barrier to
     * @param vkImage       image to changethe layout of
     * @param baseMip       mip level to transition
     * @param numberOfMips  count of mips that should be transitioned (including the base mip)
     * @param oldLayout     cuurrent layout of the image
     * @param newLayout     new layout of the image
     * @param srcAccessMask access bits of what has been done with the image so far
     * @param dstAccessMask access bits of what the intent of the image is now
     * @param srcStageMask  stage bits of what has been done with the image so far
     * @param dstStageMask  stage bits of what the intent of the image is now
     */
    protected void transitionImageLayoutTo(
        VkCommandBuffer commandBuffer, long vkImage, int baseMip, int numberOfMips, int oldLayout, int newLayout,
        int srcAccessMask, int dstAccessMask, int srcStageMask, int dstStageMask)
    {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack).sType$Default();
            barrier.oldLayout(oldLayout);
            barrier.newLayout(newLayout);
            barrier.srcAccessMask(srcAccessMask);
            barrier.dstAccessMask(dstAccessMask);
            barrier.srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED);
            barrier.image(vkImage);
            VkImageSubresourceRange subresourceRange = barrier.subresourceRange();
            subresourceRange.aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT);
            subresourceRange.baseMipLevel(baseMip);
            subresourceRange.levelCount(numberOfMips);
            subresourceRange.baseArrayLayer(0);
            subresourceRange.layerCount(1);

            VK12.vkCmdPipelineBarrier(commandBuffer, srcStageMask, dstStageMask, 0, null, null, barrier);
        }
    }

    @Override
    public String checkError(String errorSection) {
        // can't check errors like that on vulkan
        return "";
    }

    @Override
    public boolean isStencil() {
        return false;
    }

    @Override
    public void setStencil(boolean state) {}

    @Override
    public void flush() {}

    @Override
    public boolean flipEyeVertically() {
        return true;
    }

    /**
     * checks that the given extensions are supported. throws a RenderConfigException if the are not supported, or if they are not enabled
     *
     * @param instanceExtensions instance extension that are needed
     * @param deviceExtensions   device extension that are needed
     * @throws RenderConfigException thrown if something is missing
     */
    public void checkExtensionSupport(
        List<String> instanceExtensions, List<String> deviceExtensions) throws RenderConfigException
    {
        // get all supported extensions
        Set<String> availableDeviceExtensions = this.getAvailableDeviceExtensions();
        this.getAvailableInstanceExtensions();

        Set<String> availableInstanceExtensions = this.getAvailableInstanceExtensions();

        Set<String> missingExtensions = new HashSet<>();
        for (String extension : instanceExtensions) {
            if (!availableInstanceExtensions.contains(extension)) {
                missingExtensions.add("Instance extension: " + extension);
            }
        }

        for (String extension : deviceExtensions) {
            if (!availableDeviceExtensions.contains(extension)) {
                missingExtensions.add("Device extension: " + extension);
            }
        }

        if (!missingExtensions.isEmpty()) {
            //  unsupported extensions, abort with unsupported
            MutableComponent error = Component.translatable("vivecraft.messages.vulkanunsupported");

            for (String ext : missingExtensions) {
                error.append(Component.literal("\n" + ext));
            }
            throw new RenderConfigException(Component.translatable("vivecraft.messages.incompatiblegpu"), error);
        }
        // all extensions supported, check if they are already enabled
        Set<String> enabledExtensions = this.getEnabledExtensions();
        for (String extension : instanceExtensions) {
            if (!enabledExtensions.contains(extension + " (I)")) {
                missingExtensions.add("Instance extension: " + extension);
            }
        }

        for (String extension : deviceExtensions) {
            if (!enabledExtensions.contains(extension + " (D)")) {
                missingExtensions.add("Device extension: " + extension);
            }
        }
        if (!missingExtensions.isEmpty()) {
            VRSettings.LOGGER.info(
                "Vivecraft: Not all needed Vulkan Extensions are enabled, game restart required. Not enabled Extensions:\n{}",
                String.join("\n", missingExtensions));
            throw new RenderConfigException(Component.translatable("vivecraft.messages.vulkanrestarttitle"),
                Component.translatable("vivecraft.messages.vulkanrestart"));
        }
    }

    public static int getVkFormat(RawTexture.Format format) {
        return switch (format) {
            case R8G8B8A8_UNORM -> VK10.VK_FORMAT_R8G8B8A8_UNORM;
            case B8G8R8A8_UNORM -> VK10.VK_FORMAT_B8G8R8A8_UNORM;
            case R8G8B8A8_SRGB -> VK10.VK_FORMAT_R8G8B8A8_SRGB;
            case B8G8R8A8_SRGB -> VK10.VK_FORMAT_B8G8R8A8_SRGB;
            case R16G16B16A16_SFLOAT -> VK10.VK_FORMAT_R16G16B16A16_SFLOAT;
            case R32G32B32_SFLOAT -> VK10.VK_FORMAT_R32G32B32_SFLOAT;
            case R32G32B32A32_SFLOAT -> VK10.VK_FORMAT_R32G32B32A32_SFLOAT;
        };
    }

    public static String resultToString(final int error) {
        return switch (error) {
            case VK12.VK_ERROR_INVALID_OPAQUE_CAPTURE_ADDRESS -> "VK_ERROR_INVALID_OPAQUE_CAPTURE_ADDRESS";
            case VK12.VK_ERROR_FRAGMENTATION -> "VK_ERROR_FRAGMENTATION";
            case VK12.VK_ERROR_INVALID_EXTERNAL_HANDLE -> "VK_ERROR_INVALID_EXTERNAL_HANDLE";
            case VK12.VK_ERROR_OUT_OF_POOL_MEMORY -> "VK_ERROR_OUT_OF_POOL_MEMORY";
            case KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR -> "VK_ERROR_OUT_OF_DATE_KHR";
            case KHRSurface.VK_ERROR_NATIVE_WINDOW_IN_USE_KHR -> "VK_ERROR_NATIVE_WINDOW_IN_USE_KHR";
            case KHRSurface.VK_ERROR_SURFACE_LOST_KHR -> "VK_ERROR_SURFACE_LOST_KHR";
            case VK10.VK_ERROR_UNKNOWN -> "VK_ERROR_UNKNOWN";
            case VK10.VK_ERROR_FRAGMENTED_POOL -> "VK_ERROR_FRAGMENTED_POOL";
            case VK10.VK_ERROR_FORMAT_NOT_SUPPORTED -> "VK_ERROR_FORMAT_NOT_SUPPORTED";
            case VK10.VK_ERROR_TOO_MANY_OBJECTS -> "VK_ERROR_TOO_MANY_OBJECTS";
            case VK10.VK_ERROR_INCOMPATIBLE_DRIVER -> "VK_ERROR_INCOMPATIBLE_DRIVER";
            case VK10.VK_ERROR_FEATURE_NOT_PRESENT -> "VK_ERROR_FEATURE_NOT_PRESENT";
            case VK10.VK_ERROR_EXTENSION_NOT_PRESENT -> "VK_ERROR_EXTENSION_NOT_PRESENT";
            case VK10.VK_ERROR_LAYER_NOT_PRESENT -> "VK_ERROR_LAYER_NOT_PRESENT";
            case VK10.VK_ERROR_MEMORY_MAP_FAILED -> "VK_ERROR_MEMORY_MAP_FAILED";
            case VK10.VK_ERROR_DEVICE_LOST -> "VK_ERROR_DEVICE_LOST";
            case VK10.VK_ERROR_INITIALIZATION_FAILED -> "VK_ERROR_INITIALIZATION_FAILED";
            case VK10.VK_ERROR_OUT_OF_DEVICE_MEMORY -> "VK_ERROR_OUT_OF_DEVICE_MEMORY";
            case VK10.VK_ERROR_OUT_OF_HOST_MEMORY -> "VK_ERROR_OUT_OF_HOST_MEMORY";
            case VK10.VK_SUCCESS -> "VK_SUCCESS";
            case VK10.VK_NOT_READY -> "VK_NOT_READY";
            case VK10.VK_TIMEOUT -> "VK_TIMEOUT";
            case VK10.VK_EVENT_SET -> "VK_EVENT_SET";
            case VK10.VK_EVENT_RESET -> "VK_EVENT_RESET";
            case VK10.VK_INCOMPLETE -> "VK_INCOMPLETE";
            case KHRSwapchain.VK_SUBOPTIMAL_KHR -> "VK_SUBOPTIMAL_KHR";
            default -> "0x" + Integer.toHexString(error);
        };
    }

    public static void crashIfFailure(int result, String message) {
        if (result < VK10.VK_SUCCESS) {
            throw new RuntimeException(resultToString(result) + ": " + message);
        }
    }

    protected abstract Set<String> getAvailableInstanceExtensions();

    protected abstract Set<String> getAvailableDeviceExtensions();

    protected abstract Set<String> getEnabledExtensions();

    public abstract long getDevicePointer();

    public abstract VkPhysicalDevice getPhysicalDevice();

    public abstract long getVma();

    public abstract long getQueuePointer();

    public abstract int getQueueFamilyIndex();

    public abstract long getInstancePointer();
}
