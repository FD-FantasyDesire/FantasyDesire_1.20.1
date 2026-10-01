package tennouboshiuzume.mods.FantasyDesire.client.compat;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** 光影开启时冻结本模组的世界批次，在光影最终合成后重放。 */
@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FDShaderCompat {
    public enum Pass {
        DEFERRED_WORLD,
        POST_WORLD
    }

    private static final int MAX_BATCHES = 512;
    private static final long MAX_BYTES = 32L * 1024 * 1024;
    private static final int MAX_POOLED_BUFFERS = 64;
    private static final Map<ShaderInstance, Pass> PASSES = new IdentityHashMap<>();
    private static final Map<String, ShaderInstance> SHADERS = new HashMap<>();
    private static final ArrayDeque<Batch> BATCHES = new ArrayDeque<>();
    private static final ArrayDeque<GpuBuffer> POOL = new ArrayDeque<>();
    private static ClientLevel frameLevel;
    private static boolean capturing;
    private static boolean replaying;
    private static boolean limitLogged;
    private static boolean activeLogged;
    private static long queuedBytes;
    private static long pooledBytes;

    private FDShaderCompat() {
    }

    public static void registerShader(RegisterShadersEvent event, ShaderInstance shader, Pass pass,
            Consumer<ShaderInstance> loaded) {
        event.registerShader(shader, instance -> {
            PASSES.put(instance, pass);
            SHADERS.put(instance.getName(), instance);
            loaded.accept(instance);
        });
    }

    public static void resetShaders() {
        finishFrame();
        PASSES.clear();
        SHADERS.clear();
    }

    public static void beginFrame() {
        finishFrame();
        frameLevel = Minecraft.getInstance().level;
        capturing = frameLevel != null && ShaderPackCompat.isShaderPackInUse();
        if (capturing && !activeLogged) {
            activeLogged = true;
            System.out.println("[FantasyDesire] Shader-pack compatibility active: world effects replay at AFTER_LEVEL.");
        }
    }

    /** 返回 true 时已经接管并释放 CPU 缓冲，调用者必须取消原版提交。 */
    public static boolean capture(BufferBuilder.RenderedBuffer buffer) {
        ShaderInstance shader = RenderSystem.getShader();
        if (!capturing || replaying || PASSES.get(shader) != Pass.DEFERRED_WORLD) return false;
        if (ShaderPackCompat.isRenderingShadowPass() || buffer.isEmpty()) {
            buffer.release();
            return true;
        }
        long bytes = buffer.drawState().bufferSize();
        if (BATCHES.size() >= MAX_BATCHES || queuedBytes + bytes > MAX_BYTES) {
            buffer.release();
            if (!limitLogged) {
                limitLogged = true;
                System.err.println("[FantasyDesire] Deferred shader queue exceeded 512 batches / 32 MiB; excess effects skipped.");
            }
            return true;
        }
        ShaderRenderState state = new ShaderRenderState();
        ShaderUniformState uniforms = new ShaderUniformState(shader);
        int oldVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int oldArrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        GpuBuffer pooled = POOL.pollFirst();
        if (pooled != null) pooledBytes -= pooled.bytes();
        VertexBuffer mesh = pooled == null ? new VertexBuffer(VertexBuffer.Usage.DYNAMIC) : pooled.buffer();
        try {
            mesh.bind();
            // upload 立即消费 CPU 数据，后续 BufferBuilder 复用不会污染排队顶点。
            mesh.upload(buffer);
            BATCHES.addLast(new Batch(new GpuBuffer(mesh, bytes), shader.getName(), state, uniforms));
            queuedBytes += bytes;
        } catch (RuntimeException | Error e) {
            mesh.close();
            throw e;
        } finally {
            BufferUploader.invalidate();
            GlStateManager._glBindVertexArray(oldVao);
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, oldArrayBuffer);
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void closeCaptureWindow(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) capturing = false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void replayWorldEffects(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || BATCHES.isEmpty()) return;
        if (frameLevel != Minecraft.getInstance().level || ShaderPackCompat.isRenderingShadowPass()) {
            finishFrame();
            return;
        }
        replaying = true;
        try (ShaderRenderScope ignored = new ShaderRenderScope()) {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            while (!BATCHES.isEmpty()) {
                Batch batch = BATCHES.removeFirst();
                try {
                    // 以名称解析本帧最新实例；资源重载会清空队列和登记表。
                    ShaderInstance shader = SHADERS.get(batch.shaderName());
                    if (shader == null) continue;
                    ShaderUniformState live = new ShaderUniformState(shader);
                    try {
                        batch.state().apply();
                        batch.uniforms().apply(shader);
                        batch.mesh().buffer().bind();
                        batch.mesh().buffer().drawWithShader(batch.state().modelView, batch.state().projection, shader);
                    } finally {
                        shader.clear();
                        live.apply(shader);
                    }
                } finally {
                    recycle(batch.mesh());
                }
            }
        } finally {
            replaying = false;
            finishFrame();
        }
    }

    public static void finishFrame() {
        capturing = false;
        while (!BATCHES.isEmpty()) recycle(BATCHES.removeFirst().mesh());
        queuedBytes = 0;
        frameLevel = null;
    }

    private static void recycle(GpuBuffer mesh) {
        if (POOL.size() < MAX_POOLED_BUFFERS && pooledBytes + mesh.bytes() <= MAX_BYTES) {
            POOL.addLast(mesh);
            pooledBytes += mesh.bytes();
        } else {
            mesh.buffer().close();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Runnable cleanup = () -> {
            finishFrame();
            while (!POOL.isEmpty()) POOL.removeFirst().buffer().close();
            pooledBytes = 0;
        };
        if (RenderSystem.isOnRenderThread()) cleanup.run();
        else RenderSystem.recordRenderCall(cleanup::run);
    }

    private record GpuBuffer(VertexBuffer buffer, long bytes) {
    }

    private record Batch(GpuBuffer mesh, String shaderName, ShaderRenderState state, ShaderUniformState uniforms) {
    }
}
