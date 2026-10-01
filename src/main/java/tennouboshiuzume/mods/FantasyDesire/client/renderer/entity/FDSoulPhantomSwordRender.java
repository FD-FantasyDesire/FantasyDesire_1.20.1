package tennouboshiuzume.mods.FantasyDesire.client.renderer.entity;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import tennouboshiuzume.mods.FantasyDesire.client.FDShaderHandler;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDSoulPhantomSword;

@OnlyIn(Dist.CLIENT)
public class FDSoulPhantomSwordRender<T extends EntityFDSoulPhantomSword> extends FDPhantomSwordRender<T> {

    private static boolean missingCrossFlashShaderLogged = false;
    private static final float CROSS_FLASH_SIZE_SCALE = 0.5f;

    private static final RenderType CROSS_FLASH_RENDER_TYPE = RenderType.create(
            "fd_soul_cross_flash",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> FDShaderHandler.getCrossFlashShader()))
                    .setTextureState(new RenderStateShard.TextureStateShard(TextureManager.INTENTIONAL_MISSING_TEXTURE,
                            false, false))
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard(
                            "fd_additive_transparency",
                            () -> {
                                RenderSystem.enableBlend();
                                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                                        GlStateManager.DestFactor.ONE);
                            },
                            () -> {
                                RenderSystem.disableBlend();
                                RenderSystem.defaultBlendFunc();
                            }))
                    .setCullState(new RenderStateShard.CullStateShard(false))
                    .setLightmapState(new RenderStateShard.LightmapStateShard(false))
                    .setOverlayState(new RenderStateShard.OverlayStateShard(false))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false))
                    .createCompositeState(false));

    public FDSoulPhantomSwordRender(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn,
            int packedLightIn) {
        matrixStack.pushPose();
        renderCrossFlash(entity, partialTicks, matrixStack, bufferIn, packedLightIn);
        matrixStack.popPose();

        super.render(entity, entityYaw, partialTicks, matrixStack, bufferIn, packedLightIn);
    }

    private void renderCrossFlash(T entity, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn,
            int packedLightIn) {
        int flashTicks = entity.getFlashTicks();

        if (!FDShaderHandler.isCrossFlashShaderLoaded()) {
            if (!missingCrossFlashShaderLogged) {
                System.err.println(
                        "[FantasyDesire] Cross flash shader is not loaded; skipping soul phantom sword flash render.");
                missingCrossFlashShaderLogged = true;
            }
            return;
        }

        final float baseFlashScale = 4.20f * CROSS_FLASH_SIZE_SCALE;
        final float baseAlpha = 0.84f;
        final float pulseScaleAmplitude = 0.34f;
        final float pulseAlpha = 0.36f;
        float pulseDuration = flashTicks > 15 ? 20.0f : 15.0f;
        float progress = Mth.clamp(1.0f - (flashTicks - partialTicks) / pulseDuration, 0.0f, 1.0f);
        float pulsePhase = (float) Math.sin(Math.PI * progress);
        float pulseScale = baseFlashScale * entity.getScale() * (1.0f + pulseScaleAmplitude * pulsePhase);
        float baseScale = baseFlashScale * entity.getScale();

        int hexColor = entity.getColor();
        float r = Math.max(((hexColor >> 16) & 0xFF) / 255.0f, 0.18f);
        float g = Math.max(((hexColor >> 8) & 0xFF) / 255.0f, 0.18f);
        float b = Math.max((hexColor & 0xFF) / 255.0f, 0.18f);

        VertexConsumer crossFlashBuilder = bufferIn.getBuffer(CROSS_FLASH_RENDER_TYPE);

        matrixStack.pushPose();
        Vec3 flashOffset = getCrossFlashOffset(entity, partialTicks);
        matrixStack.translate(flashOffset.x, flashOffset.y, flashOffset.z);
        matrixStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        float yaw = Mth.wrapDegrees(Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot()));
        float rotateDirection = yaw >= 0.0f ? 1.0f : -1.0f;
        float rotationDegrees = rotateDirection * (45.0f + progress * 180.0f);
        matrixStack.mulPose(Axis.ZP.rotationDegrees(rotationDegrees));

        matrixStack.pushPose();
        matrixStack.scale(baseScale, baseScale, baseScale);
        emitCrossFlashQuad(crossFlashBuilder, matrixStack.last().pose(), r, g, b, baseAlpha);
        matrixStack.popPose();

        if (flashTicks > 0 && pulsePhase > 0.001f) {
            matrixStack.scale(pulseScale, pulseScale, pulseScale);
            emitCrossFlashQuad(crossFlashBuilder, matrixStack.last().pose(), r, g, b, pulseAlpha * pulsePhase);
        }

        matrixStack.popPose();
    }

    private Vec3 getCrossFlashOffset(T entity, float partialTicks) {
        Entity hits = entity.getHitEntity();
        if (hits == null) {
            return Vec3.ZERO;
        }

        PoseStack tipStack = new PoseStack();
        tipStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        tipStack.mulPose(Axis.ZP.rotationDegrees(Mth.rotLerp(partialTicks, entity.xRotO, entity.getXRot())));
        tipStack.mulPose(Axis.XP.rotationDegrees(entity.getRoll()));

        float modelScale = 0.0075f * entity.getScale();
        tipStack.scale(modelScale, modelScale, modelScale);
        tipStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        tipStack.translate(0, 0, -100);

        Matrix4f tipMatrix = tipStack.last().pose();
        Vector3f tipOffset = tipMatrix.getTranslation(new Vector3f());
        return new Vec3(tipOffset.x(), tipOffset.y(), tipOffset.z());
    }

    private void emitCrossFlashQuad(VertexConsumer builder, Matrix4f mat, float r, float g, float b, float alpha) {
        writeCrossFlashVertex(builder, mat, -0.5f, -0.5f, r, g, b, alpha, 0.0f, 0.0f);
        writeCrossFlashVertex(builder, mat, -0.5f, 0.5f, r, g, b, alpha, 0.0f, 1.0f);
        writeCrossFlashVertex(builder, mat, 0.5f, 0.5f, r, g, b, alpha, 1.0f, 1.0f);
        writeCrossFlashVertex(builder, mat, 0.5f, -0.5f, r, g, b, alpha, 1.0f, 0.0f);
    }

    private void writeCrossFlashVertex(VertexConsumer builder, Matrix4f mat,
            float x, float y, float r, float g, float b, float alpha, float u, float v) {
        builder.vertex(mat, x, y, 0.0f)
                .color(r, g, b, alpha)
                .uv(u, v)
                .endVertex();
    }
}
