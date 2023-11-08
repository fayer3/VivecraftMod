package org.vivecraft.mixin.client.blaze3d;

import com.mojang.blaze3d.platform.MemoryTracker;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MemoryTracker.class)
public interface MemoryTrackerAccessor {
    @Accessor
    static MemoryUtil.MemoryAllocator getALLOCATOR() {
        return null;
    }
}
