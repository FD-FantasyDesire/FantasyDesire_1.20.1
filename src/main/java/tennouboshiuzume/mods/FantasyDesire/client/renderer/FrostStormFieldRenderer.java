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
import net.minecraft.world.effect.MobEffectInstance;
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
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.potioneffect.FrostStormEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * 在世界渲染结束时依据场景深度绘制寒霜风暴球形能量场。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FrostStormFieldRenderer {
    private static RenderTarget sceneDepthCopy;
    private static RenderTarget blockDepthCopy;
    private static boolean blockDepthReady;

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
        Matrix4f inverseViewProjection = new Matrix4f(event.getProjectionMatrix())
                .mul(new Matrix4f(viewRotation))
                .invert();
        float fieldTime = (float) (level.getGameTime() & 0xFFFFFL) + event.getPartialTick();
        float depthScaleX = (float) mainTarget.viewWidth / mainTarget.width;
        float depthScaleY = (float) mainTarget.viewHeight / mainTarget.height;
        Vec3 patternOrigin = cameraPosition;

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE,
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
                set(shader, "DepthUvScale", depthScaleX, depthScaleY);

                RenderSystem.setShader(() -> shader);
                drawFullscreenQuad();
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
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }

            MobEffectInstance effect = living.getEffect(FDPotionEffects.FROST_STORM.get());
            if (effect == null) {
                continue;
            }

            float radius = FrostStormEffect.getFieldRadius(effect.getAmplifier());
            Vec3 center = FrostStormEffect.getFieldCenter(living, event.getPartialTick());
            AABB bounds = new AABB(center.x - radius, center.y - radius, center.z - radius,
                    center.x + radius, center.y + radius, center.z + radius);
            if (!event.getFrustum().isVisible(bounds)) {
                continue;
            }

            fields.add(new Field(center.subtract(cameraPosition), radius));
        }
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

    private record Field(Vec3 centerRelativeToCamera, float radius) {
    }
}
