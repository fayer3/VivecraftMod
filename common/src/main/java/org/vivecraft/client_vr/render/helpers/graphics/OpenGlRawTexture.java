package org.vivecraft.client_vr.render.helpers.graphics;

import com.mojang.blaze3d.opengl.GlStateManager;

public class OpenGlRawTexture extends RawTexture {

    private final int glId;
    private final int fboId;
    private boolean valid = true;

    protected OpenGlRawTexture(String name, int width, int height, Format format, int glId, int fboId) {
        super(name, width, height, format);
        this.glId = glId;
        this.fboId = fboId;
    }

    protected int getFbo() {
        return this.fboId;
    }

    @Override
    public long getHandle() {
        return this.glId;
    }

    @Override
    public void destroy() {
        if (this.valid) {
            this.valid = false;
            GlStateManager._deleteTexture(this.glId);
            GlStateManager._glDeleteFramebuffers(this.fboId);
        }
    }
}
