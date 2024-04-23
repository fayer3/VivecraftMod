package org.vivecraft.client_vr.provider;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlUtil;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43;
import org.vivecraft.client.extensions.RenderTargetExtension;
import org.vivecraft.client.utils.Utils;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.VRTextureTarget;
import org.vivecraft.client_vr.extensions.GameRendererExtension;
import org.vivecraft.client_vr.extensions.WindowExtension;
import org.vivecraft.client_vr.gameplay.screenhandlers.GuiHandler;
import org.vivecraft.client_vr.gameplay.screenhandlers.KeyboardHandler;
import org.vivecraft.client_vr.gameplay.screenhandlers.RadialHandler;
import org.vivecraft.client_vr.gameplay.trackers.TelescopeTracker;
import org.vivecraft.client_vr.render.RenderConfigException;
import org.vivecraft.client_vr.render.RenderPass;
import org.vivecraft.client_vr.render.ShaderHelper;
import org.vivecraft.client_vr.render.VRShaders;
import org.vivecraft.client_vr.settings.VRSettings;
import org.vivecraft.client_xr.render_pass.WorldRenderPass;
import org.vivecraft.mod_compat_vr.ShadersHelper;
import org.vivecraft.mod_compat_vr.resolutioncontrol.ResolutionControlHelper;
import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class VRRenderer {
    public static final String RENDER_SETUP_FAILURE_MESSAGE = "Failed to initialise stereo rendering plugin: ";

    // projection matrices
    public Matrix4f[] eyeproj = new Matrix4f[2];

    // render buffers
    public RenderTarget framebufferEye0;
    public RenderTarget framebufferEye1;
    protected int LeftEyeTextureId = -1;
    protected int RightEyeTextureId = -1;
    public RenderTarget framebufferMR;
    public RenderTarget framebufferUndistorted;
    public RenderTarget framebufferVrRender;
    public RenderTarget fsaaFirstPassResultFBO;
    public RenderTarget fsaaLastPassResultFBO;
    public RenderTarget cameraFramebuffer;
    public RenderTarget cameraRenderFramebuffer;
    public RenderTarget telescopeFramebufferL;
    public RenderTarget telescopeFramebufferR;

    // Stencil mesh buffer for each eye
    protected float[][] hiddenMesheVertecies = new float[2][];

    // variables to check setting changes that need frambuffers reinits/resizes
    private GraphicsStatus previousGraphics = null;
    protected VRSettings.MirrorMode lastMirror;
    public long lastWindow = 0L;
    public int mirrorFBHeight;
    public int mirrorFBWidth;
    protected boolean reinitFramebuffers = true;
    protected boolean resizeFrameBuffers = false;
    protected boolean acceptReinits = true;
    public float renderScale;
    // render resolution set by the VR runtime, includes the supersampling factor
    protected Tuple<Integer, Integer> resolution;

    // supersampling set by the vr runtime
    public float ss = -1.0F;
    protected MCVR vr;

    public VRRenderer(MCVR vr) {
        this.vr = vr;
    }

    public abstract void createRenderTexture(int var1, int var2);

    public abstract Matrix4f getProjectionMatrix(int var1, float var2, float var3);

    public abstract void endFrame() throws RenderConfigException;

    public abstract boolean providesStencilMask();

    /**
     * @param eye which eye the stencil should be for
     * @return the stencil for that eye, if available
     */
    public float[] getStencilMask(RenderPass eye) {
        if (this.hiddenMesheVertecies != null && (eye == RenderPass.LEFT || eye == RenderPass.RIGHT)) {
            return eye == RenderPass.LEFT ? this.hiddenMesheVertecies[0] : this.hiddenMesheVertecies[1];
        } else {
            return null;
        }
    }

    /**
     * sets up the stencil rendering, and draws the stencil
     * @param inverse if the stencil covered part, or the inverse of it should be drawn
     */
    public void doStencil(boolean inverse) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientDataHolderVR dataholder = ClientDataHolderVR.getInstance();

        //setup stencil for writing
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        RenderSystem.stencilMask(0xFF); // Write to stencil buffer

        if (inverse) {
            //clear whole image for total mask in color, stencil, depth
            RenderSystem.clearStencil(0xFF);
            RenderSystem.clearDepth(0);

            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF); // Set any stencil to 0
            RenderSystem.colorMask(false, false, false, true);
        } else {
            //clear whole image for total transparency
            RenderSystem.clearStencil(0);
            RenderSystem.clearDepth(1);

            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0xFF, 0xFF); // Set any stencil to 1
            RenderSystem.colorMask(true, true, true, true);
        }

        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT, false);

        RenderSystem.clearStencil(0);
        RenderSystem.clearDepth(1);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        RenderSystem.disableCull();

        RenderSystem.setShaderColor(0F, 0F, 0F, 1.0F);

        RenderTarget fb = minecraft.getMainRenderTarget();
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, fb.viewWidth, 0.0F, fb.viewHeight, 0.0F, 20.0F), VertexSorting.ORTHOGRAPHIC_Z);
        RenderSystem.getModelViewStack().pushPose();
        RenderSystem.getModelViewStack().setIdentity();
        if (inverse) {
            //draw on far clip
            RenderSystem.getModelViewStack().translate(0, 0, -20);
        }
        RenderSystem.applyModelViewMatrix();
        int program = GlStateManager._getInteger(GL43.GL_CURRENT_PROGRAM);

        if (dataholder.currentPass == RenderPass.SCOPEL || dataholder.currentPass == RenderPass.SCOPER) {
            drawCircle(fb.viewWidth, fb.viewHeight);
        } else if (dataholder.currentPass == RenderPass.LEFT || dataholder.currentPass == RenderPass.RIGHT) {
            drawMask();
        }

        RenderSystem.restoreProjectionMatrix();
        RenderSystem.getModelViewStack().popPose();

        RenderSystem.depthMask(true); // Do write to depth buffer
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableDepthTest();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableCull();
        ProgramManager.glUseProgram(program);
        RenderSystem.stencilFunc(GL11.GL_NOTEQUAL, 255, 1);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.stencilMask(0); // Dont Write to stencil buffer
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
    }

    /**
     * triangulates a circle and draws it
     */
    private void drawCircle(float width, float height) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        final float edges = 32.0F;
        float radius = width / 2.0F;

        // put middle vertex
        builder.vertex(radius, radius, 0.0F).endVertex();

        // put outer vertices
        for (int i = 0; i < edges + 1; i++) {
            float startAngle = (float) i / edges * (float) Math.PI * 2.0F;
            builder.vertex(
                radius + (float) Math.cos(startAngle) * radius,
                radius + (float) Math.sin(startAngle) * radius,
                0.0F).endVertex();
        }
        BufferUploader.drawWithShader(builder.end());
    }

    /**
     * draws the stencil provided by the vr runtime
     */
    private void drawMask() {
        Minecraft mc = Minecraft.getInstance();
        ClientDataHolderVR dh = ClientDataHolderVR.getInstance();
        float[] verts = getStencilMask(dh.currentPass);
        if (verts == null) {
            return;
        }

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);

        mc.getTextureManager().bindForSetup(new ResourceLocation("vivecraft:textures/black.png"));

        for (int i = 0; i < verts.length; i += 2) {
            builder.vertex(
                verts[i] * dh.vrRenderer.renderScale,
                verts[i + 1] * dh.vrRenderer.renderScale,
                0.0F).endVertex();
        }

        RenderSystem.setShader(GameRenderer::getPositionShader);
        BufferUploader.drawWithShader(builder.end());
    }

    /**
     * does a FSAA pass on the final image
     */
    public void doFSAA() {
        if (this.fsaaFirstPassResultFBO == null) {
            this.reinitFrameBuffers("FSAA Setting Changed");
        } else {
            RenderSystem.disableBlend();
            // set to always, so that we can skip the clear
            RenderSystem.depthFunc(GL43.GL_ALWAYS);

            // first pass, horizontal
            this.fsaaFirstPassResultFBO.bindWrite(true);

            RenderSystem.setShaderTexture(0, this.framebufferVrRender.getColorTextureId());
            RenderSystem.setShaderTexture(1, this.framebufferVrRender.getDepthTextureId());

            RenderSystem.activeTexture(GL43.GL_TEXTURE1);
            this.framebufferVrRender.bindRead();
            RenderSystem.activeTexture(GL43.GL_TEXTURE2);
            RenderSystem.bindTexture(this.framebufferVrRender.getDepthTextureId());
            RenderSystem.activeTexture(GL43.GL_TEXTURE0);

            VRShaders.lanczosShader.setSampler("Sampler0", RenderSystem.getShaderTexture(0));
            VRShaders.lanczosShader.setSampler("Sampler1", RenderSystem.getShaderTexture(1));
            VRShaders._Lanczos_texelWidthOffsetUniform.set(1.0F / (3.0F * (float) this.fsaaFirstPassResultFBO.viewWidth));
            VRShaders._Lanczos_texelHeightOffsetUniform.set(0.0F);
            VRShaders.lanczosShader.apply();

            this.drawQuad();

            // second pass, vertical
            this.fsaaLastPassResultFBO.bindWrite(true);
            RenderSystem.setShaderTexture(0, this.fsaaFirstPassResultFBO.getColorTextureId());
            RenderSystem.setShaderTexture(1, this.fsaaFirstPassResultFBO.getDepthTextureId());

            RenderSystem.activeTexture(GL43.GL_TEXTURE1);
            this.fsaaFirstPassResultFBO.bindRead();
            RenderSystem.activeTexture(GL43.GL_TEXTURE2);
            RenderSystem.bindTexture(this.fsaaFirstPassResultFBO.getDepthTextureId());
            RenderSystem.activeTexture(GL43.GL_TEXTURE0);

            VRShaders.lanczosShader.setSampler("Sampler0", RenderSystem.getShaderTexture(0));
            VRShaders.lanczosShader.setSampler("Sampler1", RenderSystem.getShaderTexture(1));
            VRShaders._Lanczos_texelWidthOffsetUniform.set(0.0F);
            VRShaders._Lanczos_texelHeightOffsetUniform.set(1.0F / (3.0F * (float) this.fsaaLastPassResultFBO.viewHeight));
            VRShaders.lanczosShader.apply();

            this.drawQuad();

            // Clean up time
            VRShaders.lanczosShader.clear();
            this.fsaaLastPassResultFBO.unbindWrite();
            RenderSystem.depthFunc(GL43.GL_LEQUAL);
            RenderSystem.enableBlend();
        }
    }

    private void drawQuad() {
        //RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(-1.0F, -1.0F, 0.0F).uv(0.0F, 0.0F).endVertex();
        builder.vertex(1.0F, -1.0F, 0.0F).uv(1.0F, 0.0F).endVertex();
        builder.vertex(1.0F, 1.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
        builder.vertex(-1.0F, 1.0F, 0.0F).uv(0.0F, 1.0F).endVertex();
        BufferUploader.draw(builder.end());
    }

    public double getCurrentTimeSecs() {
        return (double) System.nanoTime() / 1.0E9D;
    }

    public double getFrameTiming() {
        return this.getCurrentTimeSecs();
    }

    public String getinitError() {
        return this.vr.initStatus;
    }

    public String getLastError() {
        return "";
    }

    public String getName() {
        return "OpenVR";
    }

    /**
     * @return a list of passes that need to be rendered
     */
    public List<RenderPass> getRenderPasses() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientDataHolderVR dataholder = ClientDataHolderVR.getInstance();
        List<RenderPass> passes = new ArrayList<>();

        // Always do these for obvious reasons
        passes.add(RenderPass.LEFT);
        passes.add(RenderPass.RIGHT);

        // only do these, if the window is not minimized
        if (((WindowExtension) (Object) minecraft.getWindow()).vivecraft$getActualScreenWidth() > 0
            && ((WindowExtension) (Object) minecraft.getWindow()).vivecraft$getActualScreenHeight() > 0) {
            if (dataholder.vrSettings.displayMirrorMode == VRSettings.MirrorMode.FIRST_PERSON) {
                passes.add(RenderPass.CENTER);
            } else if (dataholder.vrSettings.displayMirrorMode == VRSettings.MirrorMode.MIXED_REALITY) {
                if (dataholder.vrSettings.mixedRealityUndistorted && dataholder.vrSettings.mixedRealityUnityLike) {
                    passes.add(RenderPass.CENTER);
                }

                passes.add(RenderPass.THIRD);
            } else if (dataholder.vrSettings.displayMirrorMode == VRSettings.MirrorMode.THIRD_PERSON) {
                passes.add(RenderPass.THIRD);
            }
        }

        if (minecraft.player != null) {
            if (TelescopeTracker.isTelescope(minecraft.player.getMainHandItem()) && TelescopeTracker.isViewing(0)) {
                passes.add(RenderPass.SCOPER);
            }

            if (TelescopeTracker.isTelescope(minecraft.player.getOffhandItem()) && TelescopeTracker.isViewing(1)) {
                passes.add(RenderPass.SCOPEL);
            }

            if (dataholder.cameraTracker.isVisible()) {
                passes.add(RenderPass.CAMERA);
            }
        }

        return passes;
    }

    /**
     * @return resolution of the headset view
     */
    public abstract Tuple<Integer, Integer> getRenderTextureSizes();

    /**
     * @param eyeFBWidth headset view width
     * @param eyeFBHeight headset view height
     * @param resolutionScale render scale from 3rd party mods
     * @return resolution of the desktop view mirror
     */
    public Tuple<Integer, Integer> getMirrorTextureSize(int eyeFBWidth, int eyeFBHeight, float resolutionScale) {
        this.mirrorFBWidth = (int) Math.ceil(((WindowExtension) (Object) Minecraft.getInstance().getWindow()).vivecraft$getActualScreenWidth() * resolutionScale);
        this.mirrorFBHeight = (int) Math.ceil(((WindowExtension) (Object) Minecraft.getInstance().getWindow()).vivecraft$getActualScreenHeight() * resolutionScale);

        if (ClientDataHolderVR.getInstance().vrSettings.displayMirrorMode == VRSettings.MirrorMode.MIXED_REALITY) {
            this.mirrorFBWidth = this.mirrorFBWidth / 2;

            if (ClientDataHolderVR.getInstance().vrSettings.mixedRealityUnityLike) {
                this.mirrorFBHeight = this.mirrorFBHeight / 2;
            }
        }

        if (ShadersHelper.needsSameSizeBuffers()) {
            this.mirrorFBWidth = eyeFBWidth;
            this.mirrorFBHeight = eyeFBHeight;
        }
        return new Tuple<>(this.mirrorFBWidth, this.mirrorFBHeight);
    }

    /**
     * @param eyeFBWidth headset view width
     * @param eyeFBHeight headset view height
     * @return resolution of the telescope view
     */
    public Tuple<Integer, Integer> getTelescopeTextureSize(int eyeFBWidth, int eyeFBHeight) {
        int telescopeFBwidth = 720;
        int telescopeFBheight = 720;

        if (ShadersHelper.needsSameSizeBuffers()) {
            telescopeFBwidth = eyeFBWidth;
            telescopeFBheight = eyeFBHeight;
        }
        return new Tuple<>(telescopeFBwidth, telescopeFBheight);
    }

    /**
     * @param eyeFBWidth headset view width
     * @param eyeFBHeight headset view height
     * @return resolution of the screenshot camera view
     */
    public Tuple<Integer, Integer> getCameraTextureSize(int eyeFBWidth, int eyeFBHeight) {
        int cameraFBwidth = Math.round(1920.0F * ClientDataHolderVR.getInstance().vrSettings.handCameraResScale);
        int cameraFBheight = Math.round(1080.0F * ClientDataHolderVR.getInstance().vrSettings.handCameraResScale);

        if (ShadersHelper.needsSameSizeBuffers()) {
            // correct for camera aspect, since that is 16:9
            float aspect = (float) cameraFBwidth / (float) cameraFBheight;
            if (aspect > (float) (eyeFBWidth / eyeFBHeight)) {
                cameraFBwidth = eyeFBWidth;
                cameraFBheight = Math.round((float) eyeFBWidth / aspect);
            } else {
                cameraFBwidth = Math.round((float) eyeFBHeight * aspect);
                cameraFBheight = eyeFBHeight;
            }
        }
        return new Tuple<>(cameraFBwidth, cameraFBheight);
    }

    public boolean isInitialized() {
        return this.vr.initSuccess;
    }

    /**
     * method to tell the vrRenderer, that render buffers changed and need to be regenerated next frame
     * @param cause cause that gets logged
     */
    public void reinitFrameBuffers(String cause) {
        if (this.acceptReinits) {
            if (!this.reinitFramebuffers) {
                // only print the first cause
                VRSettings.logger.info("Reinit Render: {}", cause);
            }
            this.reinitFramebuffers = true;
        }
    }

    /**
     * method to tell the vrRenderer, that render buffers size changed and just need to be resized next frame
     * @param cause cause that gets logged
     */
    public void resizeFrameBuffers(String cause) {
        if (!cause.isEmpty() && !this.resizeFrameBuffers) {
            VRSettings.logger.info("Resizing Buffers: {}", cause);
        }
        this.resizeFrameBuffers = true;
    }

    /**
     * sets up rendering, and makes sure all buffers are generated and sized correctly
     * @throws RenderConfigException in case something failed to initialize or the gpu vendor is unsupported
     * @throws IOException can be thrown by the WorldRenderPass init when trying to load the shaders
     */
    public void setupRenderConfiguration() throws RenderConfigException, IOException {
        Minecraft minecraft = Minecraft.getInstance();
        ClientDataHolderVR dataholder = ClientDataHolderVR.getInstance();

        // check if window is still the same
        if (minecraft.getWindow().getWindow() != this.lastWindow) {
            this.lastWindow = minecraft.getWindow().getWindow();
            this.reinitFrameBuffers("Window Handle Changed");
        }

        if (this.lastMirror != dataholder.vrSettings.displayMirrorMode) {
            if (!ShadersHelper.isShaderActive()) {
                // don't reinit with shaders, not needed
                this.reinitFrameBuffers("Mirror Changed");
            } else {
                // mixed reality is half size, so a resize is needed
                if (this.lastMirror == VRSettings.MirrorMode.MIXED_REALITY
                    || dataholder.vrSettings.displayMirrorMode == VRSettings.MirrorMode.MIXED_REALITY) {
                    this.resizeFrameBuffers("Mirror Changed");
                }
            }
            this.lastMirror = dataholder.vrSettings.displayMirrorMode;
        }

        if ((this.framebufferMR == null || this.framebufferUndistorted == null) && ShadersHelper.isShaderActive()) {
            this.reinitFrameBuffers("Shaders on, but some buffers not initialized");
        }
        if (Minecraft.getInstance().options.graphicsMode().get() != this.previousGraphics) {
            this.previousGraphics = Minecraft.getInstance().options.graphicsMode().get();
            ClientDataHolderVR.getInstance().vrRenderer.reinitFrameBuffers("gfx setting change");
        }

        if (this.resizeFrameBuffers && !this.reinitFramebuffers) {
            this.resizeFrameBuffers = false;
            Tuple<Integer, Integer> tuple = this.getRenderTextureSizes();
            int eyew = tuple.getA();
            int eyeh = tuple.getB();

            float resolutionScale = ResolutionControlHelper.isLoaded() ? ResolutionControlHelper.getCurrentScaleFactor() : 1.0F;

            this.renderScale = (float) Math.sqrt(dataholder.vrSettings.renderScaleFactor) * resolutionScale;
            int eyeFBWidth = (int) Math.ceil(eyew * this.renderScale);
            int eyeFBHeight = (int) Math.ceil(eyeh * this.renderScale);

            Tuple<Integer, Integer> mirrorSize = getMirrorTextureSize(eyeFBWidth, eyeFBHeight, resolutionScale);
            Tuple<Integer, Integer> telescopeSize = getTelescopeTextureSize(eyeFBWidth, eyeFBHeight);
            Tuple<Integer, Integer> cameraSize = getCameraTextureSize(eyeFBWidth, eyeFBHeight);

            // main render target
            ((RenderTargetExtension) WorldRenderPass.stereoXR.target).vivecraft$setUseStencil(dataholder.vrSettings.vrUseStencil);
            WorldRenderPass.stereoXR.resize(eyeFBWidth, eyeFBHeight);
            if (dataholder.vrSettings.useFsaa) {
                this.fsaaFirstPassResultFBO.resize(eyew, eyeFBHeight, Minecraft.ON_OSX);
            }

            // mirror
            if (mirrorSize.getA() > 0 && mirrorSize.getB() > 0) {
                if (WorldRenderPass.center != null) {
                    WorldRenderPass.center.resize(mirrorSize.getA(), mirrorSize.getB());
                }
                if (WorldRenderPass.mixedReality != null) {
                    WorldRenderPass.mixedReality.resize(mirrorSize.getA(), mirrorSize.getB());
                }
            }

            // telescopes
            WorldRenderPass.leftTelescope.resize(telescopeSize.getA(), telescopeSize.getB());
            WorldRenderPass.rightTelescope.resize(telescopeSize.getA(), telescopeSize.getB());

            // camera
            this.cameraFramebuffer.resize(cameraSize.getA(), cameraSize.getB(), Minecraft.ON_OSX);
            if (ShadersHelper.needsSameSizeBuffers()) {
                WorldRenderPass.camera.resize(eyeFBWidth, eyeFBHeight);
            } else {
                WorldRenderPass.camera.resize(cameraSize.getA(), cameraSize.getB());
            }

            // resize gui, if changed
            if (GuiHandler.updateResolution()) {
                GuiHandler.guiFramebuffer.resize(GuiHandler.guiWidth, GuiHandler.guiHeight, Minecraft.ON_OSX);
                if (minecraft.screen != null) {
                    int l2 = minecraft.getWindow().getGuiScaledWidth();
                    int j3 = minecraft.getWindow().getGuiScaledHeight();
                    minecraft.screen.init(minecraft, l2, j3);
                }
            }
        }

        if (this.reinitFramebuffers) {
            ShaderHelper.checkGLError("Start Init");

            // intel drivers have issues with opengl interop on windows so throw an error
            if (Util.getPlatform() == Util.OS.WINDOWS && GlUtil.getRenderer().toLowerCase().contains("intel")) {
                StringBuilder gpus = new StringBuilder();
                boolean onlyIntel = true;
                for (GraphicsCard gpu : (new SystemInfo()).getHardware().getGraphicsCards()) {
                    gpus.append("\n");
                    if (gpu.getVendor().toLowerCase().contains("intel") || gpu.getName().toLowerCase().contains("intel")) {
                        gpus.append("§c❌§r ");
                    } else {
                        onlyIntel = false;
                        gpus.append("§a✔§r ");
                    }
                    gpus.append(gpu.getVendor()).append(": ").append(gpu.getName());
                }
                throw new RenderConfigException("Incompatible", Component.translatable(
                    "vivecraft.messages.intelgraphics1",
                    Component.literal(GlUtil.getRenderer()).withStyle(ChatFormatting.GOLD),
                    gpus.toString(),
                    onlyIntel ? Component.empty()
                              : Component.translatable("vivecraft.messages.intelgraphics2", Component.literal("https://www.vivecraft.org/faq/#gpu")
                                  .withStyle(style -> style.withUnderlined(true)
                                      .withColor(ChatFormatting.GREEN)
                                      .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, CommonComponents.GUI_OPEN_IN_BROWSER))
                                      .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.vivecraft.org/faq/#gpu"))))));
            }

            if (!this.isInitialized()) {
                throw new RenderConfigException(RENDER_SETUP_FAILURE_MESSAGE + this.getName(), Component.literal(this.getinitError()));
            }

            Tuple<Integer, Integer> tuple = this.getRenderTextureSizes();
            int eyew = tuple.getA();
            int eyeh = tuple.getB();

            destroyBuffers();

            if (this.LeftEyeTextureId == -1) {
                this.createRenderTexture(eyew, eyeh);

                if (this.LeftEyeTextureId == -1) {
                    throw new RenderConfigException(RENDER_SETUP_FAILURE_MESSAGE + this.getName(), Component.literal(this.getLastError()));
                }

                VRSettings.logger.info("VR Provider supplied render texture IDs: {}, {}", this.LeftEyeTextureId, this.RightEyeTextureId);
                VRSettings.logger.info("VR Provider supplied texture resolution: {} x {}", eyew, eyeh);
            }

            ShaderHelper.checkGLError("Render Texture setup");

            if (this.framebufferEye0 == null) {
                this.framebufferEye0 = new VRTextureTarget("L Eye", eyew, eyeh, false, false, this.LeftEyeTextureId, false, true, false);
                VRSettings.logger.info(this.framebufferEye0.toString());
                ShaderHelper.checkGLError("Left Eye framebuffer setup");
            }

            if (this.framebufferEye1 == null) {
                this.framebufferEye1 = new VRTextureTarget("R Eye", eyew, eyeh, false, false, this.RightEyeTextureId, false, true, false);
                VRSettings.logger.info(this.framebufferEye1.toString());
                ShaderHelper.checkGLError("Right Eye framebuffer setup");
            }

            float resolutionScale = ResolutionControlHelper.isLoaded() ? ResolutionControlHelper.getCurrentScaleFactor() : 1.0F;

            this.renderScale = (float) Math.sqrt(dataholder.vrSettings.renderScaleFactor) * resolutionScale;
            int eyeFBWidth = (int) Math.ceil(eyew * this.renderScale);
            int eyeFBHeight = (int) Math.ceil(eyeh * this.renderScale);

            this.framebufferVrRender = new VRTextureTarget("3D Render", eyeFBWidth, eyeFBHeight, true, false, -1, true, true, dataholder.vrSettings.vrUseStencil);
            WorldRenderPass.stereoXR = new WorldRenderPass((VRTextureTarget) this.framebufferVrRender);
            VRSettings.logger.info(this.framebufferVrRender.toString());
            ShaderHelper.checkGLError("3D framebuffer setup");

            getMirrorTextureSize(eyeFBWidth, eyeFBHeight, resolutionScale);

            List<RenderPass> list = this.getRenderPasses();

            VRSettings.logger.info("Active RenderPasses: {}", list.stream().map(Enum::toString).collect(Collectors.joining(", ")));

            // only do these, if the window is not minimized
            if (this.mirrorFBWidth > 0 && this.mirrorFBHeight > 0) {
                if (list.contains(RenderPass.THIRD) || ShadersHelper.isShaderActive()) {
                    this.framebufferMR = new VRTextureTarget("Mixed Reality Render", this.mirrorFBWidth, this.mirrorFBHeight, true, false, -1, true, false, false);
                    WorldRenderPass.mixedReality = new WorldRenderPass((VRTextureTarget) this.framebufferMR);
                    VRSettings.logger.info(this.framebufferMR.toString());
                    ShaderHelper.checkGLError("Mixed reality framebuffer setup");
                }

                if (list.contains(RenderPass.CENTER) || ShadersHelper.isShaderActive()) {
                    this.framebufferUndistorted = new VRTextureTarget("Undistorted View Render", this.mirrorFBWidth, this.mirrorFBHeight, true, false, -1, false, false, false);
                    WorldRenderPass.center = new WorldRenderPass((VRTextureTarget) this.framebufferUndistorted);
                    VRSettings.logger.info(this.framebufferUndistorted.toString());
                    ShaderHelper.checkGLError("Undistorted view framebuffer setup");
                }
            }

            GuiHandler.updateResolution();
            GuiHandler.guiFramebuffer = new VRTextureTarget("GUI", GuiHandler.guiWidth, GuiHandler.guiHeight, true, false, -1, false, true, false);
            VRSettings.logger.info(GuiHandler.guiFramebuffer.toString());
            ShaderHelper.checkGLError("GUI framebuffer setup");

            KeyboardHandler.Framebuffer = new VRTextureTarget("Keyboard", GuiHandler.guiWidth, GuiHandler.guiHeight, true, false, -1, false, true, false);
            VRSettings.logger.info(KeyboardHandler.Framebuffer.toString());
            ShaderHelper.checkGLError("Keyboard framebuffer setup");

            RadialHandler.Framebuffer = new VRTextureTarget("Radial Menu", GuiHandler.guiWidth, GuiHandler.guiHeight, true, false, -1, false, true, false);
            VRSettings.logger.info(RadialHandler.Framebuffer.toString());
            ShaderHelper.checkGLError("Radial framebuffer setup");


            Tuple<Integer, Integer> telescopeSize = getTelescopeTextureSize(eyeFBWidth, eyeFBHeight);

            this.telescopeFramebufferR = new VRTextureTarget("TelescopeR", telescopeSize.getA(), telescopeSize.getB(), true, false, -1, true, false, false);
            WorldRenderPass.rightTelescope = new WorldRenderPass((VRTextureTarget) this.telescopeFramebufferR);
            VRSettings.logger.info(this.telescopeFramebufferR.toString());
            this.telescopeFramebufferR.setClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            this.telescopeFramebufferR.clear(Minecraft.ON_OSX);
            ShaderHelper.checkGLError("TelescopeR framebuffer setup");

            this.telescopeFramebufferL = new VRTextureTarget("TelescopeL", telescopeSize.getA(), telescopeSize.getB(), true, false, -1, true, false, false);
            WorldRenderPass.leftTelescope = new WorldRenderPass((VRTextureTarget) this.telescopeFramebufferL);
            VRSettings.logger.info(this.telescopeFramebufferL.toString());
            this.telescopeFramebufferL.setClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            this.telescopeFramebufferL.clear(Minecraft.ON_OSX);
            ShaderHelper.checkGLError("TelescopeL framebuffer setup");


            Tuple<Integer, Integer> cameraSize = getCameraTextureSize(eyeFBWidth, eyeFBHeight);
            int cameraRenderFBwidth = cameraSize.getA();
            int cameraRenderFBheight = cameraSize.getB();

            if (ShadersHelper.needsSameSizeBuffers()) {
                cameraRenderFBwidth = eyeFBWidth;
                cameraRenderFBheight = eyeFBHeight;
            }

            this.cameraFramebuffer = new VRTextureTarget("Handheld Camera", cameraSize.getA(), cameraSize.getB(), true, false, -1, true, false, false);
            VRSettings.logger.info(this.cameraFramebuffer.toString());

            ShaderHelper.checkGLError("Camera framebuffer setup");
            this.cameraRenderFramebuffer = new VRTextureTarget("Handheld Camera Render", cameraRenderFBwidth, cameraRenderFBheight, true, false, -1, true, true, false);
            WorldRenderPass.camera = new WorldRenderPass((VRTextureTarget) this.cameraRenderFramebuffer);
            VRSettings.logger.info(this.cameraRenderFramebuffer.toString());
            ShaderHelper.checkGLError("Camera render framebuffer setup");

            ((GameRendererExtension) minecraft.gameRenderer).vivecraft$setupClipPlanes();
            this.eyeproj[0] = this.getProjectionMatrix(0, ((GameRendererExtension) minecraft.gameRenderer).vivecraft$getMinClipDistance(), ((GameRendererExtension) minecraft.gameRenderer).vivecraft$getClipDistance());
            this.eyeproj[1] = this.getProjectionMatrix(1, ((GameRendererExtension) minecraft.gameRenderer).vivecraft$getMinClipDistance(), ((GameRendererExtension) minecraft.gameRenderer).vivecraft$getClipDistance());

            if (dataholder.vrSettings.useFsaa) {
                try {
                    ShaderHelper.checkGLError("pre FSAA FBO creation");
                    this.fsaaFirstPassResultFBO = new VRTextureTarget("FSAA Pass1 FBO", eyew, eyeFBHeight, true, false, -1, false, false, false);
                    this.fsaaLastPassResultFBO = new VRTextureTarget("FSAA Pass2 FBO", eyew, eyeh, true, false, -1, false, false, false);
                    VRSettings.logger.info(this.fsaaFirstPassResultFBO.toString());
                    VRSettings.logger.info(this.fsaaLastPassResultFBO.toString());
                    ShaderHelper.checkGLError("FSAA FBO creation");

                    VRShaders.setupFSAA();
                    ShaderHelper.checkGLError("FBO init fsaa shader");
                } catch (Exception exception) {
                    // FSAA failed to initialize so don't use it
                    dataholder.vrSettings.useFsaa = false;
                    dataholder.vrSettings.saveOptions();
                    VRSettings.logger.error("FSAA init failed with: {}", exception.getMessage());
                    // redo the setup next frame
                    this.reinitFramebuffers = true;
                    return;
                }
            }

            try {
                minecraft.mainRenderTarget = this.framebufferVrRender;
                VRShaders.setupDepthMask();
                ShaderHelper.checkGLError("init depth shader");
                VRShaders.setupFOVReduction();
                ShaderHelper.checkGLError("init FOV shader");
                VRShaders.setupPortalShaders();
                ShaderHelper.checkGLError("init portal shader");
                minecraft.gameRenderer.checkEntityPostEffect(minecraft.getCameraEntity());
            } catch (Exception exception) {
                VRSettings.logger.error(exception.getMessage());
                throw new RenderConfigException(RENDER_SETUP_FAILURE_MESSAGE + this.getName(),
                    Utils.throwableToComponent(exception));
            }

            if (minecraft.screen != null) {
                int w = minecraft.getWindow().getGuiScaledWidth();
                int h = minecraft.getWindow().getGuiScaledHeight();
                minecraft.screen.init(minecraft, w, h);
            }

            long windowPixels = (long) ((WindowExtension) (Object) minecraft.getWindow()).vivecraft$getActualScreenWidth() * ((WindowExtension) (Object) minecraft.getWindow()).vivecraft$getActualScreenHeight();
            long mirrorPixels = (long) this.mirrorFBWidth * (long) this.mirrorFBHeight;

            long vrPixels = (long) eyeFBWidth * (long) eyeFBHeight;
            long pixelsPerFrame = vrPixels * 2L;

            if (list.contains(RenderPass.CENTER)) {
                pixelsPerFrame += mirrorPixels;
            }

            if (list.contains(RenderPass.THIRD)) {
                pixelsPerFrame += mirrorPixels;
            }

            VRSettings.logger.info("""

                    New VR render config:
                    VR target: {}x{} [{}MP]
                    Render target: {}x{} [Render scale: {}%, {}MP]
                    Main window: {}x{} [{}MP]
                    Total shaded pixels per frame: {}MP (eye stencil not accounted for)""",
                eyew, eyeh, String.format("%.1f", (eyew * eyeh) / 1000000.0F),
                eyeFBWidth, eyeFBHeight, dataholder.vrSettings.renderScaleFactor * 100.0F,
                String.format("%.1f", vrPixels / 1000000.0F),
                ((WindowExtension) (Object) minecraft.getWindow()).vivecraft$getActualScreenWidth(),
                ((WindowExtension) (Object) minecraft.getWindow()).vivecraft$getActualScreenHeight(),
                String.format("%.1f", windowPixels / 1000000.0F),
                String.format("%.1f", pixelsPerFrame / 1000000.0F));

            this.reinitFramebuffers = false;

            this.acceptReinits = false;
            ShadersHelper.maybeReloadShaders();
            this.acceptReinits = true;
        }
    }

    /**
     * only destroys the render buffers, everything else stays in takt
     */
    public void destroyBuffers() {
        if (this.framebufferVrRender != null) {
            WorldRenderPass.stereoXR.close();
            WorldRenderPass.stereoXR = null;
            this.framebufferVrRender.destroyBuffers();
            this.framebufferVrRender = null;
        }

        if (this.framebufferMR != null) {
            WorldRenderPass.mixedReality.close();
            WorldRenderPass.mixedReality = null;
            this.framebufferMR.destroyBuffers();
            this.framebufferMR = null;
        }

        if (this.framebufferUndistorted != null) {
            WorldRenderPass.center.close();
            WorldRenderPass.center = null;
            this.framebufferUndistorted.destroyBuffers();
            this.framebufferUndistorted = null;
        }

        if (GuiHandler.guiFramebuffer != null) {
            GuiHandler.guiFramebuffer.destroyBuffers();
            GuiHandler.guiFramebuffer = null;
        }

        if (KeyboardHandler.Framebuffer != null) {
            KeyboardHandler.Framebuffer.destroyBuffers();
            KeyboardHandler.Framebuffer = null;
        }

        if (RadialHandler.Framebuffer != null) {
            RadialHandler.Framebuffer.destroyBuffers();
            RadialHandler.Framebuffer = null;
        }

        if (this.telescopeFramebufferL != null) {
            WorldRenderPass.leftTelescope.close();
            WorldRenderPass.leftTelescope = null;
            this.telescopeFramebufferL.destroyBuffers();
            this.telescopeFramebufferL = null;
        }

        if (this.telescopeFramebufferR != null) {
            WorldRenderPass.rightTelescope.close();
            WorldRenderPass.rightTelescope = null;
            this.telescopeFramebufferR.destroyBuffers();
            this.telescopeFramebufferR = null;
        }

        if (this.cameraFramebuffer != null) {
            this.cameraFramebuffer.destroyBuffers();
            this.cameraFramebuffer = null;
        }

        if (this.cameraRenderFramebuffer != null) {
            WorldRenderPass.camera.close();
            WorldRenderPass.camera = null;
            this.cameraRenderFramebuffer.destroyBuffers();
            this.cameraRenderFramebuffer = null;
        }

        if (this.fsaaFirstPassResultFBO != null) {
            this.fsaaFirstPassResultFBO.destroyBuffers();
            this.fsaaFirstPassResultFBO = null;
        }

        if (this.fsaaLastPassResultFBO != null) {
            this.fsaaLastPassResultFBO.destroyBuffers();
            this.fsaaLastPassResultFBO = null;
        }

        if (this.framebufferEye0 != null) {
            this.framebufferEye0.destroyBuffers();
            this.framebufferEye0 = null;
            this.LeftEyeTextureId = -1;
        }

        if (this.framebufferEye1 != null) {
            this.framebufferEye1.destroyBuffers();
            this.framebufferEye1 = null;
            this.RightEyeTextureId = -1;
        }
    }

    /**
     * destroys everything the Renderer has allocated
     */
    public void destroy() {
        destroyBuffers();
    }
}
