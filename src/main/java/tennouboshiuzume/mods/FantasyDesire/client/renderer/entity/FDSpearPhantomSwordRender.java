package tennouboshiuzume.mods.FantasyDesire.client.renderer.entity;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import tennouboshiuzume.mods.FantasyDesire.client.FDShaderHandler;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDSpearPhantomSword;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class FDSpearPhantomSwordRender<T extends EntityFDSpearPhantomSword> extends EntityRenderer<T> {

    private static boolean missingCrossFlashShaderLogged = false;

    private static final RenderType CROSS_FLASH_RENDER_TYPE = RenderType.create(
            "fd_spear_cross_flash",
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

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return entity.getTextureLoc();
    }

    public FDSpearPhantomSwordRender(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn,
            int packedLightIn) {

        // ===== 十字星型闪光特效（客户端） =====
        if (entity.getFlashTicks() > 0) {
            matrixStack.pushPose();
            renderCrossFlash(entity, partialTicks, matrixStack, bufferIn, packedLightIn);
            matrixStack.popPose();
        }

        if ((entity.getFired() || entity.getForceTail()) && entity.getHasTail()) {
            matrixStack.pushPose();
            renderTrail(entity, partialTicks, matrixStack, bufferIn, packedLightIn);
            matrixStack.popPose();
        }

        matrixStack.pushPose();
        Entity shooter = entity.getShooter();
        if (shooter != null && entity
                .getStandbyMode() == tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword.StandbyMode.PLAYER
                && !entity.getFired()) {
            double sX = Mth.lerp(partialTicks, shooter.xo, shooter.getX());
            double sY = Mth.lerp(partialTicks, shooter.yo, shooter.getY());
            double sZ = Mth.lerp(partialTicks, shooter.zo, shooter.getZ());

            float sYaw = Mth.rotLerp(partialTicks, shooter.yRotO, shooter.getYRot());
            float sPitch = Mth.rotLerp(partialTicks, shooter.xRotO, shooter.getXRot());

            Vec3 offset = entity.getOffset()
                    .xRot((float) Math.toRadians(-sPitch))
                    .yRot((float) Math.toRadians(-sYaw));
            Vec3 pos = new Vec3(sX, sY, sZ).add(entity.getCenterOffset()).add(offset);

            double eX = Mth.lerp(partialTicks, entity.xo, entity.getX());
            double eY = Mth.lerp(partialTicks, entity.yo, entity.getY());
            double eZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());

            matrixStack.translate(pos.x - eX, pos.y - eY, pos.z - eZ);
        }

        try {

            Entity hits = entity.getHitEntity();
            boolean hasHitEntity = hits != null;

            if (hasHitEntity) {
                matrixStack
                        .mulPose(Axis.YN.rotationDegrees(Mth.rotLerp(partialTicks, hits.yRotO, hits.getYRot()) - 90));
                matrixStack.mulPose(Axis.YN.rotationDegrees(entity.getOffsetYaw()));
            } else {
                matrixStack.mulPose(
                        Axis.YP.rotationDegrees(Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
            }

            matrixStack.mulPose(Axis.ZP.rotationDegrees(Mth.rotLerp(partialTicks, entity.xRotO, entity.getXRot())));

            matrixStack.mulPose(Axis.XP.rotationDegrees(entity.getRoll()));

            float scale = 0.0075f * entity.getScale();
            matrixStack.scale(scale, scale, scale);
            matrixStack.mulPose(Axis.YP.rotationDegrees(90.0F));

            if (hasHitEntity) {
                matrixStack.translate(0, 0, -100);
            }

            WavefrontObject model = BladeModelManager.getInstance().getModel(entity.getModelLoc());
            BladeRenderState.setCol(entity.getColor(), false);
            BladeRenderState.renderOverridedLuminous(ItemStack.EMPTY, model, "spear", getTextureLocation(entity),
                    matrixStack, bufferIn, packedLightIn);
        } finally {
            matrixStack.popPose();
        }

    }

    private void renderTrail(T entity, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn,
            int packedLightIn) {
        List<Vec3> trail = entity.getTrailPositions();
        if (trail == null || trail.size() < 2)
            return;
        ResourceLocation tex = getTextureLocation(entity);
        VertexConsumer builder = bufferIn.getBuffer(RenderType.entityTranslucent(tex));
        VertexConsumer coreBuilder = bufferIn.getBuffer(RenderType.eyes(tex));

        Vec3 camPos = this.entityRenderDispatcher.camera.getPosition();
        double lerpX = Mth.lerp(partialTicks, entity.xo, entity.getX());
        double lerpY = Mth.lerp(partialTicks, entity.yo, entity.getY());
        double lerpZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());
        Vec3 entityPos = new Vec3(lerpX, lerpY, lerpZ);

        List<Vec3> points = new java.util.ArrayList<>();
        points.add(entityPos);
        if (trail.size() > 1) {
            points.addAll(trail.subList(1, trail.size()));
        }

        int count = points.size();
        float baseSize = 0.1f * entity.getScale();
        double offsetDist = 0.5 * entity.getScale();

        int hexColor = entity.getColor();
        int colorR = (hexColor >> 16) & 0xFF;
        int colorG = (hexColor >> 8) & 0xFF;
        int colorB = hexColor & 0xFF;

        final float sharpenStart = 0.75f;

        Vec3[] tangents = new Vec3[count];
        for (int i = 0; i < count; i++) {
            Vec3 prev = points.get(Math.max(0, i - 1));
            Vec3 next = points.get(Math.min(count - 1, i + 1));
            Vec3 t = next.subtract(prev);
            double tlen = t.length();
            if (tlen <= 1e-6) {
                if (i < count - 1)
                    t = points.get(i + 1).subtract(points.get(i));
                else
                    t = points.get(i).subtract(points.get(i - 1));
            }
            tangents[i] = t.normalize();
        }

        for (int i = 0; i < count - 1; i++) {
            Vec3 p0 = points.get(i);
            Vec3 p1 = points.get(i + 1);

            p0 = p0.add(tangents[i].scale(offsetDist));
            p1 = p1.add(tangents[i + 1].scale(offsetDist));

            Vec3 r0 = p0.subtract(entityPos);
            Vec3 r1 = p1.subtract(entityPos);

            Vec3 tan0 = tangents[i];
            Vec3 tan1 = tangents[i + 1];
            Vec3 segTan = tan0.add(tan1).scale(0.5);
            if (segTan.length() <= 1e-6)
                segTan = p1.subtract(p0).normalize();
            else
                segTan = segTan.normalize();

            Vec3 viewDir = camPos.subtract(entityPos).subtract(r0);
            Vec3 right = viewDir.cross(segTan);
            if (right.length() <= 1e-6) {
                Quaternionf camOrient = this.entityRenderDispatcher.cameraOrientation();
                org.joml.Vector3f tmp = new org.joml.Vector3f(1f, 0f, 0f);
                camOrient.transform(tmp);
                right = new Vec3(tmp.x(), tmp.y(), tmp.z());
            }
            right = right.normalize();

            float t0 = (float) i / (float) Math.max(1, count - 1);
            float t1 = (float) (i + 1) / (float) Math.max(1, count - 1);

            float outerHalf0 = baseSize * (1.0f - t0 * 0.7f) * 0.5f;
            float outerHalf1 = baseSize * (1.0f - t1 * 0.7f) * 0.5f;
            float innerHalf0 = outerHalf0 * 0.4f;
            float innerHalf1 = outerHalf1 * 0.4f;

            if (t1 >= sharpenStart) {
                float s = (t1 - sharpenStart) / (1f - sharpenStart);
                s = Mth.clamp(s, 0f, 1f);
                outerHalf1 *= (1f - s);
                innerHalf1 *= (1f - s);
            }
            if (t0 >= sharpenStart) {
                float s = (t0 - sharpenStart) / (1f - sharpenStart);
                s = Mth.clamp(s, 0f, 1f);
                outerHalf0 *= (1f - s);
                innerHalf0 *= (1f - s);
            }

            Vec3 o0a = r0.add(right.scale(outerHalf0));
            Vec3 o0b = r0.subtract(right.scale(outerHalf0));
            Vec3 o1a = r1.add(right.scale(outerHalf1));
            Vec3 o1b = r1.subtract(right.scale(outerHalf1));

            Vec3 c0a = r0.add(right.scale(innerHalf0));
            Vec3 c0b = r0.subtract(right.scale(innerHalf0));
            Vec3 c1a = r1.add(right.scale(innerHalf1));
            Vec3 c1b = r1.subtract(right.scale(innerHalf1));

            Matrix4f mat = matrixStack.last().pose();
            Matrix3f normal = matrixStack.last().normal();

            float lerpOuter0 = t0 * 0.35f;
            float lerpOuter1 = t1 * 0.35f;
            float or0 = (colorR / 255f) * (1f - lerpOuter0) + lerpOuter0;
            float og0 = (colorG / 255f) * (1f - lerpOuter0) + lerpOuter0;
            float ob0 = (colorB / 255f) * (1f - lerpOuter0) + lerpOuter0;
            float or1 = (colorR / 255f) * (1f - lerpOuter1) + lerpOuter1;
            float og1 = (colorG / 255f) * (1f - lerpOuter1) + lerpOuter1;
            float ob1 = (colorB / 255f) * (1f - lerpOuter1) + lerpOuter1;
            float oAlpha0 = (1f - t0) * 0.85f;
            float oAlpha1 = (1f - t1) * 0.65f;
            oAlpha0 *= 1f;
            oAlpha1 *= 1f;

            float lerpInner0 = t0 * 0.15f;
            float lerpInner1 = t1 * 0.15f;
            float ir0 = (colorR / 255f) * (1f - lerpInner0) + lerpInner0;
            float ig0 = (colorG / 255f) * (1f - lerpInner0) + lerpInner0;
            float ib0 = (colorB / 255f) * (1f - lerpInner0) + lerpInner0;
            float ir1 = (colorR / 255f) * (1f - lerpInner1) + lerpInner1;
            float ig1 = (colorG / 255f) * (1f - lerpInner1) + lerpInner1;
            float ib1 = (colorB / 255f) * (1f - lerpInner1) + lerpInner1;
            float iAlpha0 = (1f - t0) * 0.95f;
            float iAlpha1 = (1f - t1) * 0.75f;
            final float glowBoost = 1.4f;
            ir0 = Mth.clamp(ir0 * glowBoost, 0f, 1f);
            ig0 = Mth.clamp(ig0 * glowBoost, 0f, 1f);
            ib0 = Mth.clamp(ib0 * glowBoost, 0f, 1f);
            ir1 = Mth.clamp(ir1 * glowBoost, 0f, 1f);
            ig1 = Mth.clamp(ig1 * glowBoost, 0f, 1f);
            ib1 = Mth.clamp(ib1 * glowBoost, 0f, 1f);
            iAlpha0 = Mth.clamp(iAlpha0 * 1.1f, 0f, 1f);
            iAlpha1 = Mth.clamp(iAlpha1 * 1.1f, 0f, 1f);

            builder.vertex(mat, (float) o0a.x, (float) o0a.y, (float) o0a.z)
                    .color((int) (or0 * 255), (int) (og0 * 255), (int) (ob0 * 255), (int) (oAlpha0 * 255))
                    .uv(0f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(normal, 0f, 0f, 1f)
                    .endVertex();
            builder.vertex(mat, (float) o0b.x, (float) o0b.y, (float) o0b.z)
                    .color((int) (or0 * 255), (int) (og0 * 255), (int) (ob0 * 255), (int) (oAlpha0 * 255))
                    .uv(0f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(normal, 0f, 0f, 1f)
                    .endVertex();
            builder.vertex(mat, (float) o1b.x, (float) o1b.y, (float) o1b.z)
                    .color((int) (or1 * 255), (int) (og1 * 255), (int) (ob1 * 255), (int) (oAlpha1 * 255))
                    .uv(1f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(normal, 0f, 0f, 1f)
                    .endVertex();
            builder.vertex(mat, (float) o1a.x, (float) o1a.y, (float) o1a.z)
                    .color((int) (or1 * 255), (int) (og1 * 255), (int) (ob1 * 255), (int) (oAlpha1 * 255))
                    .uv(1f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(normal, 0f, 0f, 1f)
                    .endVertex();

            coreBuilder.vertex(mat, (float) c0a.x, (float) c0a.y, (float) c0a.z)
                    .color((int) (ir0 * 255), (int) (ig0 * 255), (int) (ib0 * 255), (int) (iAlpha0 * 255))
                    .uv(0f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(normal, 0f, 0f, 1f)
                    .endVertex();
            coreBuilder.vertex(mat, (float) c0b.x, (float) c0b.y, (float) c0b.z)
                    .color((int) (ir0 * 255), (int) (ig0 * 255), (int) (ib0 * 255), (int) (iAlpha0 * 255))
                    .uv(0f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(normal, 0f, 0f, 1f)
                    .endVertex();
            coreBuilder.vertex(mat, (float) c1b.x, (float) c1b.y, (float) c1b.z)
                    .color((int) (ir1 * 255), (int) (ig1 * 255), (int) (ib1 * 255), (int) (iAlpha1 * 255))
                    .uv(1f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(normal, 0f, 0f, 1f)
                    .endVertex();
            coreBuilder.vertex(mat, (float) c1a.x, (float) c1a.y, (float) c1a.z)
                    .color((int) (ir1 * 255), (int) (ig1 * 255), (int) (ib1 * 255), (int) (iAlpha1 * 255))
                    .uv(1f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(normal, 0f, 0f, 1f)
                    .endVertex();
        }
    }

    private void renderCrossFlash(T entity, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn,
            int packedLightIn) {
        int flashTicks = entity.getFlashTicks();
        if (flashTicks <= 0)
            return;

        if (!FDShaderHandler.isCrossFlashShaderLoaded()) {
            if (!missingCrossFlashShaderLogged) {
                System.err.println(
                        "[FantasyDesire] Cross flash shader is not loaded; skipping spear flash render.");
                missingCrossFlashShaderLogged = true;
            }
            return;
        }

        float progress = Mth.clamp(1.0f - (flashTicks - partialTicks) / 15.0f, 0.0f, 1.0f);
        float fadeIn = Mth.clamp(progress / 0.12f, 0.0f, 1.0f);
        float fadeOut = 1.0f - Mth.clamp((progress - 0.38f) / 0.62f, 0.0f, 1.0f);
        float alpha = fadeIn * fadeOut;
        if (alpha <= 0.01f)
            return;

        float burst = 1.0f + 0.55f * (float) Math.sin(Math.PI * Mth.clamp(progress / 0.28f, 0.0f, 1.0f));
        float settle = Mth.lerp(progress, 1.0f, 0.68f);
        float finalScale = 3.25f * entity.getScale() * burst * settle;

        int hexColor = entity.getColor();
        float r = Math.max(((hexColor >> 16) & 0xFF) / 255.0f, 0.18f);
        float g = Math.max(((hexColor >> 8) & 0xFF) / 255.0f, 0.18f);
        float b = Math.max((hexColor & 0xFF) / 255.0f, 0.18f);

        matrixStack.pushPose();
        matrixStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        matrixStack.scale(finalScale, finalScale, finalScale);

        emitCrossFlashQuad(bufferIn.getBuffer(CROSS_FLASH_RENDER_TYPE), matrixStack.last().pose(), r, g, b, alpha);

        matrixStack.popPose();
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
