package tennouboshiuzume.mods.FantasyDesire.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
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
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.client.FDShaderHandler;
import tennouboshiuzume.mods.FantasyDesire.init.FDAttributes;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 将活体模型写入独立遮罩，再在世界渲染结束后合成双层虚空焰型轮廓。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VoidFlameOutlineRenderer {
    private static final float MAX_VOID_STRIKE_STACK = 50.0F;
    private static final float MAX_ECHO_DAMAGE = 1000.0F;
    private static final float MAX_EFFECT_RADIUS = 128.0F;
    private static final BufferBuilder SILHOUETTE_BUILDER = new BufferBuilder(256);
    private static final Map<LivingEntity, Integer> OUTLINED_ENTITIES = new IdentityHashMap<>();

    private static RenderTarget silhouetteTarget;
    private static boolean maskReady;
    private static boolean hasSilhouette;
    private static int nextTargetId;

    private VoidFlameOutlineRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            prepareMaskTarget();
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            renderOutline(event);
        }
    }

    private static void prepareMaskTarget() {
        maskReady = false;
        hasSilhouette = false;
        OUTLINED_ENTITIES.clear();
        nextTargetId = 1;
        if (!FDShaderHandler.isVoidFlameMaskShaderLoaded()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        silhouetteTarget = ensureTarget(silhouetteTarget, mainTarget);
        silhouetteTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        silhouetteTarget.bindWrite(true);
        silhouetteTarget.clear(Minecraft.ON_OSX);
        silhouetteTarget.copyDepthFrom(mainTarget);
        mainTarget.bindWrite(false);
        maskReady = true;
    }

    public static <T extends LivingEntity> void renderSilhouette(T entity, EntityModel<T> model, PoseStack poseStack,
            int packedLight) {
        if (!maskReady || silhouetteTarget == null || !hasVisualState(entity)) {
            return;
        }

        ShaderInstance shader = FDShaderHandler.getVoidFlameMaskShader();
        if (shader == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        silhouetteTarget.bindWrite(false);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();

        try {
            int targetId = OUTLINED_ENTITIES.computeIfAbsent(entity, ignored -> nextTargetId++);
            setTargetColor(shader, targetId);
            RenderSystem.setShader(() -> shader);
            SILHOUETTE_BUILDER.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
            model.renderToBuffer(poseStack, SILHOUETTE_BUILDER, packedLight, OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, 1.0F);
            BufferUploader.drawWithShader(SILHOUETTE_BUILDER.end());
            hasSilhouette = true;
        } finally {
            minecraft.getMainRenderTarget().bindWrite(false);
            RenderSystem.enableCull();
        }
    }

    private static void renderOutline(RenderLevelStageEvent event) {
        if (!maskReady) {
            return;
        }

        maskReady = false;
        if (!hasSilhouette || silhouetteTarget == null || !FDShaderHandler.isVoidFlameOutlineShaderLoaded()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        ShaderInstance shader = FDShaderHandler.getVoidFlameOutlineShader();
        if (shader == null) {
            return;
        }

        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        mainTarget.bindWrite(true);
        shader.setSampler("SilhouetteSampler", silhouetteTarget.getColorTextureId());
        set(shader, "ScreenSize", mainTarget.viewWidth, mainTarget.viewHeight);
        set(shader, "FlameTime", (float) (level.getGameTime() & 0xFFFFFL) + event.getPartialTick());

        Matrix3f viewRotation = new Matrix3f(RenderSystem.getInverseViewRotationMatrix()).invert();
        Matrix4f viewProjection = new Matrix4f(event.getProjectionMatrix()).mul(new Matrix4f(viewRotation));
        Vec3 cameraPosition = event.getCamera().getPosition();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);

        try {
            for (Map.Entry<LivingEntity, Integer> entry : OUTLINED_ENTITIES.entrySet()) {
                LivingEntity entity = entry.getKey();
                ScreenBounds bounds = projectBounds(entity, event.getPartialTick(), viewProjection, cameraPosition);
                if (bounds == null) {
                    continue;
                }

                float projectedHeight = (bounds.maxY() - bounds.minY()) * mainTarget.viewHeight * 0.5F;
                float effectRadius = Math.min(MAX_EFFECT_RADIUS, Math.max(3.0F, projectedHeight * 0.0575F));
                float outlineMargin = effectRadius * 1.2F + 4.0F;
                // 碰撞箱不包含摆臂、武器等模型姿态外伸部分，预留充足的屏幕空间。
                float modelMargin = projectedHeight * 0.38F + 8.0F;
                float expandX = (outlineMargin + modelMargin) * 2.0F / mainTarget.viewWidth;
                float expandY = (outlineMargin + modelMargin) * 2.0F / mainTarget.viewHeight;
                float minX = Math.max(-1.0F, bounds.minX() - expandX);
                float minY = Math.max(-1.0F, bounds.minY() - expandY);
                float maxX = Math.min(1.0F, bounds.maxX() + expandX);
                float maxY = Math.min(1.0F, bounds.maxY() + expandY);

                set(shader, "EffectRadius", effectRadius);
                set(shader, "EffectCenter", (bounds.minX() + bounds.maxX()) * 0.25F + 0.5F,
                        (bounds.minY() + bounds.maxY()) * 0.25F + 0.5F);
                set(shader, "EffectSeed", (entity.getId() & 0xFFFF) * 0.75487766F);
                set(shader, "VoidStrikeStrength", normalize(FDAttributes.getVoidStrikeStack(entity),
                        MAX_VOID_STRIKE_STACK));
                set(shader, "EchoDamageStrength", normalize(FDAttributes.getTotalEchoDamage(entity),
                        MAX_ECHO_DAMAGE));
                setTargetColor(shader, entry.getValue());
                RenderSystem.setShader(() -> shader);
                drawScreenQuad(minX, minY, maxX, maxY);
            }
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
        }
    }

    private static RenderTarget ensureTarget(RenderTarget target, RenderTarget mainTarget) {
        boolean stencilChanged = target != null && target.isStencilEnabled() != mainTarget.isStencilEnabled();
        if (target == null || stencilChanged) {
            if (target != null) {
                target.destroyBuffers();
            }
            target = new TextureTarget(mainTarget.width, mainTarget.height, true, Minecraft.ON_OSX);
            if (mainTarget.isStencilEnabled()) {
                target.enableStencil();
            }
        } else if (target.width != mainTarget.width || target.height != mainTarget.height) {
            target.resize(mainTarget.width, mainTarget.height, Minecraft.ON_OSX);
        }
        target.setFilterMode(GL11.GL_LINEAR);
        return target;
    }

    private static boolean hasVisualState(LivingEntity entity) {
        return FDAttributes.getVoidStrikeStack(entity) > 0.0F
                || FDAttributes.getTotalEchoDamage(entity) > 0.0F;
    }

    private static float normalize(float value, float maximum) {
        return Math.min(1.0F, Math.max(0.0F, value / maximum));
    }

    private static ScreenBounds projectBounds(LivingEntity entity, float partialTick, Matrix4f viewProjection,
            Vec3 cameraPosition) {
        double renderX = Mth.lerp(partialTick, entity.xo, entity.getX());
        double renderY = Mth.lerp(partialTick, entity.yo, entity.getY());
        double renderZ = Mth.lerp(partialTick, entity.zo, entity.getZ());
        AABB bounds = entity.getBoundingBox().move(renderX - entity.getX(), renderY - entity.getY(),
                renderZ - entity.getZ());
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        for (int xIndex = 0; xIndex < 2; ++xIndex) {
            double x = (xIndex == 0 ? bounds.minX : bounds.maxX) - cameraPosition.x;
            for (int yIndex = 0; yIndex < 2; ++yIndex) {
                double y = (yIndex == 0 ? bounds.minY : bounds.maxY) - cameraPosition.y;
                for (int zIndex = 0; zIndex < 2; ++zIndex) {
                    double z = (zIndex == 0 ? bounds.minZ : bounds.maxZ) - cameraPosition.z;
                    Vector4f clip = viewProjection.transform(new Vector4f((float) x, (float) y, (float) z, 1.0F));
                    if (clip.w <= 0.001F) {
                        return new ScreenBounds(-1.0F, -1.0F, 1.0F, 1.0F);
                    }

                    float inverseW = 1.0F / clip.w;
                    float screenX = clip.x * inverseW;
                    float screenY = clip.y * inverseW;
                    minX = Math.min(minX, screenX);
                    minY = Math.min(minY, screenY);
                    maxX = Math.max(maxX, screenX);
                    maxY = Math.max(maxY, screenY);
                }
            }
        }

        if (maxX < -1.0F || minX > 1.0F || maxY < -1.0F || minY > 1.0F) {
            return null;
        }
        return new ScreenBounds(minX, minY, maxX, maxY);
    }

    private static void drawScreenQuad(float minX, float minY, float maxX, float maxY) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(minX, minY, -1.0F).uv(minX * 0.5F + 0.5F, minY * 0.5F + 0.5F).endVertex();
        builder.vertex(maxX, minY, -1.0F).uv(maxX * 0.5F + 0.5F, minY * 0.5F + 0.5F).endVertex();
        builder.vertex(maxX, maxY, -1.0F).uv(maxX * 0.5F + 0.5F, maxY * 0.5F + 0.5F).endVertex();
        builder.vertex(minX, maxY, -1.0F).uv(minX * 0.5F + 0.5F, maxY * 0.5F + 0.5F).endVertex();
        BufferUploader.drawWithShader(builder.end());
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

    private static void setTargetColor(ShaderInstance shader, int targetId) {
        if (shader.getUniform("TargetColor") != null) {
            shader.getUniform("TargetColor").set(
                    (targetId & 0xFF) / 255.0F,
                    ((targetId >>> 8) & 0xFF) / 255.0F,
                    ((targetId >>> 16) & 0xFF) / 255.0F);
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        RenderTarget maskTarget = silhouetteTarget;
        silhouetteTarget = null;
        maskReady = false;
        hasSilhouette = false;
        OUTLINED_ENTITIES.clear();
        nextTargetId = 1;

        if (RenderSystem.isOnRenderThread()) {
            destroyTarget(maskTarget);
        } else {
            RenderSystem.recordRenderCall(() -> destroyTarget(maskTarget));
        }
    }

    private static void destroyTarget(RenderTarget target) {
        if (target != null) {
            target.destroyBuffers();
        }
    }

    private record ScreenBounds(float minX, float minY, float maxX, float maxY) {
    }
}
