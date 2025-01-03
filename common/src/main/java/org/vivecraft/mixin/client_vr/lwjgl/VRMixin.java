package org.vivecraft.mixin.client_vr.lwjgl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.lwjgl.openvr.VR;
import org.lwjgl.system.Library;
import org.lwjgl.system.SharedLibrary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * compatibility mixin, to make it possible to load lwjgl 3.3.2 openvr on lwjgl 3.2.2
 */
@Mixin(VR.class)
public class VRMixin {
    @WrapOperation(method = "<clinit>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/system/Library;loadNative(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;Z)Lorg/lwjgl/system/SharedLibrary;"), remap = false, expect = 0, require = 0)
    private static SharedLibrary vivecraft$fixLoadingOn322(
        Class<?> context, String module, String name, boolean bundledWithLWJGL, Operation<SharedLibrary> original)
    {
        try {
            return original.call(context, module, name, bundledWithLWJGL);
        } catch (NoSuchMethodError expected) {
            try {
                // lwjgl 3.2.2 doesn't have the module field
                Method loadNative = Library.class.getMethod("loadNative", Class.class, String.class, boolean.class);
                return (SharedLibrary) loadNative.invoke(null, context, name, bundledWithLWJGL);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to load OpenVR native library", e);
            }
        }
    }
}
