package org.vivecraft.client_vr.render.helpers.graphics;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.*;
import org.vivecraft.client_vr.VRTextureTarget;
import org.vivecraft.client_vr.settings.VRSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenGLHelper implements GraphicsHelper {

    private final boolean directStateSupport;
    private final boolean formatQuerySupport;

    private RawTexture.Format[] supportedFormats;

    public OpenGLHelper() {
        GLCapabilities cap = GL.getCapabilities();

        this.directStateSupport = cap.GL_ARB_direct_state_access;
        this.formatQuerySupport = cap.GL_ARB_internalformat_query;
    }

    private GlDevice getGlDevice() {
        if (RenderSystem.getDevice().backend instanceof GlDevice glDevice) {
            return glDevice;
        } else {
            throw new IllegalArgumentException("Vivecraft: not a opengl device in opengl context");
        }
    }

    private GlTexture getGlTexture(GpuTexture texture) {
        if (texture instanceof GlTexture glTexture) {
            return glTexture;
        }
        throw new IllegalArgumentException("Vivecraft: not a opengl texture in opengl context");
    }

    @Override
    public RawTexture createTexture(String name, int width, int height, RawTexture.Format format) {
        int prevTexture = GlStateManager._getInteger(GL30C.GL_TEXTURE_BINDING_2D);
        int prevFbo = GlStateManager.getFrameBuffer(GL30C.GL_DRAW_FRAMEBUFFER);
        int textureId = GlStateManager._genTexture();
        GlStateManager._bindTexture(textureId);
        GlStateManager._texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MIN_FILTER, GL30C.GL_LINEAR);
        GlStateManager._texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MAG_FILTER, GL30C.GL_LINEAR);
        GlStateManager._texImage2D(GL30C.GL_TEXTURE_2D, 0,
            getInternalFormat(format), width, height, 0,
            getFormat(format), getType(format), null);

        // create fbo
        int fboId = GlStateManager.glGenFramebuffers();
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, fboId);
        GlStateManager._glFramebufferTexture2D(GL30C.GL_DRAW_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0,
            GL30C.GL_TEXTURE_2D, textureId, 0);

        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, prevFbo);
        GlStateManager._bindTexture(prevTexture);
        checkError(name + " texture creation");

        return new OpenGlRawTexture(name, width, height, format, textureId, fboId);
    }

    @Override
    public void blitTextures(VRTextureTarget[] sources, RawTexture[] targets) {
        if (sources.length != targets.length) {
            throw new RuntimeException("Vivecraft: source and target image count do not match!");
        }
        GlDevice glDevice = getGlDevice();
        for (int i = 0; i < sources.length; i++) {
            GlTexture source = getGlTexture(sources[i].getColorTexture());
            OpenGlRawTexture target = (OpenGlRawTexture) targets[i];

            // no need to copy if they are the same backing texture
            if (sources[i].fixedTexId == target.getHandle()) continue;

            int sourceFbo = source.getFbo(glDevice.directStateAccess(), null);
            if (this.directStateSupport) {
                ARBDirectStateAccess.glBlitNamedFramebuffer(
                    sourceFbo, target.getFbo(),
                    0, 0, source.getWidth(0), source.getHeight(0),
                    0, 0, target.width, target.height,
                    GL30C.GL_COLOR_BUFFER_BIT,
                    // nearest since these are supposed to be the same size anyway
                    GL30C.GL_NEAREST);
            } else {
                int oldRead = GlStateManager.getFrameBuffer(GL30C.GL_READ_FRAMEBUFFER);
                int oldDraw = GlStateManager.getFrameBuffer(GL30C.GL_DRAW_FRAMEBUFFER);
                GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, sourceFbo);
                GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, target.getFbo());
                GlStateManager._glBlitFrameBuffer(
                    0, 0, source.getWidth(0), source.getHeight(0),
                    0, 0, target.width, target.height,
                    GL30C.GL_COLOR_BUFFER_BIT,
                    // nearest since these are supposed to be the same size anyway
                    GL30C.GL_NEAREST);
                GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, oldRead);
                GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, oldDraw);
            }
        }
    }

    @Override
    public RawTexture.Format[] supportedTextureFormats() {
        if (this.supportedFormats == null) {
            if (this.formatQuerySupport) {
                // actually check if formats are supported
                List<RawTexture.Format> supportedFormatsList = new ArrayList<>();
                for (RawTexture.Format format : RawTexture.Format.values()) {
                    if (ARBInternalformatQuery.glGetInternalformati(GL30C.GL_TEXTURE_2D, getInternalFormat(format),
                        GL43C.GL_INTERNALFORMAT_SUPPORTED) == GL30C.GL_TRUE)
                    {
                        supportedFormatsList.add(format);
                    }
                }
                if (supportedFormatsList.isEmpty()) {
                    throw new RuntimeException("Vivecraft: none of the required GPU Texture Foramts is supported");
                }
                this.supportedFormats = supportedFormatsList.toArray(new RawTexture.Format[0]);
            } else {
                // can only assume the required ones are supported
                this.supportedFormats = new RawTexture.Format[]{
                    RawTexture.Format.R8G8B8A8_UNORM,
                    RawTexture.Format.R8G8B8A8_SRGB,
                    RawTexture.Format.R16G16B16A16_SFLOAT,
                    RawTexture.Format.R32G32B32A32_SFLOAT
                };
            }
        }
        return this.supportedFormats;
    }

    @Override
    public long getTextureHandle(GpuTexture texture) {
        if (texture instanceof GlTexture glTexture) {
            return glTexture.glId();
        }
        throw new IllegalArgumentException("Vivecraft: not an opengl texture in opengl context");
    }

    public static void bindTexture(int slot, GpuTextureView texture) {
        if (texture instanceof GlTextureView glTextureView) {
            GlStateManager._activeTexture(GL30C.GL_TEXTURE0 + slot);
            GlStateManager._bindTexture(glTextureView.texture().glId());
        } else {
            throw new IllegalStateException("Vivecraft: only opengl textures are supported");
        }
    }

    @Override
    public void genMipmaps(GpuTexture texture) {
        if (texture instanceof GlTexture glTexture) {
            int textureUnit = GlStateManager._getInteger(GL30C.GL_ACTIVE_TEXTURE);
            int boundTexture = GlStateManager._getInteger(GL30C.GL_TEXTURE_BINDING_2D);

            GlStateManager._activeTexture(GL30C.GL_TEXTURE0);
            GlStateManager._bindTexture(glTexture.glId());

            GL30C.glGenerateMipmap(GL30C.GL_TEXTURE_2D);

            GlStateManager._activeTexture(textureUnit);
            GlStateManager._bindTexture(boundTexture);
        } else {
            throw new IllegalStateException("Vivecraft: only opengl textures are supported");
        }
    }

    private final Map<String, Pair<Integer, Integer>> glErrors = new HashMap<>();

    /**
     * checks if there were any opengl errors since this was last called
     *
     * @param errorSection name of the section that is checked, this gets logged if there are any errors
     * @return error string if there was one
     */
    @Override
    public String checkError(String errorSection) {
        int error = GlStateManager._getError();
        int count = 0;
        Pair<Integer, Integer> oldError = this.glErrors.get(errorSection);
        if (error != 0 && oldError != null && oldError.getLeft() == error) {
            count = oldError.getRight() + 1;
        }
        this.glErrors.put(errorSection, Pair.of(error, count));
        if (error != 0 && count < 5) {
            String errorString = switch (error) {
                case GL30C.GL_INVALID_ENUM -> "invalid enum";
                case GL30C.GL_INVALID_VALUE -> "invalid value";
                case GL30C.GL_INVALID_OPERATION -> "invalid operation";
                case GL30C.GL_STACK_OVERFLOW -> "stack overflow";
                case GL30C.GL_STACK_UNDERFLOW -> "stack underflow";
                case GL30C.GL_OUT_OF_MEMORY -> "out of memory";
                case GL30C.GL_INVALID_FRAMEBUFFER_OPERATION -> "framebuffer is not complete";
                default -> "unknown error";
            };
            VRSettings.LOGGER.error("Vivecraft: ########## GL ERROR ##########");
            VRSettings.LOGGER.error("Vivecraft: @ {}", errorSection);
            VRSettings.LOGGER.error("Vivecraft: {}: {}", error, errorString);
            return errorString;
        } else if (count == 5) {
            VRSettings.LOGGER.error("Vivecraft: repeated gl errors for {}, not logging anymore", errorSection);
        }
        return "";
    }

    @Override
    public boolean isStencil() {
        return GL30C.glIsEnabled(GL30C.GL_STENCIL_TEST);
    }

    @Override
    public void setStencil(boolean state) {
        if (state) {
            GL30C.glEnable(GL30C.GL_STENCIL_TEST);
        } else {
            GL30C.glDisable(GL30C.GL_STENCIL_TEST);
        }
    }

    @Override
    public void flush() {
        GL30C.glFlush();
    }

    public static int getInternalFormat(RawTexture.Format format) {
        return switch (format) {
            case R8G8B8A8_UNORM, B8G8R8A8_UNORM -> GL30C.GL_RGBA8;
            case R8G8B8A8_SRGB, B8G8R8A8_SRGB -> GL30C.GL_SRGB8_ALPHA8;
            case R16G16B16A16_SFLOAT -> GL30C.GL_RGBA16F;
            case R32G32B32_SFLOAT -> GL30C.GL_RGB32F;
            case R32G32B32A32_SFLOAT -> GL30C.GL_RGBA32F;
        };
    }

    public static int getFormat(RawTexture.Format format) {
        return switch (format) {
            case R8G8B8A8_UNORM, R8G8B8A8_SRGB, R16G16B16A16_SFLOAT, R32G32B32A32_SFLOAT -> GL30C.GL_RGBA;
            case B8G8R8A8_UNORM, B8G8R8A8_SRGB -> GL30C.GL_BGRA;
            case R32G32B32_SFLOAT -> GL30C.GL_RGB;
        };
    }

    public static int getType(RawTexture.Format format) {
        return switch (format) {
            case R8G8B8A8_UNORM, B8G8R8A8_UNORM, R8G8B8A8_SRGB, B8G8R8A8_SRGB -> GL30C.GL_UNSIGNED_BYTE;
            case R16G16B16A16_SFLOAT -> GL30C.GL_HALF_FLOAT;
            case R32G32B32_SFLOAT, R32G32B32A32_SFLOAT -> GL30C.GL_FLOAT;
        };
    }
}
