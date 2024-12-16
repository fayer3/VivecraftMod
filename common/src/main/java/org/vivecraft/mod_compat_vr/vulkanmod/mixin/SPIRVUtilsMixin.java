package org.vivecraft.mod_compat_vr.vulkanmod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.ResourceLocationException;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.vivecraft.client.Xplat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

@Mixin(SPIRVUtils.class)
public class SPIRVUtilsMixin {
    @WrapOperation(method = "compileShaderAbsoluteFile", at = @At(value = "INVOKE", target = "Ljava/nio/file/Files;readAllBytes(Ljava/nio/file/Path;)[B"), remap = false)
    private static byte[] compileShaderAbsoluteFile(Path path, Operation<byte[]> original) {
        // vulkanmod inits before the resource manager and uses class paths by default.
        // we need to change that for it to find our shaders
        if (path.toString().contains("/assets/vulkanmod/shaders/") ||
            path.toString().contains("\\assets\\vulkanmod\\shaders\\"))
        {
            // get asset path
            int assetIndex = path.toString().indexOf("/assets/vulkanmod/shaders/");
            if (assetIndex == -1) {
                assetIndex = path.toString().indexOf("\\assets\\vulkanmod\\shaders\\");
            }

            if (Minecraft.getInstance().getResourceManager() != null) {
                String asset = path.toString().substring(assetIndex + 18);
                try (InputStream is = Minecraft.getInstance().getResourceManager()
                    .open(new ResourceLocation("vulkanmod", asset.replace("\\", "/"))))
                {
                    return is.readAllBytes();
                } catch (IOException | ResourceLocationException ignored) {
                    // somehow vulkanmods shaders aren't in the resource manager
                }
            }
            String asset = path.toString().substring(assetIndex + 1);
            return original.call(Xplat.getJarPath("vulkanmod").resolve(asset));
        }
        return original.call(path);
    }
}
