package org.vivecraft.mixin.client_vr.lwjgl;

import com.llamalad7.mixinextras.sugar.Local;
import org.lwjgl.Version;
import org.lwjgl.system.Library;
import org.lwjgl.system.Platform;
import org.lwjgl.system.Pointer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * compatibility mixin, to make it possible to load lwjgl 3.3.2 openvr on lwjgl 3.2.2
 */
@Mixin(Library.class)
public class LibraryMixin {
    @ModifyArg(method = {
        "loadNative(Ljava/lang/Class;Ljava/lang/String;ZZ)Lorg/lwjgl/system/SharedLibrary;",
        "loadSystem(Ljava/util/function/Consumer;Ljava/util/function/Consumer;Ljava/lang/Class;Ljava/lang/String;)V"
    }, at = @At(value = "INVOKE", target = "Ljava/lang/ClassLoader;getResource(Ljava/lang/String;)Ljava/net/URL;"), remap = false, expect = 0, require = 0)
    private static String vivecraft$changeOpenVrLibName(String libName, @Local(argsOnly = true) String name) {
        // starting with 3.2.3 loading has a module name, which is what we ship
        if (Version.getVersion().startsWith("3.2.2") && (name.equals("openvr_api") || name.equals("lwjgl_openvr"))) {
            libName = String.format("%s/%s/%s/%s", Platform.get().getName().toLowerCase(),
                Pointer.BITS64 ? "x64" : "x86", "org/lwjgl/openvr", libName);
        }
        return libName;
    }
}
