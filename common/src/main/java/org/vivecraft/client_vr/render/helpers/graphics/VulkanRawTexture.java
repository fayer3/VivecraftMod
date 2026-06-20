package org.vivecraft.client_vr.render.helpers.graphics;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

public class VulkanRawTexture extends RawTexture {

    protected final long vkImage;
    private final long vmaAllocation;
    protected int currentLayout = VK10.VK_IMAGE_LAYOUT_UNDEFINED;

    protected VulkanRawTexture(String name, int width, int height, Format format) {
        super(name, width, height, format);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.calloc(stack).sType$Default();
            imageCreateInfo.imageType(VK10.VK_IMAGE_TYPE_2D);
            imageCreateInfo.extent().set(width, height, 1);
            imageCreateInfo.mipLevels(1);
            imageCreateInfo.arrayLayers(1);
            imageCreateInfo.format(VulkanHelper.getVkFormat(format));
            imageCreateInfo.tiling(VK10.VK_IMAGE_TILING_OPTIMAL);
            imageCreateInfo.initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
            imageCreateInfo.usage(VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT |
                VK10.VK_IMAGE_USAGE_SAMPLED_BIT);
            imageCreateInfo.sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);
            imageCreateInfo.samples(VK10.VK_SAMPLE_COUNT_1_BIT);
            imageCreateInfo.flags(0);
            VmaAllocationCreateInfo allocationCreateInfo = VmaAllocationCreateInfo.calloc(stack);
            allocationCreateInfo.usage(Vma.VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE);
            LongBuffer imageHandlePtr = stack.callocLong(1);
            PointerBuffer allocationHandlePtr = stack.callocPointer(1);
            VulkanHelper.crashIfFailure(
                Vma.vmaCreateImage(((VulkanHelper) GraphicsHelper.INSTANCE).getVma(), imageCreateInfo,
                    allocationCreateInfo, imageHandlePtr, allocationHandlePtr, null),
                "Failed to create image"
            );
            this.vkImage = imageHandlePtr.get(0);
            this.vmaAllocation = allocationHandlePtr.get(0);
        }
    }

    protected void transitionLayoutTo(
        VkCommandBuffer commandBuffer, int newLayout, int srcAccessMask, int dstAccessMask)
    {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack).sType$Default();
            barrier.oldLayout(this.currentLayout);
            barrier.newLayout(newLayout);
            barrier.srcAccessMask(srcAccessMask);
            barrier.dstAccessMask(dstAccessMask);
            barrier.srcQueueFamilyIndex(-1);
            barrier.dstQueueFamilyIndex(-1);
            barrier.image(this.vkImage);
            VkImageSubresourceRange subresourceRange = barrier.subresourceRange();
            subresourceRange.aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT);
            subresourceRange.baseMipLevel(0);
            subresourceRange.levelCount(1);
            subresourceRange.baseArrayLayer(0);
            subresourceRange.layerCount(1);

            VK12.vkCmdPipelineBarrier(commandBuffer, VK10.VK_PIPELINE_STAGE_TRANSFER_BIT,
                VK10.VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, barrier);
            this.currentLayout = newLayout;
        }
    }

    @Override
    public long getHandle() {
        return this.vkImage;
    }

    @Override
    public void destroy() {
        Vma.vmaDestroyImage(((VulkanHelper) GraphicsHelper.INSTANCE).getVma(), this.vkImage, this.vmaAllocation);
    }
}
