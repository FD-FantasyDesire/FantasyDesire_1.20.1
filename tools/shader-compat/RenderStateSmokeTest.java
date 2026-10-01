package tennouboshiuzume.mods.FantasyDesire.client.compat;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import tennouboshiuzume.mods.FantasyDesire.mixin.client.ShaderInstanceAccessor;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.Optional;

/** 独立隐藏 GL 检查，不启动游戏、不修改用户窗口或世界存档。 */
public final class RenderStateSmokeTest {
    public static void main(String[] args) throws Exception {
        if (!GLFW.glfwInit()) throw new AssertionError("GLFW init failed");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        long window = GLFW.glfwCreateWindow(64, 64, "Shader compatibility probe", 0, 0);
        if (window == 0) throw new AssertionError("Hidden GL context creation failed");
        try {
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            RenderSystem.initRenderThread();
            RenderSystem.initGameThread(false);
            verifyScope();
            verifyUniforms(Path.of(args[0]));
            if (GL11.glGetError() != GL11.GL_NO_ERROR) throw new AssertionError("OpenGL error");
            System.out.println("PASS: GL state restoration, deferred GPU buffer ownership, uniform/sampler isolation, nonblank/distinct pixels");
        } finally {
            GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
        }
    }

    private static void verifyScope() {
        RenderSystem.viewport(3, 5, 37, 41);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
        RenderSystem.disableCull();
        RenderSystem.setProjectionMatrix(new Matrix4f().scale(2), VertexSorting.ORTHOGRAPHIC_Z);
        int fbo = GL30.glGenFramebuffers();
        try {
            try (ShaderRenderScope ignored = new ShaderRenderScope()) {
                RenderSystem.viewport(0, 0, 16, 16);
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();
                RenderSystem.enableCull();
                RenderSystem.colorMask(false, false, false, false);
                RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.DISTANCE_TO_ORIGIN);
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
            }
            int[] viewport = new int[4];
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            check(viewport[0] == 3 && viewport[1] == 5 && viewport[2] == 37 && viewport[3] == 41, "viewport");
            check(GL11.glIsEnabled(GL11.GL_DEPTH_TEST) && !GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK), "depth");
            check(GL11.glIsEnabled(GL11.GL_BLEND) && GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB) == GL11.GL_ONE, "blend");
            check(!GL11.glIsEnabled(GL11.GL_CULL_FACE), "cull");
            check(GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) == 0, "framebuffer");
            check(RenderSystem.getProjectionMatrix().m00() == 2, "projection");
        } finally {
            GL30.glDeleteFramebuffers(fbo);
        }
    }

    private static void verifyUniforms(Path root) throws Exception {
        Path resources = root.resolve("src/main/resources");
        try (PathPackResources pack = new PathPackResources("probe", resources, false)) {
            ResourceProvider provider = location -> {
                Path file = resources.resolve("assets").resolve(location.getNamespace()).resolve(location.getPath());
                return Files.isRegularFile(file)
                        ? Optional.of(new Resource(pack, () -> Files.newInputStream(file))) : Optional.empty();
            };
            try (ProbeShader shader = new ProbeShader(provider, "fd_bfg_bridge", DefaultVertexFormat.POSITION_COLOR_TEX)) {
                shader.getUniform("Time").set(1.25F);
                shader.getUniform("CoreColor").set(1F, 0.2F, 0.3F, 0.4F);
                shader.setSampler("ProbeSampler", 11);
                ShaderUniformState first = new ShaderUniformState(shader);
                shader.getUniform("Time").set(9.5F);
                shader.getUniform("CoreColor").set(0.1F, 0.8F, 0.7F, 0.6F);
                shader.setSampler("ProbeSampler", 22);
                ShaderUniformState second = new ShaderUniformState(shader);
                first.apply(shader);
                check(shader.getUniform("Time").getFloatBuffer().get(0) == 1.25F, "first instance time");
                check(shader.getUniform("CoreColor").getFloatBuffer().get(0) == 1F, "first instance color");
                check(shader.fantasydesire$getSamplerMap().get("ProbeSampler").equals(11), "first sampler");
                second.apply(shader);
                check(shader.getUniform("Time").getFloatBuffer().get(0) == 9.5F, "second instance time");
                check(shader.getUniform("CoreColor").getFloatBuffer().get(1) == 0.8F, "second instance color");
                check(shader.fantasydesire$getSamplerMap().get("ProbeSampler").equals(22), "second sampler");
                verifyPixels(shader, first, second);
            }
            try (ProbeShader shader = new ProbeShader(provider, "fd_shin", DefaultVertexFormat.POSITION)) {
                shader.getUniform("ResolvePass").set(1);
                shader.getUniform("EffectLocalMat").set(new Matrix4f().scale(3));
                ShaderUniformState first = new ShaderUniformState(shader);
                shader.getUniform("ResolvePass").set(0);
                shader.getUniform("EffectLocalMat").set(new Matrix4f());
                first.apply(shader);
                check(shader.getUniform("ResolvePass").getIntBuffer().get(0) == 1, "integer uniform");
                check(shader.getUniform("EffectLocalMat").getFloatBuffer().get(0) == 3, "matrix uniform");
            }
        }
    }

    private static void verifyPixels(ShaderInstance shader, ShaderUniformState first, ShaderUniformState second) {
        try (ShaderRenderScope ignored = new ShaderRenderScope()) {
            RenderSystem.viewport(0, 0, 64, 64);
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.getModelViewStack().setIdentity();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShader(() -> shader);
            setStatic(ShaderPackCompat.class, "resolved", true);
            Map<ShaderInstance, FDShaderCompat.Pass> passes = getStatic(FDShaderCompat.class, "PASSES");
            passes.put(shader, FDShaderCompat.Pass.DEFERRED_WORLD);
            setStatic(FDShaderCompat.class, "capturing", true);
            int[] colors = new int[2];
            ShaderUniformState[] states = {first, second};
            BufferBuilder builder = new BufferBuilder(256);
            for (int i = 0; i < states.length; i++) {
                states[i].apply(shader);
                builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
                builder.vertex(-1, -1, 0).color(255, 255, 255, 255).uv(0, 0).endVertex();
                builder.vertex(1, -1, 0).color(255, 255, 255, 255).uv(1, 0).endVertex();
                builder.vertex(1, 1, 0).color(255, 255, 255, 255).uv(1, 1).endVertex();
                builder.vertex(-1, 1, 0).color(255, 255, 255, 255).uv(0, 1).endVertex();
                check(FDShaderCompat.capture(builder.end()), "deferred capture");
            }
            // 同一个 CPU builder 已被复用；排队顶点与各自的 uniform 必须仍然独立。
            ArrayDeque<?> batches = getStatic(FDShaderCompat.class, "BATCHES");
            check(batches.size() == 2, "queued batch count");
            int i = 0;
            for (Object batch : batches) {
                RenderSystem.clearColor(0, 0, 0, 0);
                RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, false);
                ShaderUniformState snapshot = component(batch, "uniforms");
                snapshot.apply(shader);
                Object gpu = component(batch, "mesh");
                VertexBuffer mesh = component(gpu, "buffer");
                mesh.bind();
                mesh.drawWithShader(new Matrix4f(), new Matrix4f(), shader);
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    ByteBuffer pixel = stack.malloc(4);
                    GL11.glReadPixels(32, 32, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
                    colors[i] = (pixel.get(0) & 255) | ((pixel.get(1) & 255) << 8) | ((pixel.get(2) & 255) << 16);
                    check(colors[i] != 0, "nonblank effect pixels");
                }
                i++;
            }
            check(colors[0] != colors[1], "distinct instance pixels");
        } finally {
            FDShaderCompat.resetShaders();
            FDShaderCompat.onLogout(null);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getStatic(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void setStatic(Class<?> type, String name, Object value) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T component(Object record, String name) {
        try {
            var method = record.getClass().getDeclaredMethod(name);
            method.setAccessible(true);
            return (T) method.invoke(record);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError("State mismatch: " + label);
    }

    // 独立进程没有 Mixin 启动器，测试对象提供与 accessor 相同的数据接口。
    private static final class ProbeShader extends ShaderInstance implements ShaderInstanceAccessor {
        ProbeShader(ResourceProvider provider, String name, VertexFormat format) throws Exception {
            super(provider, new ResourceLocation("fantasydesire", name), format);
        }

        @Override
        public Map<String, Uniform> fantasydesire$getUniformMap() {
            return field("uniformMap");
        }

        @Override
        public Map<String, Object> fantasydesire$getSamplerMap() {
            return field("samplerMap");
        }

        @SuppressWarnings("unchecked")
        private <T> T field(String name) {
            try {
                Field field = ShaderInstance.class.getDeclaredField(name);
                field.setAccessible(true);
                return (T) field.get(this);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }
    }
}
