package org.vivecraft.fabric.mixin.client.resources.model;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.gameplay.trackers.CameraTracker;
import org.vivecraft.client_vr.gameplay.trackers.ClimbTracker;
import org.vivecraft.client_vr.gameplay.trackers.TelescopeTracker;
import org.vivecraft.client_vr.settings.VRSettings;

import java.io.IOException;

@Mixin(ModelBakery.class)
public abstract class FabricModelBakeryMixin {
    @Shadow
    protected abstract void registerModel(ModelResourceLocation modelResourceLocation, UnbakedModel unbakedModel);

    @Shadow
    protected abstract BlockModel loadBlockModel(ResourceLocation resourceLocation) throws IOException;

    @Inject(method = "<init>", at = @At(value = "CONSTANT", args = "stringValue=special"))
    private void vivecraft$fabricLoadModels(CallbackInfo ci) {
        // item models
        this.vivecraft$loadBlockModel(TelescopeTracker.SCOPE_MODEL);
        this.vivecraft$loadBlockModel(ClimbTracker.CLAWS_MODEL);

        // blockmodels
        this.vivecraft$loadBlockModel(ClientDataHolderVR.THIRD_PERSON_CAMERA_MODEL);
        this.vivecraft$loadBlockModel(ClientDataHolderVR.THIRD_PERSON_CAMERA_DISPLAY_MODEL);
        this.vivecraft$loadBlockModel(CameraTracker.CAMERA_MODEL);
        this.vivecraft$loadBlockModel(CameraTracker.CAMERA_DISPLAY_MODEL);
    }

    @Unique
    private void vivecraft$loadBlockModel(ModelResourceLocation modelResourceLocation) {
        try {
            this.registerModel(modelResourceLocation, this.loadBlockModel(modelResourceLocation.id()));
        } catch (IOException e) {
            VRSettings.LOGGER.error("Failed to load vivecraft model '{}': {}", modelResourceLocation, e.getMessage());
        }
    }
}
