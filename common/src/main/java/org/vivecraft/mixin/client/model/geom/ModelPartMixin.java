package org.vivecraft.mixin.client.model.geom;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client.extensions.ModelPartExtension;

@Mixin(ModelPart.class)
public class ModelPartMixin implements ModelPartExtension {

    @Unique
    protected float vivecraft$scaleX = 1F;

    @Unique
    protected float vivecraft$scaleY = 1F;

    @Unique
    protected float vivecraft$scaleZ = 1F;

    @Inject(method = "translateAndRotate", at = @At("TAIL"))
    private void vivecraft$scaleModelPart(PoseStack poseStack, CallbackInfo ci) {
        if (this.vivecraft$scaleX != 1.0F || this.vivecraft$scaleY != 1.0F || this.vivecraft$scaleZ != 1.0F) {
            poseStack.scale(this.vivecraft$scaleX, this.vivecraft$scaleY, this.vivecraft$scaleZ);
        }
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void vivecraft$scaleModelPart(ModelPart modelPart, CallbackInfo ci) {
        ((ModelPartMixin) (Object) modelPart).vivecraft$copyScaleTo((ModelPart) (Object) this);
    }

    @Unique
    @Override
    public void vivecraft$setScale(float x, float y, float z) {
        this.vivecraft$scaleX = x;
        this.vivecraft$scaleY = y;
        this.vivecraft$scaleZ = z;
    }

    @Unique
    @Override
    public void vivecraft$copyScaleTo(ModelPart other) {
        ((ModelPartExtension) (Object) other).vivecraft$setScale(this.vivecraft$scaleX, this.vivecraft$scaleY,
            this.vivecraft$scaleZ);
    }
}
