package org.vivecraft.mixin.client.renderer.entity.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.vivecraft.client.ClientVRPlayers;
import org.vivecraft.client.extensions.EntityRenderStateExtension;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements EntityRenderStateExtension {

    @Unique
    private ClientVRPlayers.RotInfo vivecraft$rotInfo;

    @Unique
    private boolean vivecraft$isMainPlayer;

    @Unique
    private float vivecraft$totalScale;

    @Override
    public ClientVRPlayers.RotInfo vivecraft$getRotInfo() {
        return this.vivecraft$rotInfo;
    }

    @Override
    public void vivecraft$setRotInfo(ClientVRPlayers.RotInfo rotInfo) {
        this.vivecraft$rotInfo = rotInfo;
    }

    @Override
    public boolean vivecraft$isMainPlayer() {
        return this.vivecraft$isMainPlayer;
    }

    @Override
    public void vivecraft$setMainPlayer(boolean mainPlayer) {
        this.vivecraft$isMainPlayer = mainPlayer;
    }

    @Override
    public float vivecraft$getTotalScale() {
        return this.vivecraft$totalScale;
    }

    @Override
    public void vivecraft$setTotalScale(float totalScale) {
        this.vivecraft$totalScale = totalScale;
    }
}
