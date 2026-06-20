package org.vivecraft.mod_compat_vr.vulkanmod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.vulkanmod.render.engine.VkGpuDevice;
import net.vulkanmod.render.engine.VkGpuTexture;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.queue.Queue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.vivecraft.client_vr.render.helpers.graphics.VulkanHelper;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VulkanModVulkanHelper extends VulkanHelper {

    private final Field Vulkan_VkInstance;

    private final Map<VkCommandBuffer, CommandPool.CommandBuffer> bufferMap = new HashMap<>();

    public VulkanModVulkanHelper() {
        // make sure the VkInstance is accessible
        try {
            this.Vulkan_VkInstance = Vulkan.class.getDeclaredField("instance");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Vivecraft: Failed to initialize VulkanMod support");
        }
    }

    private VkGpuDevice getVulkanDevice() {
        if (RenderSystem.getDevice().backend instanceof VkGpuDevice vulkanDevice) {
            return vulkanDevice;
        } else {
            throw new IllegalArgumentException("Vivecraft: not a vulkan device in vulkan context");
        }
    }

    private VkGpuTexture getVulkanTexture(GpuTexture texture) {
        if (texture instanceof VkGpuTexture vulkanTexture) {
            return vulkanTexture;
        }
        throw new IllegalArgumentException("Vivecraft: not a vulkan texture in vulkan context");
    }

    @Override
    protected VkCommandBuffer allocateAndBeginCommandBuffer() {
        CommandPool.CommandBuffer commandBuffer = DeviceManager.getGraphicsQueue().getCommandBuffer();
        this.bufferMap.put(commandBuffer.handle, commandBuffer);
        return commandBuffer.handle;
    }

    @Override
    protected void endCommandBuffer(VkCommandBuffer commandBuffer) {
        DeviceManager.getGraphicsQueue().submitCommands(this.bufferMap.get(commandBuffer));
        this.bufferMap.remove(commandBuffer);
    }

    @Override
    public long getTextureHandle(GpuTexture texture) {
        return getVulkanTexture(texture).getVulkanImage().getId();
    }

    @Override
    protected int getImageLayout(GpuTexture texture) {
        return getVulkanTexture(texture).getVulkanImage().getCurrentLayout();
    }

    @Override
    protected void setImageLayout(GpuTexture texture, int newLayout) {
        getVulkanTexture(texture).getVulkanImage().setCurrentLayout(newLayout);
    }

    @Override
    protected Set<String> getAvailableInstanceExtensions() {
        return Set.of();
    }

    @Override
    protected Set<String> getAvailableDeviceExtensions() {
        return Set.of();
    }

    @Override
    protected Set<String> getEnabledExtensions() {
        return new HashSet<>(getVulkanDevice().getEnabledExtensions());
    }

    @Override
    public long getDevicePointer() {
        return DeviceManager.vkDevice.address();
    }

    @Override
    public VkPhysicalDevice getPhysicalDevice() {
        return DeviceManager.vkDevice.getPhysicalDevice();
    }

    @Override
    public long getVma() {
        return Vulkan.getAllocator();
    }

    @Override
    public long getQueuePointer() {
        return DeviceManager.getGraphicsQueue().vkQueue().address();
    }

    @Override
    public int getQueueFamilyIndex() {
        return Queue.getQueueFamilies().graphicsFamily;
    }

    @Override
    public long getInstancePointer() {
        try {
            return ((VkInstance) this.Vulkan_VkInstance.get(null)).address();
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Vivecfraft: failed to get VkInstance pointer");
        }
    }
}
