package tennouboshiuzume.mods.FantasyDesire.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.client.FDShaderHandler;
import tennouboshiuzume.mods.FantasyDesire.client.compat.ShaderPackCompat;
import tennouboshiuzume.mods.FantasyDesire.client.compat.ShaderRenderScope;
import tennouboshiuzume.mods.FantasyDesire.init.FDAttributes;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.WeakHashMap;

/** 将虚空斑块和回响裂隙直接绘制在当前姿态的实体表面。 */
@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VoidFlameRenderer {
    private static final BufferBuilder BUILDER = new BufferBuilder(4096);
    private static final ArrayDeque<Surface> SURFACES = new ArrayDeque<>();
    private static final Map<LivingEntity, VisualState> STATES = new WeakHashMap<>();
    private static ClientLevel currentLevel;
    private static VertexBuffer vertexBuffer;
    private static boolean collecting;

    private VoidFlameRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (ShaderPackCompat.isRenderingShadowPass()) return;
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            releaseSurfaces();
            ClientLevel level = Minecraft.getInstance().level;
            if (currentLevel != level) {
                STATES.clear();
                currentLevel = level;
            }
            collecting = level != null && FDShaderHandler.isVoidFlameShaderLoaded();
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            collecting = false;
            if (!ShaderPackCompat.isShaderPackInUse()) renderSurfaces();
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            collecting = false;
            if (ShaderPackCompat.isShaderPackInUse() && !SURFACES.isEmpty()) {
                try (ShaderRenderScope ignored = new ShaderRenderScope()) {
                    renderSurfaces();
                }
            }
        }
    }

    public static <T extends LivingEntity> void capture(T entity, EntityModel<T> model, ResourceLocation texture,
            PoseStack poseStack, int packedLight, float partialTick) {
        if (!collecting || ShaderPackCompat.isRenderingShadowPass()
                || entity.isInvisible() || entity.isRemoved() || entity.level() != currentLevel) {
            return;
        }
        float stacks = positive(FDAttributes.getVoidStrikeStack(entity));
        float damage = positive(FDAttributes.getTotalEchoDamage(entity));
        if (stacks == 0.0F && damage == 0.0F) {
            STATES.remove(entity);
            return;
        }

        Matrix4f root = poseStack.last().pose();
        // 特殊模型可能在过渡动画中缩放到零，此时不能计算逆矩阵。
        float determinant = root.determinant();
        if (!Float.isFinite(determinant) || Math.abs(determinant) < 1.0E-8F) {
            return;
        }
        double now = entity.tickCount + (double) partialTick;
        VisualState state = STATES.computeIfAbsent(entity, ignored -> new VisualState());
        state.update(stacks, damage, now);

        BUILDER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
        BufferBuilder.RenderedBuffer mesh;
        try {
            model.renderToBuffer(poseStack, BUILDER, packedLight, OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, 1.0F);
        } catch (RuntimeException exception) {
            mesh = BUILDER.endOrDiscardIfEmpty();
            if (mesh != null) {
                mesh.release();
            }
            throw exception;
        }
        mesh = BUILDER.endOrDiscardIfEmpty();
        if (mesh != null) {
            SURFACES.add(new Surface(mesh, texture, new Matrix4f(root).invert(),
                    new Matrix4f(RenderSystem.getModelViewMatrix()), new Matrix4f(RenderSystem.getProjectionMatrix()),
                    (float) ((now / 20.0) % 4096.0), (entity.getUUID().hashCode() & 0xFFFF) / 65535.0F,
                    state.voidStrength, state.echoStrength, state.pulseAges(now),
                    new Vector4f(state.pulses[0], state.pulses[1], state.pulses[2], state.pulses[3])));
        }
    }

    private static void renderSurfaces() {
        if (SURFACES.isEmpty()) {
            return;
        }
        ShaderInstance shader = FDShaderHandler.getVoidFlameShader();
        if (!FDShaderHandler.isVoidFlameShaderLoaded() || shader == null) {
            releaseSurfaces();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        // 原版仍有延迟提交的基础模型与盔甲；每帧统一提交一次，避免覆盖层被稍后绘制的皮肤盖住。
        minecraft.renderBuffers().bufferSource().endBatch();
        minecraft.getMainRenderTarget().bindWrite(false);
        int oldTexture = RenderSystem.getShaderTexture(0);
        if (vertexBuffer == null) {
            vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-1.0F, -1.0F);
        try {
            while (!SURFACES.isEmpty()) {
                Surface surface = SURFACES.removeFirst();
                // upload 接管并释放 CPU 网格，即使上传抛错也不会重复释放。
                vertexBuffer.bind();
                vertexBuffer.upload(surface.mesh());
                RenderSystem.setShaderTexture(0, surface.texture());
                shader.safeGetUniform("EffectLocalMat").set(surface.localMatrix());
                shader.safeGetUniform("EffectTime").set(surface.time());
                shader.safeGetUniform("EffectSeed").set(surface.seed());
                shader.safeGetUniform("VoidStrength").set(surface.voidStrength());
                shader.safeGetUniform("EchoStrength").set(surface.echoStrength());
                shader.safeGetUniform("EchoPulseAges").set(surface.pulseAges());
                shader.safeGetUniform("EchoPulses").set(surface.pulses());
                vertexBuffer.drawWithShader(surface.modelView(), surface.projection(), shader);
            }
        } finally {
            releaseSurfaces();
            VertexBuffer.unbind();
            RenderSystem.setShaderTexture(0, oldTexture);
            RenderSystem.polygonOffset(0.0F, 0.0F);
            RenderSystem.disablePolygonOffset();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
        }
    }

    private static float positive(float value) {
        return Float.isFinite(value) ? Math.max(0.0F, value) : 0.0F;
    }

    private static void releaseSurfaces() {
        while (!SURFACES.isEmpty()) {
            SURFACES.removeFirst().mesh().release();
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (RenderSystem.isOnRenderThread()) {
            clear();
        } else {
            RenderSystem.recordRenderCall(VoidFlameRenderer::clear);
        }
    }

    private static void clear() {
        collecting = false;
        releaseSurfaces();
        STATES.clear();
        currentLevel = null;
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }

    private record Surface(BufferBuilder.RenderedBuffer mesh, ResourceLocation texture, Matrix4f localMatrix,
            Matrix4f modelView, Matrix4f projection, float time, float seed, float voidStrength, float echoStrength,
            Vector4f pulseAges, Vector4f pulses) {
    }

    private static final class VisualState {
        private double previousTime = -1.0;
        private final double[] pulseTimes = { -100.0, -100.0, -100.0, -100.0 };
        private final float[] pulses = new float[4];
        private int nextPulse;
        private float previousDamage;
        private float voidStrength;
        private float echoStrength;

        private void update(float stacks, float damage, double now) {
            // 时间用实体 tick + partialTick；平滑与帧率无关，暂停时不推进。
            float elapsed = previousTime < 0.0 ? 1.0F : (float) Math.max(0.0, now - previousTime);
            float blend = 1.0F - (float) Math.exp(-elapsed / 2.5F);
            float targetVoid = (float) Math.sqrt(Math.min(stacks / 50.0F, 1.0F));
            // 低伤害也可见，超过 1000 后仍能增长；不修改服务端伤害值。
            float targetEcho = damage / (damage + 120.0F);
            voidStrength = stacks == 0.0F ? 0.0F : Mth.lerp(blend, voidStrength, targetVoid);
            echoStrength = damage == 0.0F ? 0.0F : Mth.lerp(blend, echoStrength, targetEcho);
            if (damage > previousDamage) {
                float pulse = 0.35F + 0.65F * (float) Math.sqrt((damage - previousDamage) / (damage + 16.0F));
                int previousPulse = (nextPulse + 3) % 4;
                // 高频命中合并进最近的波，不重置传播位置；四道波最多同时覆盖 1.2 秒。
                if (now - pulseTimes[previousPulse] < 6.0) {
                    pulses[previousPulse] = Math.min(1.0F, pulses[previousPulse] + pulse * 0.5F);
                } else {
                    pulseTimes[nextPulse] = now;
                    pulses[nextPulse] = pulse;
                    nextPulse = (nextPulse + 1) % 4;
                }
            } else if (damage == 0.0F) {
                for (int i = 0; i < 4; ++i) {
                    pulses[i] = 0.0F;
                    pulseTimes[i] = -100.0;
                }
            }
            previousTime = now;
            previousDamage = damage;
        }

        private Vector4f pulseAges(double now) {
            // 每道波的年龄以秒上传，着色器在 1.2 秒后严格清除贡献。
            return new Vector4f(pulseAge(now, 0), pulseAge(now, 1), pulseAge(now, 2), pulseAge(now, 3));
        }

        private float pulseAge(double now, int index) {
            return (float) Math.min(2.0, Math.max(0.0, (now - pulseTimes[index]) / 20.0));
        }
    }
}
