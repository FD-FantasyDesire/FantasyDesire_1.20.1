package tennouboshiuzume.mods.FantasyDesire.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.lwjgl.opengl.GL30;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.client.FDShaderHandler;
import tennouboshiuzume.mods.FantasyDesire.client.compat.ShaderPackCompat;
import tennouboshiuzume.mods.FantasyDesire.client.compat.ShaderRenderScope;
import tennouboshiuzume.mods.FantasyDesire.init.FDAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** 三维环绕流焰，以实体和方块的场景深度截断体积积分。 */
@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShinAuraRenderer {
    private static final ResourceLocation NOISE_TEXTURE = new ResourceLocation(FantasyDesire.MODID,
            "textures/effect/shin_noise.png");
    private static RenderTarget sceneDepthCopy;
    private static RenderTarget auraColor;
    private static List<Aura> pendingAuras = List.of();
    private static ClientLevel snapshotLevel;
    private static ClientLevel transitionLevel;
    private static final Map<LivingEntity, ShinStrengthTransition> STRENGTH_TRANSITIONS = new WeakHashMap<>();
    private static float sceneFogStart, sceneFogEnd;
    private static FogShape sceneFogShape = FogShape.SPHERE;

    private ShinAuraRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                && event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (ShaderPackCompat.isRenderingShadowPass()) return;
        renderLevelStage(event);
    }

    private static void renderLevelStage(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !FDShaderHandler.isShinShaderLoaded()) {
            pendingAuras = List.of();
            snapshotLevel = null;
            if (level == null) {
                STRENGTH_TRANSITIONS.clear();
                transitionLevel = null;
            }
            return;
        }
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        boolean shaderPack = ShaderPackCompat.isShaderPackInUse();
        // 光影与已兼容的冰霜领域对齐，在最终合成后读取共享主深度；原版 Fabulous 保留早期快照。
        boolean snapshotStage = shaderPack
                ? event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL
                : event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES;
        if (snapshotStage) {
            pendingAuras = collect(level, event, event.getCamera().getPosition());
            snapshotLevel = level;
            if (pendingAuras.isEmpty()) return;
            // 实体批次已提交；Fabulous 最终合成会清空主深度，必须在此之前保留快照。
            int drawTarget = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            int readTarget = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            try {
                sceneDepthCopy = ensureDepthCopy(sceneDepthCopy, mainTarget);
                sceneDepthCopy.copyDepthFrom(mainTarget);
            } finally {
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readTarget);
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawTarget);
            }
            sceneFogStart = RenderSystem.getShaderFogStart();
            sceneFogEnd = RenderSystem.getShaderFogEnd();
            sceneFogShape = RenderSystem.getShaderFogShape();
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        List<Aura> auras = snapshotLevel == level ? pendingAuras : List.of();
        pendingAuras = List.of();
        if (auras.isEmpty()) return;

        try (ShaderRenderScope ignored = new ShaderRenderScope()) {
            renderAuras(event, level, mainTarget, auras);
        }
    }

    private static void renderAuras(RenderLevelStageEvent event, ClientLevel level,
            RenderTarget mainTarget, List<Aura> auras) {
        Minecraft minecraft = Minecraft.getInstance();
        // 每帧取新实例，F3+T 后不保留已被关闭的 ShaderInstance。
        ShaderInstance shader = FDShaderHandler.getShinShader();
        if (shader == null) return;
        auraColor = ensureAuraColor(auraColor, mainTarget);
        mainTarget.bindWrite(true);
        shader.setSampler("DepthSampler", sceneDepthCopy.getDepthTextureId());
        shader.setSampler("NoiseSampler", minecraft.getTextureManager().getTexture(NOISE_TEXTURE).getId());
        ShaderInstance previousShader = RenderSystem.getShader();
        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting previousSorting = RenderSystem.getVertexSorting();
        float previousFogStart = RenderSystem.getShaderFogStart(), previousFogEnd = RenderSystem.getShaderFogEnd();
        FogShape previousFogShape = RenderSystem.getShaderFogShape();
        RenderSystem.setShaderFogStart(sceneFogStart);
        RenderSystem.setShaderFogEnd(sceneFogEnd);
        RenderSystem.setShaderFogShape(sceneFogShape);
        // Fabulous 的透明合成可能留下屏幕投影；显式恢复本帧的世界投影。
        RenderSystem.setProjectionMatrix(event.getProjectionMatrix(), VertexSorting.DISTANCE_TO_ORIGIN);
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();
        // 出口面的深度不能代替体积深度，由 shader 逐射线裁剪；不修改场景深度。
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            RenderSystem.setShader(() -> shader);
            // 单位为秒，800 tick = 40 秒，与 shader 周期一致。
            float seconds = ((level.getGameTime() % 800L) + event.getPartialTick()) / 20.0F;
            shader.safeGetUniform("EffectTime").set(seconds);
            shader.safeGetUniform("InvProjMat").set(new Matrix4f(event.getProjectionMatrix()).invert());
            // Forge 1.20.1 的 AFTER_LEVEL pose 是投影栈，必须另外取得世界到视图的旋转。
            Matrix4f view = new Matrix4f(new Matrix3f(RenderSystem.getInverseViewRotationMatrix()).invert());
            for (Aura aura : auras) {
                shader.safeGetUniform("ShinStrength").set(aura.strength());
                shader.safeGetUniform("EffectSeed").set(aura.seed());
                shader.safeGetUniform("EffectYaw").set(aura.yaw());
                Matrix4f pose = new Matrix4f(view)
                        .translate((float) aura.relativeCenter().x, (float) aura.relativeCenter().y,
                                (float) aura.relativeCenter().z)
                        .scale(aura.halfWidth(), aura.halfHeight(), aura.halfWidth());
                shader.safeGetUniform("EffectLocalMat").set(new Matrix4f(pose).invert());
                // 每个实体复用半分辨率目标，保持远到近顺序；采样器绝不绑定当前写入附件。
                auraColor.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                auraColor.clear(Minecraft.ON_OSX);
                auraColor.bindWrite(true);
                shader.setSampler("AuraSampler", sceneDepthCopy.getDepthTextureId());
                shader.safeGetUniform("RenderSize").set((float) auraColor.width, (float) auraColor.height);
                shader.safeGetUniform("ResolvePass").set(0);
                draw(pose);
                mainTarget.bindWrite(true);
                shader.setSampler("AuraSampler", auraColor.getColorTextureId());
                shader.safeGetUniform("RenderSize").set((float) mainTarget.width, (float) mainTarget.height);
                shader.safeGetUniform("ResolvePass").set(1);
                draw(pose);
            }
        } finally {
            mainTarget.bindWrite(true);
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(previousProjection, previousSorting);
            RenderSystem.setShaderFogStart(previousFogStart);
            RenderSystem.setShaderFogEnd(previousFogEnd);
            RenderSystem.setShaderFogShape(previousFogShape);
            RenderSystem.setShader(() -> previousShader);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
        }
    }

    private static List<Aura> collect(ClientLevel level, RenderLevelStageEvent event, Vec3 camera) {
        if (transitionLevel != level) {
            STRENGTH_TRANSITIONS.clear();
            transitionLevel = level;
        }
        double time = level.getGameTime() + (double) event.getPartialTick();
        List<Aura> auras = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            float strength = FDAttributes.getShinStrength(living);
            if (strength <= 0.0F || !living.isAlive() || living.isRemoved()) {
                STRENGTH_TRANSITIONS.remove(living);
                continue;
            }
            // 初始档立即显示 20%；后续档位用 5 tick 平滑过渡，失效后立即清除。
            ShinStrengthTransition transition = STRENGTH_TRANSITIONS.computeIfAbsent(living,
                    ignored -> new ShinStrengthTransition(strength, time));
            transition.update(strength, time);
            if (living.isInvisible()
                    || living.isSpectator()
                    || !living.shouldRender(camera.x, camera.y, camera.z)) continue;
            // 第一人称不把本人的气场铺在镜头上；第三人称及其他实体正常显示。
            if (living == event.getCamera().getEntity() && !event.getCamera().isDetached()) continue;
            float width = living.getBbWidth(), height = living.getBbHeight();
            if (!Float.isFinite(width) || !Float.isFinite(height)
                    || width <= 0.0F || height <= 0.0F) continue;
            // 与 ShaderLab 的 0.6 × 2 格实体对齐：盒宽深 3.6w、高 2h、中心距脚底 0.75h。
            float halfWidth = width * 1.8F, halfHeight = height;
            Vec3 center = living.getPosition(event.getPartialTick()).add(0.0, height * 0.75, 0.0);
            Vec3 relative = center.subtract(camera);
            AABB bounds = new AABB(center.x - halfWidth, center.y - halfHeight, center.z - halfWidth,
                    center.x + halfWidth, center.y + halfHeight, center.z + halfWidth);
            if (!event.getFrustum().isVisible(bounds)) continue;
            float seed = (living.getUUID().hashCode() & 0xFFFF) / 65535.0F;
            // 角度制，跨 ±180° 取最短插值；跟随实体 yaw，不使用镜头或头部 pitch。
            float yaw = Mth.wrapDegrees(Mth.rotLerp(event.getPartialTick(), living.yRotO, living.getYRot()));
            if (!Float.isFinite(yaw)) yaw = 0.0F;
            auras.add(new Aura(relative, halfWidth, halfHeight, transition.value(time), seed, yaw));
        }
        // 标准透明混合，从远到近绘制；相交体积仍受透明排序的近似边界约束。
        auras.sort(Comparator.comparingDouble((Aura aura) -> aura.relativeCenter().lengthSqr()).reversed());
        return auras;
    }

    private static RenderTarget ensureDepthCopy(RenderTarget copy, RenderTarget main) {
        if (copy == null || copy.isStencilEnabled() != main.isStencilEnabled()) {
            if (copy != null) copy.destroyBuffers();
            copy = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
            if (main.isStencilEnabled()) copy.enableStencil();
        } else if (copy.width != main.width || copy.height != main.height) {
            copy.resize(main.width, main.height, Minecraft.ON_OSX);
        }
        return copy;
    }

    private static RenderTarget ensureAuraColor(RenderTarget target, RenderTarget main) {
        int width = Math.max(1, (main.width + 1) / 2), height = Math.max(1, (main.height + 1) / 2);
        if (target == null) target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        else if (target.width != width || target.height != height) target.resize(width, height, Minecraft.ON_OSX);
        return target;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        RenderTarget copy = sceneDepthCopy;
        RenderTarget color = auraColor;
        sceneDepthCopy = null;
        auraColor = null;
        pendingAuras = List.of();
        snapshotLevel = null;
        STRENGTH_TRANSITIONS.clear();
        transitionLevel = null;
        Runnable release = () -> {
            if (copy != null) copy.destroyBuffers();
            if (color != null) color.destroyBuffers();
        };
        if (RenderSystem.isOnRenderThread()) release.run();
        else RenderSystem.recordRenderCall(release::run);
    }

    // 内向绕序，只画射线离开体积的面，镜头进入盒内也不会突然消失。
    private static final float[][] FACES = {
            {-1,-1,1, -1,1,1, 1,1,1, 1,-1,1},
            {1,-1,-1, 1,1,-1, -1,1,-1, -1,-1,-1},
            {1,-1,1, 1,1,1, 1,1,-1, 1,-1,-1},
            {-1,-1,-1, -1,1,-1, -1,1,1, -1,-1,1},
            {-1,1,1, -1,1,-1, 1,1,-1, 1,1,1},
            {-1,-1,-1, -1,-1,1, 1,-1,1, 1,-1,-1}
    };

    private static void draw(Matrix4f pose) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (float[] face : FACES) {
            for (int i = 0; i < 12; i += 3) {
                builder.vertex(pose, face[i], face[i + 1], face[i + 2]).endVertex();
            }
        }
        BufferUploader.drawWithShader(builder.end());
    }

    private record Aura(Vec3 relativeCenter, float halfWidth, float halfHeight,
            float strength, float seed, float yaw) {
    }
}
