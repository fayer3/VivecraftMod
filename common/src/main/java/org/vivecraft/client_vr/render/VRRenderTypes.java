package org.vivecraft.client_vr.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.function.Function;

public class VRRenderTypes {
    private static final Function<RenderTarget, RenderType> ENTITY_TRANSLUCENT = Util.memoize(
        (target) -> RenderType.create("entity_translucent_vr",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true,
            RenderType.CompositeState.builder()
                .setShaderState(RenderType.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(getTexture(target))
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .createCompositeState(false)));

    private static final Function<RenderTarget, RenderType> ENTITY_SOLID = Util.memoize(
        target -> RenderType.create("entity_solid_vr",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false,
            RenderType.CompositeState.builder()
                .setShaderState(RenderType.RENDERTYPE_ENTITY_SOLID_SHADER)
                .setTextureState(getTexture(target))
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .createCompositeState(false)));

    private static final Function<RenderTarget, RenderType> ENTITY_CUTOUT = Util.memoize(
        target -> RenderType.create("entity_cutout_vr",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false,
            RenderType.CompositeState.builder()
                .setShaderState(RenderType.RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                .setTextureState(getTexture(target))
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .createCompositeState(false)));

    private static RenderStateShard.EmptyTextureStateShard getTexture(RenderTarget target) {
        return new RenderStateShard.EmptyTextureStateShard(
            () -> RenderSystem.setShaderTexture(0, target.getColorTextureId()), () -> {});
    }

    public static RenderType entityTranslucent(RenderTarget target) {
        return ENTITY_TRANSLUCENT.apply(target);
    }
    public static RenderType entitySolid(RenderTarget target) {
        return ENTITY_SOLID.apply(target);
    }
    public static RenderType entityCutout(RenderTarget target) {
        return ENTITY_CUTOUT.apply(target);
    }
}
