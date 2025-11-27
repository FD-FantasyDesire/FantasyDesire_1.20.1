package tennouboshiuzume.mods.FantasyDesire.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import mods.flammpfeil.slashblade.client.renderer.util.MSAutoCloser;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDEnergyBullet;

import java.awt.*;
import java.util.List;
import java.util.Random;

@SuppressWarnings("removal")
@OnlyIn(Dist.CLIENT)
public class FDEnergyBulletRender <T extends EntityFDEnergyBullet> extends EntityRenderer<T> {
    private static final ResourceLocation modelLocation = new ResourceLocation("slashblade", "model/util/slashdim.obj");
    private static final ResourceLocation textureLocation = new ResourceLocation("slashblade", "model/util/slashdim.png");


    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T entity) {
        return textureLocation;
    }

    public FDEnergyBulletRender(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack matrixStackIn,
                       @NotNull MultiBufferSource bufferIn, int packedLightIn) {

        try (MSAutoCloser msac = MSAutoCloser.pushMatrix(matrixStackIn)) {

            if (entity.getFired() && entity.getHasTail()) {
                matrixStackIn.pushPose();
                renderTrail(entity, partialTicks, matrixStackIn, bufferIn, packedLightIn);
                matrixStackIn.popPose();
            }

            matrixStackIn
                    .mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
            matrixStackIn.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));

            WavefrontObject model = BladeModelManager.getInstance().getModel(modelLocation);

            int lifetime = entity.getDelay();

            double deathTime = lifetime;
            // double baseAlpha = Math.sin(Math.PI * 0.5 * (Math.min(deathTime, Math.max(0,
            // (lifetime - (entity.ticksExisted) - partialTicks))) / deathTime));
            double baseAlpha = (Math.min(deathTime, Math.max(0, (lifetime - (entity.tickCount) - partialTicks)))
                    / deathTime);
            baseAlpha = -Math.pow(baseAlpha - 1, 4.0) + 1.0;
            Random random = new Random();
            int seed = random.nextInt(360);

            matrixStackIn.mulPose(Axis.YP.rotationDegrees(seed));
            float globalScale = entity.getScale();
            float scale = 0.01f * globalScale;
            matrixStackIn.scale(scale, scale, scale);

            int color = entity.getColor() & 0xFFFFFF;
            Color col = new Color(color);
            float[] hsb = Color.RGBtoHSB(col.getRed(), col.getGreen(), col.getBlue(), null);
            int baseColor = Color.HSBtoRGB(0.5f + hsb[0], hsb[1], 0.2f/* hsb[2] */) & 0xFFFFFF;

            try (MSAutoCloser msacB = MSAutoCloser.pushMatrix(matrixStackIn)) {
                for (int l = 0; l < 5; l++) {
                    float innerScale = 0.95f;
                    matrixStackIn.scale(innerScale, innerScale, innerScale);

                    BladeRenderState.setCol(baseColor | ((0xFF & (int) (0x66 * baseAlpha)) << 24));
                    BladeRenderState.renderOverridedReverseLuminous(ItemStack.EMPTY, model, "base",
                            this.getTextureLocation(entity), matrixStackIn, bufferIn, packedLightIn);
                }
            }

            int loop = 3;
            for (int l = 0; l < loop; l++) {
                try (MSAutoCloser msacB = MSAutoCloser.pushMatrix(matrixStackIn)) {
                    float cycleTicks = 15;
                    float wave = (entity.tickCount + (cycleTicks / (float) loop * l) + partialTicks) % cycleTicks;
                    float waveScale = 1.0f + 0.03f * wave;
                    matrixStackIn.scale(waveScale, waveScale, waveScale);

                    BladeRenderState
                            .setCol(baseColor | ((int) (0x88 * ((cycleTicks - wave) / cycleTicks) * baseAlpha) << 24));
                    BladeRenderState.renderOverridedReverseLuminous(ItemStack.EMPTY, model, "base",
                            this.getTextureLocation(entity), matrixStackIn, bufferIn, packedLightIn);
                }
            }

            int windCount = 5;
            for (int l = 0; l < windCount; l++) {
                try (MSAutoCloser msacB = MSAutoCloser.pushMatrix(matrixStackIn)) {

                    matrixStackIn.mulPose(Axis.XP.rotationDegrees((360.0f / windCount) * l));
                    matrixStackIn.mulPose(Axis.YP.rotationDegrees(30.0f));

                    double rotWind = 360.0 / 20.0;

                    double offsetBase = 7;

                    double offset = l * offsetBase;

                    double motionLen = offsetBase * (windCount - 1);

                    double ticks = entity.tickCount + partialTicks + seed;
                    double offsetTicks = ticks + offset;
                    double progress = (offsetTicks % motionLen) / motionLen;

                    double rad = (Math.PI) * 2.0;
                    rad *= progress;

                    float windScale = (float) (0.4 + progress);
                    matrixStackIn.scale(windScale, windScale, windScale);

                    matrixStackIn.mulPose(Axis.ZP.rotationDegrees((float) (rotWind * offsetTicks)));

                    Color cc = new Color(col.getRed(), col.getGreen(), col.getBlue(),
                            0xff & (int) (Math.min(0, 0xFF * Math.sin(rad) * baseAlpha)));
                    BladeRenderState.setCol(cc);
                    BladeRenderState.renderOverridedColorWrite(ItemStack.EMPTY, model, "wind",
                            this.getTextureLocation(entity), matrixStackIn, bufferIn, BladeRenderState.MAX_LIGHT);
                }
            }
        }
    }
    private void renderTrail(T entity, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn, int packedLightIn) {
        List<Vec3> trail = entity.getTrailPositions();
        if (trail == null || trail.size() < 2) return;
        ResourceLocation tex = getTextureLocation(entity);
        // Ensure we get a VertexConsumer to draw the trail and core highlight
        VertexConsumer builder = bufferIn.getBuffer(RenderType.entityTranslucent(tex));
        // Use eyes() render type for additive/emissive (glow) effect for the inner core
        VertexConsumer coreBuilder = bufferIn.getBuffer(RenderType.eyes(tex));
        // partialTicks intentionally unused here

        Vec3 camPos = this.entityRenderDispatcher.camera.getPosition();
        Vec3 entityPos = entity.position();

        int count = trail.size();
        float baseSize = 0.2f * entity.getScale(); // 基础宽度（full width）

        int hexColor = entity.getColor();
        int colorR = (hexColor >> 16) & 0xFF;
        int colorG = (hexColor >> 8) & 0xFF;
        int colorB = hexColor & 0xFF;

        final float sharpenStart = 0.75f; // 尾端从 75% 开始尖化

        // 预计算每个点的平滑切向（tangent），使用前后点平均
        Vec3[] tangents = new Vec3[count];
        for (int i = 0; i < count; i++) {
            Vec3 prev = trail.get(Math.max(0, i - 1));
            Vec3 next = trail.get(Math.min(count - 1, i + 1));
            Vec3 t = next.subtract(prev);
            double tlen = t.length();
            if (tlen <= 1e-6) {
                // fallback to forward direction
                if (i < count - 1) t = trail.get(i + 1).subtract(trail.get(i));
                else t = trail.get(i).subtract(trail.get(i - 1));
            }
            tangents[i] = t.normalize();
        }

        // 绘制每段：外层 + 内层高亮
        for (int i = 0; i < count - 1; i++) {
            Vec3 p0 = trail.get(i);
            Vec3 p1 = trail.get(i + 1);
            Vec3 r0 = p0.subtract(entityPos);
            Vec3 r1 = p1.subtract(entityPos);

            // 使用两端的切向平均以获得段切向（更平滑）
            Vec3 tan0 = tangents[i];
            Vec3 tan1 = tangents[i + 1];
            Vec3 segTan = tan0.add(tan1).scale(0.5);
            if (segTan.length() <= 1e-6) segTan = p1.subtract(p0).normalize();
            else segTan = segTan.normalize();

            // 计算右向量 = (cameraPos - point) x tangent
            Vec3 viewDir = camPos.subtract(entityPos).subtract(r0);
            Vec3 right = viewDir.cross(segTan);
            if (right.length() <= 1e-6) {
                // fallback to camera orientation right vector
                Quaternionf camOrient = this.entityRenderDispatcher.cameraOrientation();
                org.joml.Vector3f tmp = new org.joml.Vector3f(1f, 0f, 0f);
                camOrient.transform(tmp);
                right = new Vec3(tmp.x(), tmp.y(), tmp.z());
            }
            right = right.normalize();

            // t along trail (0 newest -> 1 oldest)
            float t0 = (float) i / (float) Math.max(1, count - 1);
            float t1 = (float) (i + 1) / (float) Math.max(1, count - 1);

            // widths
            float outerHalf0 = baseSize * (1.0f - t0 * 0.7f) * 0.5f;
            float outerHalf1 = baseSize * (1.0f - t1 * 0.7f) * 0.5f;
            float innerHalf0 = outerHalf0 * 0.4f; // highlight is narrower
            float innerHalf1 = outerHalf1 * 0.4f;

            // sharpening at tail
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

            // compute vertex positions
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

            // colors: outer fades toward white and alpha drops; inner is brighter (closer to white) and more opaque
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

            float lerpInner0 = t0 * 0.15f; // inner less desaturated
            float lerpInner1 = t1 * 0.15f;
            float ir0 = (colorR / 255f) * (1f - lerpInner0) + lerpInner0;
            float ig0 = (colorG / 255f) * (1f - lerpInner0) + lerpInner0;
            float ib0 = (colorB / 255f) * (1f - lerpInner0) + lerpInner0;
            float ir1 = (colorR / 255f) * (1f - lerpInner1) + lerpInner1;
            float ig1 = (colorG / 255f) * (1f - lerpInner1) + lerpInner1;
            float ib1 = (colorB / 255f) * (1f - lerpInner1) + lerpInner1;
            float iAlpha0 = (1f - t0) * 0.95f;
            float iAlpha1 = (1f - t1) * 0.75f;
            // Boost inner color to make it glow (then clamp to [0,1])
            final float glowBoost = 1.4f;
            ir0 = Mth.clamp(ir0 * glowBoost, 0f, 1f);
            ig0 = Mth.clamp(ig0 * glowBoost, 0f, 1f);
            ib0 = Mth.clamp(ib0 * glowBoost, 0f, 1f);
            ir1 = Mth.clamp(ir1 * glowBoost, 0f, 1f);
            ig1 = Mth.clamp(ig1 * glowBoost, 0f, 1f);
            ib1 = Mth.clamp(ib1 * glowBoost, 0f, 1f);
            // Optionally increase alpha a bit for inner core
            iAlpha0 = Mth.clamp(iAlpha0 * 1.1f, 0f, 1f);
            iAlpha1 = Mth.clamp(iAlpha1 * 1.1f, 0f, 1f);

            // draw outer quad (two triangles) v0a v0b v1b v1a
            builder.vertex(mat, (float) o0a.x, (float) o0a.y, (float) o0a.z)
                    .color((int) (or0 * 255), (int) (og0 * 255), (int) (ob0 * 255), (int) (oAlpha0 * 255))
                    .uv(0f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(normal, 0f, 0f, 1f).endVertex();
            builder.vertex(mat, (float) o0b.x, (float) o0b.y, (float) o0b.z)
                    .color((int) (or0 * 255), (int) (og0 * 255), (int) (ob0 * 255), (int) (oAlpha0 * 255))
                    .uv(0f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(normal, 0f, 0f, 1f).endVertex();
            builder.vertex(mat, (float) o1b.x, (float) o1b.y, (float) o1b.z)
                    .color((int) (or1 * 255), (int) (og1 * 255), (int) (ob1 * 255), (int) (oAlpha1 * 255))
                    .uv(1f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(normal, 0f, 0f, 1f).endVertex();
            builder.vertex(mat, (float) o1a.x, (float) o1a.y, (float) o1a.z)
                    .color((int) (or1 * 255), (int) (og1 * 255), (int) (ob1 * 255), (int) (oAlpha1 * 255))
                    .uv(1f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(normal, 0f, 0f, 1f).endVertex();

            // draw inner highlight quad (narrower)
            coreBuilder.vertex(mat, (float) c0a.x, (float) c0a.y, (float) c0a.z)
                    .color((int) (ir0 * 255), (int) (ig0 * 255), (int) (ib0 * 255), (int) (iAlpha0 * 255))
                    .uv(0f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(normal, 0f, 0f, 1f).endVertex();
            coreBuilder.vertex(mat, (float) c0b.x, (float) c0b.y, (float) c0b.z)
                    .color((int) (ir0 * 255), (int) (ig0 * 255), (int) (ib0 * 255), (int) (iAlpha0 * 255))
                    .uv(0f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(normal, 0f, 0f, 1f).endVertex();
            coreBuilder.vertex(mat, (float) c1b.x, (float) c1b.y, (float) c1b.z)
                    .color((int) (ir1 * 255), (int) (ig1 * 255), (int) (ib1 * 255), (int) (iAlpha1 * 255))
                    .uv(1f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(normal, 0f, 0f, 1f).endVertex();
            coreBuilder.vertex(mat, (float) c1a.x, (float) c1a.y, (float) c1a.z)
                    .color((int) (ir1 * 255), (int) (ig1 * 255), (int) (ib1 * 255), (int) (iAlpha1 * 255))
                    .uv(1f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn).normal(normal, 0f, 0f, 1f).endVertex();
        }
    }
}
