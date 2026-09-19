package tennouboshiuzume.mods.FantasyDesire.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.client.FDShaderHandler;
import tennouboshiuzume.mods.FantasyDesire.config.FDConfig;
import tennouboshiuzume.mods.FantasyDesire.init.FDAttributes;
import tennouboshiuzume.mods.FantasyDesire.potioneffect.FrostStormEffect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 依据场景深度绘制冰封大地、脉冲和半透明冰裂球壳，再绘制贴地生长的冰晶。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FrostStormFieldRenderer {
    private static RenderTarget sceneDepthCopy;
    private static RenderTarget blockDepthCopy;
    private static boolean blockDepthReady;
    private static ClientLevel visualLevel;
    private static final Map<UUID, FieldVisual> FIELD_VISUALS = new HashMap<>();

    private FrostStormFieldRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!FDShaderHandler.isFrostFieldShaderLoaded()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        if (visualLevel != level) {
            FIELD_VISUALS.clear();
            blockDepthReady = false;
            visualLevel = level;
        }

        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            blockDepthReady = false;
            if (collectVisibleFields(level, event, cameraPosition).isEmpty()) {
                return;
            }

            blockDepthCopy = ensureDepthCopy(blockDepthCopy, mainTarget);
            blockDepthCopy.copyDepthFrom(mainTarget);
            blockDepthReady = true;
            return;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !blockDepthReady) {
            return;
        }

        List<Field> fields = collectVisibleFields(level, event, cameraPosition);
        if (fields.isEmpty()) {
            blockDepthReady = false;
            return;
        }

        sceneDepthCopy = ensureDepthCopy(sceneDepthCopy, mainTarget);
        sceneDepthCopy.copyDepthFrom(mainTarget);
        mainTarget.bindWrite(true);

        Matrix3f viewRotation = new Matrix3f(RenderSystem.getInverseViewRotationMatrix()).invert();
        Matrix4f viewProjection = new Matrix4f(event.getProjectionMatrix()).mul(new Matrix4f(viewRotation));
        Matrix4f inverseViewProjection = new Matrix4f(viewProjection).invert();
        // 闪光周期为 60 tick，2400 tick 回绕保持连续；外扩脉冲使用独立状态。
        float fieldTime = (float) (level.getGameTime() % 2400L) + event.getPartialTick();
        float depthScaleX = (float) mainTarget.viewWidth / mainTarget.width;
        float depthScaleY = (float) mainTarget.viewHeight / mainTarget.height;
        Vec3 patternOrigin = cameraPosition;

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);

        try {
            for (Field field : fields) {
                ShaderInstance shader = FDShaderHandler.getFrostFieldShader();
                if (shader == null) {
                    break;
                }

                shader.setSampler("DepthSampler", sceneDepthCopy.getDepthTextureId());
                shader.setSampler("BlockDepthSampler", blockDepthCopy.getDepthTextureId());
                set(shader, "InvViewProj", inverseViewProjection);
                set(shader, "PatternOrigin", patternOrigin);
                set(shader, "FieldCenter", field.centerRelativeToCamera());
                set(shader, "FieldRadius", field.radius());
                set(shader, "FieldTime", fieldTime);
                set(shader, "FieldAge", field.age());
                set(shader, "PulseRadius", field.pulseRadius());
                set(shader, "DepthUvScale", depthScaleX, depthScaleY);

                RenderSystem.setShader(() -> shader);
                drawFullscreenQuad();
            }
            for (Field field : fields) {
                field.visual().crystals.render(viewProjection, cameraPosition,
                        field.centerRelativeToCamera(), field.radius(), field.age(), field.pulseRadius(),
                        level.getGameTime(), event.getPartialTick());
            }
        } finally {
            blockDepthReady = false;
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
        }
    }

    private static List<Field> collectVisibleFields(ClientLevel level, RenderLevelStageEvent event,
            Vec3 cameraPosition) {
        List<Field> fields = new ArrayList<>();
        Set<UUID> active = new HashSet<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }

            // 属性修改器检测：服务端每 tick 同步的半径/强度（绕开 MobEffect 对非玩家实体同步失败）
            float radius = Math.min(FDAttributes.getStormRadius(living), FDConfig.FROST_STORM.radiusCap());
            if (!Float.isFinite(radius) || radius <= 0.01F || FDAttributes.getStormStrength(living) <= 0.01F) {
                continue;
            }

            Vec3 center = FrostStormEffect.getFieldCenter(living, event.getPartialTick());
            active.add(living.getUUID());
            FieldVisual visual = FIELD_VISUALS.computeIfAbsent(living.getUUID(),
                    ignored -> new FieldVisual(level.getGameTime(), living.getUUID()));
            float pulseRadius = visual.pulse.advance(level.getGameTime() + (double) event.getPartialTick(), radius);
            AABB bounds = new AABB(center.x - radius, center.y - radius, center.z - radius,
                    center.x + radius, center.y + radius, center.z + radius);
            if (!event.getFrustum().isVisible(bounds)) {
                continue;
            }

            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
                visual.crystals.update(level, living, center, radius);
            }
            float age = Math.min(240.0F, level.getGameTime() - visual.startedAt + event.getPartialTick());
            fields.add(new Field(center.subtract(cameraPosition), radius, age, pulseRadius, visual));
        }
        FIELD_VISUALS.keySet().retainAll(active);
        fields.sort(Comparator.comparingDouble((Field field) -> field.centerRelativeToCamera().lengthSqr()).reversed());
        return fields;
    }

    private static RenderTarget ensureDepthCopy(RenderTarget depthCopy, RenderTarget mainTarget) {
        boolean stencilChanged = depthCopy != null
                && depthCopy.isStencilEnabled() != mainTarget.isStencilEnabled();
        if (depthCopy == null || stencilChanged) {
            if (depthCopy != null) {
                depthCopy.destroyBuffers();
            }
            depthCopy = new TextureTarget(mainTarget.width, mainTarget.height, true, Minecraft.ON_OSX);
            if (mainTarget.isStencilEnabled()) {
                depthCopy.enableStencil();
            }
        } else if (depthCopy.width != mainTarget.width || depthCopy.height != mainTarget.height) {
            depthCopy.resize(mainTarget.width, mainTarget.height, Minecraft.ON_OSX);
        }
        return depthCopy;
    }

    private static void drawFullscreenQuad() {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(-1.0, -1.0, -1.0).uv(0.0F, 0.0F).endVertex();
        builder.vertex(1.0, -1.0, -1.0).uv(1.0F, 0.0F).endVertex();
        builder.vertex(1.0, 1.0, -1.0).uv(1.0F, 1.0F).endVertex();
        builder.vertex(-1.0, 1.0, -1.0).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(builder.end());
    }

    private static void set(ShaderInstance shader, String name, Matrix4f value) {
        if (shader.getUniform(name) != null) {
            shader.getUniform(name).set(value);
        }
    }

    private static void set(ShaderInstance shader, String name, Vec3 value) {
        if (shader.getUniform(name) != null) {
            shader.getUniform(name).set((float) value.x, (float) value.y, (float) value.z);
        }
    }

    private static void set(ShaderInstance shader, String name, float value) {
        if (shader.getUniform(name) != null) {
            shader.getUniform(name).set(value);
        }
    }

    private static void set(ShaderInstance shader, String name, float x, float y) {
        if (shader.getUniform(name) != null) {
            shader.getUniform(name).set(x, y);
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        FIELD_VISUALS.clear();
        visualLevel = null;
        RenderTarget sceneTarget = sceneDepthCopy;
        RenderTarget blockTarget = blockDepthCopy;
        sceneDepthCopy = null;
        blockDepthCopy = null;
        blockDepthReady = false;
        if (sceneTarget == null && blockTarget == null) {
            return;
        }
        if (RenderSystem.isOnRenderThread()) {
            destroy(sceneTarget);
            destroy(blockTarget);
        } else {
            RenderSystem.recordRenderCall(() -> {
                destroy(sceneTarget);
                destroy(blockTarget);
            });
        }
    }

    private static void destroy(RenderTarget target) {
        if (target != null) {
            target.destroyBuffers();
        }
    }

    private static final class FieldVisual {
        private final long startedAt;
        private final FrostFieldCrystals crystals;
        private final FrostFieldPulse pulse = new FrostFieldPulse();

        private FieldVisual(long startedAt, UUID owner) {
            this.startedAt = startedAt;
            this.crystals = new FrostFieldCrystals(owner.getMostSignificantBits() ^ owner.getLeastSignificantBits() ^ startedAt);
        }
    }

    private record Field(Vec3 centerRelativeToCamera, float radius, float age, float pulseRadius, FieldVisual visual) {
    }
}
