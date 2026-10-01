package tennouboshiuzume.mods.FantasyDesire.client.compat;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

/** 保存世界特效实际使用的状态；GL 状态通过原版管理器恢复，保持缓存一致。 */
final class ShaderRenderState {
    final Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
    final Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
    private final Matrix3f inverseView = new Matrix3f(RenderSystem.getInverseViewRotationMatrix());
    private final Matrix4f textureMatrix = new Matrix4f(RenderSystem.getTextureMatrix());
    private final ShaderInstance shader = RenderSystem.getShader();
    private final float[] color = RenderSystem.getShaderColor().clone();
    private final float[] fogColor = RenderSystem.getShaderFogColor().clone();
    private final float fogStart = RenderSystem.getShaderFogStart(), fogEnd = RenderSystem.getShaderFogEnd();
    private final FogShape fogShape = RenderSystem.getShaderFogShape();
    private final float glintAlpha = RenderSystem.getShaderGlintAlpha();
    private final float lineWidth = RenderSystem.getShaderLineWidth();
    private final int[] textures = new int[12];
    private final boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
    private final boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    private final int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
    private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
    private final int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
    private final int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private final int equationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
    private final int equationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
    private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
    private final int cullFace = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE), frontFace = GL11.glGetInteger(GL11.GL_FRONT_FACE);
    private final boolean polygonOffset = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
    private final float offsetFactor = GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_FACTOR);
    private final float offsetUnits = GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_UNITS);
    private final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
    private final int[] scissorBox = new int[4];
    private final boolean[] colorMask = new boolean[4];

    ShaderRenderState() {
        for (int i = 0; i < textures.length; i++) textures[i] = RenderSystem.getShaderTexture(i);
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer values = stack.malloc(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, values);
            for (int i = 0; i < colorMask.length; i++) colorMask[i] = values.get(i) != 0;
        }
    }

    void apply() {
        RenderSystem.setShader(() -> shader);
        RenderSystem.setInverseViewRotationMatrix(inverseView);
        RenderSystem.setTextureMatrix(textureMatrix);
        RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
        RenderSystem.setShaderFogColor(fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
        RenderSystem.setShaderFogStart(fogStart);
        RenderSystem.setShaderFogEnd(fogEnd);
        RenderSystem.setShaderFogShape(fogShape);
        RenderSystem.setShaderGlintAlpha(glintAlpha);
        RenderSystem.lineWidth(lineWidth);
        for (int i = 0; i < textures.length; i++) RenderSystem.setShaderTexture(i, textures[i]);
        if (depthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
        RenderSystem.depthMask(depthWrite);
        RenderSystem.depthFunc(depthFunc);
        if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
        RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        // 原版管理器只缓存同一 RGB/alpha 方程；分离方程在最后直接恢复。
        RenderSystem.blendEquation(equationRgb);
        GL20.glBlendEquationSeparate(equationRgb, equationAlpha);
        if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
        GL11.glCullFace(cullFace);
        GL11.glFrontFace(frontFace);
        RenderSystem.polygonOffset(offsetFactor, offsetUnits);
        if (polygonOffset) RenderSystem.enablePolygonOffset(); else RenderSystem.disablePolygonOffset();
        if (scissor) RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
        else RenderSystem.disableScissor();
        RenderSystem.colorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
    }
}
