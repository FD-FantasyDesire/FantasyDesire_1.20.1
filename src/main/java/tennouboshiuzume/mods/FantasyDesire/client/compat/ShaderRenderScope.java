package tennouboshiuzume.mods.FantasyDesire.client.compat;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.joml.Matrix3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/** 独立 pass 的状态边界；与矩阵栈自身的 push/pop 配合使用。 */
public final class ShaderRenderScope implements AutoCloseable {
    private final ShaderRenderState previous = new ShaderRenderState();
    private final Matrix3f modelViewNormal = new Matrix3f(RenderSystem.getModelViewStack().last().normal());
    private final VertexSorting sorting = RenderSystem.getVertexSorting();
    private final int drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
    private final int readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
    private final int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
    private final int arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
    private final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
    private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final int[] viewport = new int[4];
    private final int[] boundTextures = new int[12];

    public ShaderRenderScope() {
        RenderSystem.assertOnRenderThread();
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        for (int i = 0; i < boundTextures.length; i++) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + i);
            boundTextures[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        }
        RenderSystem.activeTexture(activeTexture);
    }

    @Override
    public void close() {
        previous.apply();
        RenderSystem.setProjectionMatrix(previous.projection, sorting);
        RenderSystem.getModelViewStack().last().pose().set(previous.modelView);
        RenderSystem.getModelViewStack().last().normal().set(modelViewNormal);
        RenderSystem.applyModelViewMatrix();
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo);
        RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        BufferUploader.invalidate();
        GlStateManager._glBindVertexArray(vao);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
        ProgramManager.glUseProgram(program);
        for (int i = 0; i < boundTextures.length; i++) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + i);
            RenderSystem.bindTexture(boundTextures[i]);
        }
        RenderSystem.activeTexture(activeTexture);
    }
}
