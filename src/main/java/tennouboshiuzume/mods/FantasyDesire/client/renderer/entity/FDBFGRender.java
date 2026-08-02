package tennouboshiuzume.mods.FantasyDesire.client.renderer.entity;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import mods.flammpfeil.slashblade.client.renderer.util.MSAutoCloser;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
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
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDBFG;

import java.awt.*;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import tennouboshiuzume.mods.FantasyDesire.client.FDShaderHandler;

@SuppressWarnings("removal")
@OnlyIn(Dist.CLIENT)
public class FDBFGRender<T extends EntityFDBFG> extends EntityRenderer<T> {
        private static final ResourceLocation modelLocation = new ResourceLocation("slashblade",
                        "model/util/slashdim.obj");
        private static final ResourceLocation textureLocation = new ResourceLocation("slashblade",
                        "model/util/slashdim.png");
        private static final ResourceLocation ENERGY_TEX = new ResourceLocation("minecraft",
                        "textures/entity/creeper/creeper_armor.png");
        private static boolean missingCoronaShaderLogged = false;
        private static boolean missingBridgeShaderLogged = false;

        private static final RenderType BFG_CORONA_RENDER_TYPE = RenderType.create(
                        "fd_bfg_corona_plasma",
                        DefaultVertexFormat.POSITION_COLOR_TEX,
                        VertexFormat.Mode.QUADS,
                        512,
                        false,
                        true,
                        RenderType.CompositeState.builder()
                                        .setShaderState(new RenderStateShard.ShaderStateShard(
                                                        FDShaderHandler::getBfgCoronaShader))
                                        .setTextureState(new RenderStateShard.TextureStateShard(
                                                        TextureManager.INTENTIONAL_MISSING_TEXTURE, false, false))
                                        .setTransparencyState(new RenderStateShard.TransparencyStateShard(
                                                        "fd_bfg_corona_additive_transparency",
                                                        () -> {
                                                                RenderSystem.enableBlend();
                                                                RenderSystem.blendFunc(
                                                                                GlStateManager.SourceFactor.SRC_ALPHA,
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

        private static final RenderType BFG_BRIDGE_RENDER_TYPE = RenderType.create(
                        "fd_bfg_bridge_plasma",
                        DefaultVertexFormat.POSITION_COLOR_TEX,
                        VertexFormat.Mode.QUADS,
                        1024,
                        false,
                        true,
                        RenderType.CompositeState.builder()
                                        .setShaderState(new RenderStateShard.ShaderStateShard(
                                                        FDShaderHandler::getBfgBridgeShader))
                                        .setTextureState(new RenderStateShard.TextureStateShard(
                                                        TextureManager.INTENTIONAL_MISSING_TEXTURE, false, false))
                                        .setTransparencyState(new RenderStateShard.TransparencyStateShard(
                                                        "fd_bfg_bridge_additive_transparency",
                                                        () -> {
                                                                RenderSystem.enableBlend();
                                                                RenderSystem.blendFunc(
                                                                                GlStateManager.SourceFactor.SRC_ALPHA,
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
        public @NotNull ResourceLocation getTextureLocation(@NotNull T entity) {
                return textureLocation;
        }

        public FDBFGRender(EntityRendererProvider.Context context) {
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
                                        .mulPose(Axis.YP.rotationDegrees(
                                                        Mth.lerp(partialTicks, entity.yRotO, entity.getYRot())
                                                                        - 90.0F));
                        matrixStackIn.mulPose(Axis.ZP
                                        .rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));

                        WavefrontObject model = BladeModelManager.getInstance().getModel(modelLocation);

                        int lifetime = entity.getDelay();

                        double deathTime = lifetime;
                        // double baseAlpha = Math.sin(Math.PI * 0.5 * (Math.min(deathTime, Math.max(0,
                        // (lifetime - (entity.ticksExisted) - partialTicks))) / deathTime));
                        double baseAlpha = (Math.min(deathTime,
                                        Math.max(0, (lifetime - (entity.tickCount) - partialTicks)))
                                        / deathTime);
                        baseAlpha = -Math.pow(baseAlpha - 1, 4.0) + 1.0;
                        int seed = Math.floorMod(entity.getId() * 47, 360);

                        matrixStackIn.mulPose(Axis.YP.rotationDegrees(seed));
                        float globalScale = entity.getScale();
                        float scale = 0.01f * globalScale;
                        matrixStackIn.scale(scale, scale, scale);

                        int color = entity.getColor() & 0xFFFFFF;
                        Color col = new Color(color);
                        float[] hsb = Color.RGBtoHSB(col.getRed(), col.getGreen(), col.getBlue(), null);
                        int baseColor = Color.HSBtoRGB(0.5f + hsb[0], hsb[1], 0.2f/* hsb[2] */) & 0xFFFFFF;

                        try (MSAutoCloser msacB = MSAutoCloser.pushMatrix(matrixStackIn)) {
                                for (int l = 0; l < 3; l++) {
                                        float innerScale = 0.82f;
                                        matrixStackIn.scale(innerScale, innerScale, innerScale);

                                        BladeRenderState.setCol(baseColor | ((0xFF & (int) (0x42 * baseAlpha)) << 24));
                                        BladeRenderState.renderOverridedReverseLuminous(ItemStack.EMPTY, model, "base",
                                                        this.getTextureLocation(entity), matrixStackIn, bufferIn,
                                                        packedLightIn);
                                }
                        }

                        int loop = 2;
                        for (int l = 0; l < loop; l++) {
                                try (MSAutoCloser msacB = MSAutoCloser.pushMatrix(matrixStackIn)) {
                                        float cycleTicks = 15;
                                        float wave = (entity.tickCount + (cycleTicks / (float) loop * l) + partialTicks)
                                                        % cycleTicks;
                                        float waveScale = 0.72f + 0.012f * wave;
                                        matrixStackIn.scale(waveScale, waveScale, waveScale);

                                        BladeRenderState
                                                        .setCol(baseColor | ((int) (0x38
                                                                        * ((cycleTicks - wave) / cycleTicks)
                                                                        * baseAlpha) << 24));
                                        BladeRenderState.renderOverridedReverseLuminous(ItemStack.EMPTY, model, "base",
                                                        this.getTextureLocation(entity), matrixStackIn, bufferIn,
                                                        packedLightIn);
                                }
                        }

                        /*
                         * int windCount = 5;
                         * for (int l = 0; l < windCount; l++) {
                         * try (MSAutoCloser msacB = MSAutoCloser.pushMatrix(matrixStackIn)) {
                         * 
                         * matrixStackIn.mulPose(Axis.XP.rotationDegrees((360.0f / windCount) * l));
                         * matrixStackIn.mulPose(Axis.YP.rotationDegrees(30.0f));
                         * 
                         * double rotWind = 360.0 / 20.0;
                         * 
                         * double offsetBase = 7;
                         * 
                         * double offset = l * offsetBase;
                         * 
                         * double motionLen = offsetBase * (windCount - 1);
                         * 
                         * double ticks = entity.tickCount + partialTicks + seed;
                         * double offsetTicks = ticks + offset;
                         * double progress = (offsetTicks % motionLen) / motionLen;
                         * 
                         * double rad = (Math.PI) * 2.0;
                         * rad *= progress;
                         * 
                         * float windScale = (float) (0.4 + progress);
                         * matrixStackIn.scale(windScale, windScale, windScale);
                         * 
                         * matrixStackIn.mulPose(Axis.ZP.rotationDegrees((float) (rotWind *
                         * offsetTicks)));
                         * 
                         * Color cc = new Color(col.getRed(), col.getGreen(), col.getBlue(),
                         * 0xff & (int) (Math.min(0, 0xFF * Math.sin(rad) * baseAlpha)));
                         * BladeRenderState.setCol(cc);
                         * BladeRenderState.renderOverridedColorWrite(ItemStack.EMPTY, model, "wind",
                         * this.getTextureLocation(entity), matrixStackIn, bufferIn,
                         * BladeRenderState.MAX_LIGHT);
                         * }
                         * }
                         */
                }

                renderBFGEffects(entity, partialTicks, matrixStackIn, bufferIn, packedLightIn);
        }

        private void renderBFGEffects(T entity, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn,
                        int packedLightIn) {
                Vec3 entityPos = new Vec3(
                                Mth.lerp(partialTicks, entity.xo, entity.getX()),
                                Mth.lerp(partialTicks, entity.yo, entity.getY()),
                                Mth.lerp(partialTicks, entity.zo, entity.getZ()));
                Vec3 camPos = this.entityRenderDispatcher.camera.getPosition();

                float time = entity.tickCount + partialTicks;

                renderCoronaPlasmaCore(entity, partialTicks, matrixStack, bufferIn, packedLightIn);

                renderMagneticFieldArcs(entity, time, matrixStack, bufferIn);

                if (entity.clientTargets != null && !entity.clientTargets.isEmpty()) {
                        for (LivingEntity target : entity.clientTargets) {
                                Vec3 targetPos = new Vec3(
                                                Mth.lerp(partialTicks, target.xo, target.getX()),
                                                Mth.lerp(partialTicks, target.yo, target.getY())
                                                                + target.getBbHeight() / 2,
                                                Mth.lerp(partialTicks, target.zo, target.getZ()));

                                renderPlasmaBridge(entity, target, time, matrixStack, bufferIn, camPos, entityPos,
                                                targetPos);
                                renderBridgeReleaseArcs(entity, target, time, matrixStack, bufferIn, camPos, entityPos,
                                                targetPos);
                        }
                }
        }

        private void renderMagneticFieldArcs(T entity, float time, PoseStack matrixStack, MultiBufferSource bufferIn) {
                VertexConsumer builder = bufferIn.getBuffer(RenderType.lightning());
                int[] arcColor = getBfgEffectChannels(entity, 0.16f);
                int arcCount = 26;
                for (int arc = 0; arc < arcCount; arc++) {
                        float seed = arc * 12.9898f + entity.getId() * 0.173f;
                        float radius = entity.getScale() * (0.52f + 0.36f * pseudoRandom(seed));
                        float arcDegrees = 18.0f + 50.0f * pseudoRandom(seed + 2.31f);
                        float startDegrees = 360.0f * pseudoRandom(seed + 5.17f)
                                        + time * (2.4f + 5.8f * pseudoRandom(seed + 7.73f));
                        float width = entity.getScale() * (0.009f + 0.018f * pseudoRandom(seed + 9.41f));
                        width *= 0.56f + 0.44f * Mth.sin(time * (0.055f + pseudoRandom(seed + 1.91f) * 0.045f) + seed);

                        matrixStack.pushPose();
                        matrixStack.mulPose(Axis.XP.rotationDegrees(360.0f * pseudoRandom(seed + 11.0f)));
                        matrixStack.mulPose(Axis.YP.rotationDegrees(360.0f * pseudoRandom(seed + 13.0f)
                                        + time * (-5.0f + 10.0f * pseudoRandom(seed + 14.0f))));
                        matrixStack.mulPose(Axis.ZP.rotationDegrees(360.0f * pseudoRandom(seed + 17.0f)));
                        Matrix4f mat = matrixStack.last().pose();
                        int segments = 5 + (int) (pseudoRandom(seed + 19.0f) * 7.0f);
                        for (int i = 0; i < segments; i++) {
                                float a0 = (float) Math.toRadians(startDegrees + arcDegrees * i / segments);
                                float a1 = (float) Math.toRadians(startDegrees + arcDegrees * (i + 1) / segments);
                                float wobble0 = (float) Math.sin(time * 0.16f + seed + i * 0.8f) * radius * 0.035f;
                                float wobble1 = (float) Math.sin(time * 0.16f + seed + (i + 1) * 0.8f) * radius
                                                * 0.035f;
                                Vec3 p0 = new Vec3(Math.cos(a0) * (radius + wobble0),
                                                Math.sin(a0 * 1.7f + seed) * radius * 0.10f,
                                                Math.sin(a0) * (radius - wobble0));
                                Vec3 p1 = new Vec3(Math.cos(a1) * (radius + wobble1),
                                                Math.sin(a1 * 1.7f + seed) * radius * 0.10f,
                                                Math.sin(a1) * (radius - wobble1));
                                int alpha = (int) (128 + 58 * Mth.sin(time * 0.05f + seed + i * 0.37f));
                                emitSegment(builder, mat, p0, p1, width, arcColor[0], arcColor[1], arcColor[2], alpha);
                        }
                        matrixStack.popPose();
                }
        }

        private void renderTendrilLayer(VertexConsumer builder, Matrix4f mat, Vec3 camPos, Vec3 entityPos, Vec3 diff,
                        int seed, float time, int segments, float width, int r, int g, int b, int alpha) {
                Vec3 prev = entityPos;
                for (int i = 0; i < segments; i++) {
                        float progress = (float) i / segments;
                        float nextProgress = (float) (i + 1) / segments;
                        Vec3 next = entityPos.add(diff.scale(nextProgress))
                                        .add(tendrilOffset(diff, seed, time, nextProgress));
                        if (i == 0) {
                                prev = entityPos.add(tendrilOffset(diff, seed, time, progress).scale(0.25));
                        }
                        Vec3 segmentDir = next.subtract(prev);
                        if (segmentDir.length() > 1e-6) {
                                Vec3 right = camPos.subtract(prev).cross(segmentDir).normalize().scale(width);
                                Vec3 start = prev.subtract(entityPos);
                                Vec3 end = next.subtract(entityPos);
                                emitQuad(builder, mat, start.add(right), start.subtract(right), end.subtract(right),
                                                end.add(right), r, g, b, alpha);
                        }
                        prev = next;
                }
        }

        private Vec3 tendrilOffset(Vec3 diff, int seed, float time, float progress) {
                Vec3 dir = diff.normalize();
                Vec3 axisA = dir.cross(new Vec3(0.0, 1.0, 0.0));
                if (axisA.length() <= 1e-6) {
                        axisA = dir.cross(new Vec3(1.0, 0.0, 0.0));
                }
                axisA = axisA.normalize();
                Vec3 axisB = dir.cross(axisA).normalize();
                double envelope = Math.sin(Math.PI * progress);
                double amp = Math.min(0.75, diff.length() * 0.075) * envelope;
                double phase = seed * 0.137 + time * 0.085;
                double waveA = Math.sin(progress * Math.PI * (2.1 + pseudoRandom(seed + 4.0f) * 1.7) + phase);
                double waveB = Math.sin(progress * Math.PI * (3.0 + pseudoRandom(seed + 8.0f) * 1.5) - phase * 1.27);
                return axisA.scale(waveA * amp).add(axisB.scale(waveB * amp * 0.72));
        }

        private void renderPlasmaBridge(T entity, LivingEntity target, float time, PoseStack matrixStack,
                        MultiBufferSource bufferIn, Vec3 camPos, Vec3 entityPos, Vec3 targetPos) {
                Vec3 diff = targetPos.subtract(entityPos);
                double dist = diff.length();
                if (dist <= 1e-4) {
                        return;
                }

                if (!FDShaderHandler.isBfgBridgeShaderLoaded()) {
                        if (!missingBridgeShaderLogged) {
                                System.err.println(
                                                "[FantasyDesire] BFG bridge shader is not loaded; using reduced lightning bridge fallback.");
                                missingBridgeShaderLogged = true;
                        }
                        renderReducedLegacyBridge(entity, target, time, matrixStack, bufferIn, camPos, entityPos,
                                        targetPos);
                        return;
                }

                ShaderInstance shader = FDShaderHandler.getBfgBridgeShader();
                setUniform(shader, "Time", time);
                setUniform(shader, "Opacity", 0.90f);
                setUniform(shader, "FlowSpeed", 0.18f);
                setUniform(shader, "LumpScale", 4.8f + (float) Math.min(dist * 0.18, 3.2));
                setUniform(shader, "FilamentIntensity", 0.86f);
                float[] effectColor = getBfgEffectColor(entity);
                float coreR = mixColor(effectColor[0], 1.0f, 0.72f);
                float coreG = mixColor(effectColor[1], 1.0f, 0.72f);
                float coreB = mixColor(effectColor[2], 0.96f, 0.72f);
                float edgeR = mixColor(effectColor[0], 1.0f, 0.30f);
                float edgeG = mixColor(effectColor[1], 1.0f, 0.30f);
                float edgeB = mixColor(effectColor[2], 1.0f, 0.30f);
                int edgeRi = toColorChannel(edgeR);
                int edgeGi = toColorChannel(edgeG);
                int edgeBi = toColorChannel(edgeB);
                setUniform(shader, "CoreColor", coreR, coreG, coreB, 1.0f);
                setUniform(shader, "FlowColor", effectColor[0], effectColor[1], effectColor[2], 1.0f);
                setUniform(shader, "EdgeColor", edgeR, edgeG, edgeB, 1.0f);

                VertexConsumer bridgeBuilder = bufferIn.getBuffer(BFG_BRIDGE_RENDER_TYPE);
                Matrix4f mat = matrixStack.last().pose();

                Vec3 dir = diff.normalize();
                Vec3 axisA = stablePerpendicular(dir);
                Vec3 axisB = dir.cross(axisA).normalize();
                int samples = Mth.clamp((int) (dist * 2.35), 8, 28);
                float scale = entity.getScale();
                int seed = entity.getId() * 31 + target.getId() * 17;

                Vec3 start = entityPos.subtract(dir.scale(scale * 0.030));
                Vec3 end = targetPos.add(dir.scale(scale * 0.080));
                Vec3 c1 = entityPos.add(dir.scale(dist * 0.30))
                                .add(axisA.scale(Math.sin(seed * 0.11) * dist * 0.055))
                                .add(axisB.scale(Math.cos(seed * 0.07) * dist * 0.040));
                Vec3 c2 = entityPos.add(dir.scale(dist * 0.74))
                                .add(axisA.scale(Math.sin(seed * 0.17 + 1.3) * dist * 0.052))
                                .add(axisB.scale(Math.cos(seed * 0.13 + 2.1) * dist * 0.042));

                Vec3[] points = new Vec3[samples + 1];
                Vec3[] tangents = new Vec3[samples + 1];
                Vec3[] rights = new Vec3[samples + 1];
                float[] widths = new float[samples + 1];
                int[] alphas = new int[samples + 1];

                for (int i = 0; i <= samples; i++) {
                        float t = (float) i / samples;
                        points[i] = bezier(start, c1, c2, end, t)
                                        .add(bridgeFlowOffset(axisA, axisB, seed, time, t, dist));

                        float envelope = Mth.sin((float) Math.PI * t);
                        widths[i] = scale * (0.135f + 0.125f * envelope)
                                        * (0.94f + 0.06f * Mth.sin(time * 0.08f - t * 4.7f + seed));
                        alphas[i] = (int) (178 + 46 * envelope);
                }

                for (int i = 0; i <= samples; i++) {
                        Vec3 prev = points[Math.max(0, i - 1)];
                        Vec3 next = points[Math.min(samples, i + 1)];
                        Vec3 tangent = next.subtract(prev);
                        if (tangent.length() <= 1e-6) {
                                tangent = i < samples ? points[i + 1].subtract(points[i])
                                                : points[i].subtract(points[i - 1]);
                        }
                        tangents[i] = tangent.length() <= 1e-6 ? dir : tangent.normalize();
                }

                for (int i = 0; i <= samples; i++) {
                        Vec3 right = camPos.subtract(points[i]).cross(tangents[i]);
                        if (right.length() <= 1e-6) {
                                right = axisA;
                        } else {
                                right = right.normalize();
                        }
                        if (i > 0 && rights[i - 1].dot(right) < 0.0) {
                                right = right.scale(-1.0);
                        }
                        rights[i] = right;
                }

                for (int i = 1; i < samples; i++) {
                        Vec3 smoothed = rights[i - 1].add(rights[i].scale(2.0)).add(rights[i + 1]);
                        if (smoothed.length() > 1e-6) {
                                rights[i] = smoothed.normalize();
                        }
                }

                for (int i = 0; i < samples; i++) {
                        float t0 = (float) i / samples;
                        float t1 = (float) (i + 1) / samples;
                        Vec3 p0 = points[i];
                        Vec3 p1 = points[i + 1];

                        emitBridgeRibbonQuad(bridgeBuilder, mat, entityPos, p0, p1, rights[i], rights[i + 1],
                                        widths[i], widths[i + 1], 255, 255, 255, alphas[i], alphas[i + 1],
                                        t0, t1, 0.0f, 1.0f);
                        emitBridgeRibbonQuad(bridgeBuilder, mat, entityPos, p0, p1, axisA, axisA,
                                        widths[i] * 0.58f, widths[i + 1] * 0.58f,
                                        edgeRi, edgeGi, edgeBi, alphas[i] / 2, alphas[i + 1] / 2, t0, t1, 0.16f,
                                        0.84f);
                }

                if (bufferIn instanceof MultiBufferSource.BufferSource bufferSource) {
                        setUniform(shader, "Time", time);
                        setUniform(shader, "Opacity", 0.90f);
                        setUniform(shader, "FlowSpeed", 0.18f);
                        setUniform(shader, "LumpScale", 4.8f + (float) Math.min(dist * 0.18, 3.2));
                        setUniform(shader, "FilamentIntensity", 0.86f);
                        setUniform(shader, "CoreColor", coreR, coreG, coreB, 1.0f);
                        setUniform(shader, "FlowColor", effectColor[0], effectColor[1], effectColor[2], 1.0f);
                        setUniform(shader, "EdgeColor", edgeR, edgeG, edgeB, 1.0f);
                        bufferSource.endBatch(BFG_BRIDGE_RENDER_TYPE);
                }
        }

        private void renderReducedLegacyBridge(T entity, LivingEntity target, float time, PoseStack matrixStack,
                        MultiBufferSource bufferIn, Vec3 camPos, Vec3 entityPos, Vec3 targetPos) {
                Vec3 diff = targetPos.subtract(entityPos);
                double dist = diff.length();
                if (dist <= 1e-4) {
                        return;
                }
                int[] flowColor = getBfgEffectChannels(entity, 0.10f);
                VertexConsumer builder = bufferIn.getBuffer(RenderType.lightning());
                Matrix4f mat = matrixStack.last().pose();
                Vec3 dir = diff.normalize();
                Vec3 axisA = stablePerpendicular(dir);
                Vec3 axisB = dir.cross(axisA).normalize();
                int seed = entity.getId() * 31 + target.getId() * 17;
                Vec3 start = entityPos.add(dir.scale(entity.getScale() * 0.015));
                Vec3 c1 = entityPos.add(dir.scale(dist * 0.30)).add(axisA.scale(Math.sin(seed * 0.11) * dist * 0.055));
                Vec3 c2 = entityPos.add(dir.scale(dist * 0.74))
                                .add(axisB.scale(Math.cos(seed * 0.13 + 2.1) * dist * 0.042));
                Vec3 prev = start;
                int samples = Mth.clamp((int) (dist * 1.35), 5, 14);
                for (int i = 0; i < samples; i++) {
                        float t = (float) (i + 1) / samples;
                        Vec3 next = bezier(start, c1, c2, targetPos, t)
                                        .add(bridgeFlowOffset(axisA, axisB, seed, time, t, dist));
                        Vec3 tangent = next.subtract(prev);
                        if (tangent.length() > 1e-6) {
                                Vec3 right = camPos.subtract(prev).cross(tangent);
                                right = right.length() <= 1e-6 ? axisA : right.normalize();
                                float width = entity.getScale() * 0.045f;
                                emitFlowQuad(builder, mat, entityPos, prev, next, right, width, width,
                                                flowColor[0], flowColor[1], flowColor[2], 68, 68);
                        }
                        prev = next;
                }
        }

        private void renderBridgeReleaseArcs(T entity, LivingEntity target, float time, PoseStack matrixStack,
                        MultiBufferSource bufferIn, Vec3 camPos, Vec3 entityPos, Vec3 targetPos) {
                Vec3 diff = targetPos.subtract(entityPos);
                double dist = diff.length();
                if (dist <= 1e-4) {
                        return;
                }
                int[] outerArcColor = getBfgEffectChannels(entity, 0.08f);
                int[] innerArcColor = getBfgEffectChannels(entity, 0.64f);
                VertexConsumer builder = bufferIn.getBuffer(RenderType.lightning());
                Matrix4f mat = matrixStack.last().pose();
                Vec3 dir = diff.normalize();
                Vec3 axisA = stablePerpendicular(dir);
                Vec3 axisB = dir.cross(axisA).normalize();
                for (int arc = 0; arc < 3; arc++) {
                        int seed = entity.getId() * 53 + target.getId() * 19 + arc * 101;
                        float center = 0.18f + 0.68f * pseudoRandom(seed + 0.37f);
                        float span = 0.10f + 0.10f * pseudoRandom(seed + 2.7f);
                        float flicker = 0.45f
                                        + 0.55f * Mth.sin(time * (0.10f + pseudoRandom(seed + 3.1f) * 0.12f) + seed);
                        if (flicker < 0.32f) {
                                continue;
                        }
                        Vec3 prev = entityPos.add(diff.scale(Math.max(0.02f, center - span * 0.5f)))
                                        .add(axisA.scale((pseudoRandom(seed + 5.0f) - 0.5f) * entity.getScale()
                                                        * 0.25f));
                        int segments = 3 + (int) (pseudoRandom(seed + 7.0f) * 3.0f);
                        for (int i = 0; i < segments; i++) {
                                float p = center - span * 0.5f + span * (i + 1) / segments;
                                Vec3 next = entityPos.add(diff.scale(Mth.clamp(p, 0.0f, 1.0f)))
                                                .add(axisA.scale(Math.sin(time * 0.19f + seed + i) * entity.getScale()
                                                                * 0.16f))
                                                .add(axisB.scale(Math.cos(time * 0.17f + seed * 0.7f + i)
                                                                * entity.getScale() * 0.12f));
                                Vec3 seg = next.subtract(prev);
                                if (seg.length() > 1e-6) {
                                        Vec3 right = camPos.subtract(prev).cross(seg);
                                        right = right.length() <= 1e-6 ? axisB : right.normalize();
                                        float width = entity.getScale() * (0.010f + 0.009f * flicker);
                                        emitFlowQuad(builder, mat, entityPos, prev, next, right, width * 2.6f,
                                                        width * 2.0f,
                                                        outerArcColor[0], outerArcColor[1], outerArcColor[2],
                                                        (int) (40 * flicker), (int) (30 * flicker));
                                        emitFlowQuad(builder, mat, entityPos, prev, next, right, width, width * 0.75f,
                                                        innerArcColor[0], innerArcColor[1], innerArcColor[2],
                                                        (int) (150 * flicker), (int) (120 * flicker));
                                }
                                prev = next;
                        }
                }
        }

        private Vec3 stablePerpendicular(Vec3 dir) {
                Vec3 axis = dir.cross(new Vec3(0.0, 1.0, 0.0));
                if (axis.length() <= 1e-6) {
                        axis = dir.cross(new Vec3(1.0, 0.0, 0.0));
                }
                return axis.normalize();
        }

        private Vec3 bezier(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
                double u = 1.0 - t;
                return p0.scale(u * u * u).add(p1.scale(3.0 * u * u * t)).add(p2.scale(3.0 * u * t * t))
                                .add(p3.scale(t * t * t));
        }

        private Vec3 bridgeFlowOffset(Vec3 axisA, Vec3 axisB, int seed, float time, float progress, double dist) {
                double envelope = Math.sin(Math.PI * progress);
                double amp = Math.min(0.38, dist * 0.035) * envelope;
                double phase = seed * 0.071 - time * 0.070 + progress * 5.4;
                return axisA.scale(Math.sin(phase) * amp).add(axisB.scale(Math.cos(phase * 0.73 + 1.8) * amp * 0.62));
        }

        private float bridgeFlow(int seed, float time, float progress) {
                float flow = 0.5f + 0.5f * Mth.sin(progress * 8.0f - time * 0.18f + seed * 0.03f);
                float lump = 0.5f + 0.5f * Mth.sin(progress * 3.4f - time * 0.072f + seed * 0.11f);
                return Mth.clamp(flow * 0.58f + lump * 0.42f, 0.0f, 1.0f);
        }

        private void emitFlowQuad(VertexConsumer builder, Matrix4f mat, Vec3 entityPos, Vec3 p0, Vec3 p1, Vec3 right,
                        float halfWidth0, float halfWidth1, int r, int g, int b, int alpha0, int alpha1) {
                Vec3 r0 = right.scale(halfWidth0);
                Vec3 r1 = right.scale(halfWidth1);
                Vec3 v0 = p0.subtract(entityPos).add(r0);
                Vec3 v1 = p0.subtract(entityPos).subtract(r0);
                Vec3 v2 = p1.subtract(entityPos).subtract(r1);
                Vec3 v3 = p1.subtract(entityPos).add(r1);
                builder.vertex(mat, (float) v0.x, (float) v0.y, (float) v0.z).color(r, g, b, Mth.clamp(alpha0, 0, 255))
                                .endVertex();
                builder.vertex(mat, (float) v1.x, (float) v1.y, (float) v1.z).color(r, g, b, Mth.clamp(alpha0, 0, 255))
                                .endVertex();
                builder.vertex(mat, (float) v2.x, (float) v2.y, (float) v2.z).color(r, g, b, Mth.clamp(alpha1, 0, 255))
                                .endVertex();
                builder.vertex(mat, (float) v3.x, (float) v3.y, (float) v3.z).color(r, g, b, Mth.clamp(alpha1, 0, 255))
                                .endVertex();
        }

        private void emitBridgeRibbonQuad(VertexConsumer builder, Matrix4f mat, Vec3 entityPos, Vec3 p0, Vec3 p1,
                        Vec3 right0, Vec3 right1, float halfWidth0, float halfWidth1, int r, int g, int b, int alpha0,
                        int alpha1, float u0, float u1, float vMin, float vMax) {
                Vec3 r0 = right0.scale(halfWidth0);
                Vec3 r1 = right1.scale(halfWidth1);
                Vec3 v0 = p0.subtract(entityPos).add(r0);
                Vec3 v1 = p0.subtract(entityPos).subtract(r0);
                Vec3 v2 = p1.subtract(entityPos).subtract(r1);
                Vec3 v3 = p1.subtract(entityPos).add(r1);
                builder.vertex(mat, (float) v0.x, (float) v0.y, (float) v0.z).color(r, g, b, Mth.clamp(alpha0, 0, 255))
                                .uv(u0, vMax).endVertex();
                builder.vertex(mat, (float) v1.x, (float) v1.y, (float) v1.z).color(r, g, b, Mth.clamp(alpha0, 0, 255))
                                .uv(u0, vMin).endVertex();
                builder.vertex(mat, (float) v2.x, (float) v2.y, (float) v2.z).color(r, g, b, Mth.clamp(alpha1, 0, 255))
                                .uv(u1, vMin).endVertex();
                builder.vertex(mat, (float) v3.x, (float) v3.y, (float) v3.z).color(r, g, b, Mth.clamp(alpha1, 0, 255))
                                .uv(u1, vMax).endVertex();
        }

        private void emitSegment(VertexConsumer builder, Matrix4f mat, Vec3 p0, Vec3 p1, float width, int r, int g,
                        int b, int alpha) {
                Vec3 segment = p1.subtract(p0);
                Vec3 right = new Vec3(-segment.z, 0.0, segment.x);
                if (right.length() <= 1e-6) {
                        right = new Vec3(width, 0.0, 0.0);
                } else {
                        right = right.normalize().scale(width);
                }
                emitQuad(builder, mat, p0.add(right), p0.subtract(right), p1.subtract(right), p1.add(right), r, g, b,
                                alpha);
        }

        private void emitQuad(VertexConsumer builder, Matrix4f mat, Vec3 v0, Vec3 v1, Vec3 v2, Vec3 v3, int r, int g,
                        int b, int alpha) {
                builder.vertex(mat, (float) v0.x, (float) v0.y, (float) v0.z).color(r, g, b, alpha).endVertex();
                builder.vertex(mat, (float) v1.x, (float) v1.y, (float) v1.z).color(r, g, b, alpha).endVertex();
                builder.vertex(mat, (float) v2.x, (float) v2.y, (float) v2.z).color(r, g, b, alpha).endVertex();
                builder.vertex(mat, (float) v3.x, (float) v3.y, (float) v3.z).color(r, g, b, alpha).endVertex();
        }

        private static float pseudoRandom(float seed) {
                return Mth.frac(Mth.sin(seed) * 43758.5453f);
        }

        private float[] getBfgEffectColor(T entity) {
                int hexColor = entity.getColor() & 0xFFFFFF;
                float r = ((hexColor >> 16) & 0xFF) / 255.0f;
                float g = ((hexColor >> 8) & 0xFF) / 255.0f;
                float b = (hexColor & 0xFF) / 255.0f;
                float max = Math.max(r, Math.max(g, b));
                if (max < 0.08f) {
                        return new float[] { 0.18f, 1.0f, 0.18f };
                }
                r /= max;
                g /= max;
                b /= max;
                return new float[] { Mth.clamp(r, 0.0f, 1.0f), Mth.clamp(g, 0.0f, 1.0f),
                                Mth.clamp(b, 0.0f, 1.0f) };
        }

        private int[] getBfgEffectChannels(T entity, float whiteMix) {
                float[] color = getBfgEffectColor(entity);
                return new int[] {
                                toColorChannel(mixColor(color[0], 1.0f, whiteMix)),
                                toColorChannel(mixColor(color[1], 1.0f, whiteMix)),
                                toColorChannel(mixColor(color[2], 1.0f, whiteMix))
                };
        }

        private static float mixColor(float from, float to, float amount) {
                return Mth.lerp(amount, from, to);
        }

        private static int toColorChannel(float value) {
                return Mth.clamp((int) (Mth.clamp(value, 0.0f, 1.0f) * 255.0f), 0, 255);
        }

        private void renderCoronaPlasmaCore(T entity, float partialTicks, PoseStack matrixStack,
                        MultiBufferSource bufferIn, int packedLightIn) {
                float time = entity.tickCount + partialTicks;
                if (!FDShaderHandler.isBfgCoronaShaderLoaded()) {
                        if (!missingCoronaShaderLogged) {
                                System.err.println(
                                                "[FantasyDesire] BFG corona shader is not loaded; using legacy energySwirl fallback.");
                                missingCoronaShaderLogged = true;
                        }
                        renderLegacyPlasmaCore(entity, time, matrixStack, bufferIn, packedLightIn);
                        return;
                }

                float[] effectColor = getBfgEffectColor(entity);
                int baseR = toColorChannel(effectColor[0]);
                int baseG = toColorChannel(effectColor[1]);
                int baseB = toColorChannel(effectColor[2]);
                float coreR = mixColor(effectColor[0], 1.0f, 0.78f);
                float coreG = mixColor(effectColor[1], 1.0f, 0.78f);
                float coreB = mixColor(effectColor[2], 1.0f, 0.78f);

                float coreScale = entity.getScale() * 0.62f;
                float pulseFrequency = 0.055f;
                float opacity = 0.88f;
                float eruptionIntensity = 0.18f + 0.03f * Mth.sin(time * 0.045f);
                float noiseSpeed = 0.16f;

                ShaderInstance shader = FDShaderHandler.getBfgCoronaShader();
                setUniform(shader, "Time", time);
                setUniform(shader, "CoreColor", coreR, coreG, coreB, 1.0f);
                setUniform(shader, "FlameColor", effectColor[0], effectColor[1], effectColor[2], 1.0f);
                setUniform(shader, "EruptionIntensity", eruptionIntensity);
                setUniform(shader, "NoiseSpeed", noiseSpeed);
                setUniform(shader, "PulseFrequency", pulseFrequency);
                setUniform(shader, "Opacity", opacity);
                setUniform(shader, "RadiusScale", coreScale);

                VertexConsumer builder = bufferIn.getBuffer(BFG_CORONA_RENDER_TYPE);
                int alpha = (int) (255 * opacity);
                renderCoronaLayer(matrixStack, builder, coreScale * 1.78f, (int) (alpha * 0.15f), true, baseR, baseG,
                                baseB);
                renderCoronaLayer(matrixStack, builder, coreScale * 1.08f, (int) (alpha * 0.82f), true, baseR, baseG,
                                baseB);
                renderCoronaLayer(matrixStack, builder, coreScale * 0.82f, (int) (alpha * 0.66f), false, baseR, baseG,
                                baseB);
                renderCoronaLayer(matrixStack, builder, coreScale * 0.18f, 255, true, 255, 255, 255);

                if (bufferIn instanceof MultiBufferSource.BufferSource bufferSource) {
                        setUniform(shader, "Time", time);
                        setUniform(shader, "CoreColor", coreR, coreG, coreB, 1.0f);
                        setUniform(shader, "FlameColor", effectColor[0], effectColor[1], effectColor[2], 1.0f);
                        setUniform(shader, "EruptionIntensity", eruptionIntensity);
                        setUniform(shader, "NoiseSpeed", noiseSpeed);
                        setUniform(shader, "PulseFrequency", pulseFrequency);
                        setUniform(shader, "Opacity", opacity);
                        setUniform(shader, "RadiusScale", coreScale);
                        bufferSource.endBatch(BFG_CORONA_RENDER_TYPE);
                }
        }

        private void renderCoronaLayer(PoseStack matrixStack, VertexConsumer builder, float scale, int alpha,
                        boolean cameraFacing, int r, int g, int b) {
                if (cameraFacing) {
                        matrixStack.pushPose();
                        matrixStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
                        matrixStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                        matrixStack.scale(scale, scale, scale);
                        Matrix4f mat = matrixStack.last().pose();
                        renderCoronaFace(builder, mat, r, g, b, alpha,
                                        -1.0f, -1.0f, 0.0f,
                                        1.0f, -1.0f, 0.0f,
                                        1.0f, 1.0f, 0.0f,
                                        -1.0f, 1.0f, 0.0f);
                        matrixStack.popPose();
                }

                matrixStack.pushPose();
                matrixStack.scale(scale, scale, scale);
                Matrix4f mat = matrixStack.last().pose();
                renderCoronaFace(builder, mat, r, g, b, alpha,
                                -1.0f, -1.0f, 0.0f,
                                1.0f, -1.0f, 0.0f,
                                1.0f, 1.0f, 0.0f,
                                -1.0f, 1.0f, 0.0f);
                renderCoronaFace(builder, mat, r, g, b, alpha,
                                0.0f, -1.0f, -1.0f,
                                0.0f, -1.0f, 1.0f,
                                0.0f, 1.0f, 1.0f,
                                0.0f, 1.0f, -1.0f);
                renderCoronaFace(builder, mat, r, g, b, alpha,
                                -1.0f, 0.0f, -1.0f,
                                1.0f, 0.0f, -1.0f,
                                1.0f, 0.0f, 1.0f,
                                -1.0f, 0.0f, 1.0f);
                matrixStack.popPose();
        }

        private void renderLegacyPlasmaCore(T entity, float time, PoseStack matrixStack, MultiBufferSource bufferIn,
                        int packedLightIn) {
                matrixStack.pushPose();
                float coreScale = entity.getScale() * 0.35f;
                matrixStack.scale(coreScale, coreScale, coreScale);
                matrixStack.mulPose(Axis.YP.rotationDegrees(time * 10f));
                matrixStack.mulPose(Axis.XP.rotationDegrees(time * 10f));

                Matrix4f mat = matrixStack.last().pose();
                Matrix3f normal = matrixStack.last().normal();
                VertexConsumer builder = bufferIn
                                .getBuffer(RenderType.energySwirl(ENERGY_TEX, time * 0.01f, time * 0.01f));

                int[] fallbackColor = getBfgEffectChannels(entity, 0.12f);
                int r = fallbackColor[0], g = fallbackColor[1], b = fallbackColor[2];
                renderFace(builder, mat, normal, r, g, b, packedLightIn, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f,
                                0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0, 0, 1);
                renderFace(builder, mat, normal, r, g, b, packedLightIn, 0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f,
                                -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0, 0, -1);
                renderFace(builder, mat, normal, r, g, b, packedLightIn, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f,
                                0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0, 1, 0);
                renderFace(builder, mat, normal, r, g, b, packedLightIn, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f,
                                0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0, -1, 0);
                renderFace(builder, mat, normal, r, g, b, packedLightIn, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f,
                                0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 1, 0, 0);
                renderFace(builder, mat, normal, r, g, b, packedLightIn, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f,
                                -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -1, 0, 0);
                matrixStack.popPose();
        }

        private void renderCoronaFace(VertexConsumer builder, Matrix4f mat, int r, int g, int b, int a,
                        float x0, float y0, float z0,
                        float x1, float y1, float z1,
                        float x2, float y2, float z2,
                        float x3, float y3, float z3) {
                builder.vertex(mat, x0, y0, z0).color(r, g, b, a).uv(0, 1).endVertex();
                builder.vertex(mat, x1, y1, z1).color(r, g, b, a).uv(1, 1).endVertex();
                builder.vertex(mat, x2, y2, z2).color(r, g, b, a).uv(1, 0).endVertex();
                builder.vertex(mat, x3, y3, z3).color(r, g, b, a).uv(0, 0).endVertex();
        }

        private static void setUniform(ShaderInstance shader, String name, float value) {
                if (shader == null) {
                        return;
                }
                Uniform uniform = shader.getUniform(name);
                if (uniform != null) {
                        uniform.set(value);
                }
        }

        private static void setUniform(ShaderInstance shader, String name, float x, float y, float z, float w) {
                if (shader == null) {
                        return;
                }
                Uniform uniform = shader.getUniform(name);
                if (uniform != null) {
                        uniform.set(x, y, z, w);
                }
        }

        private void renderFace(VertexConsumer builder, Matrix4f mat, Matrix3f normal, int r, int g, int b, int light,
                        float x0, float y0, float z0,
                        float x1, float y1, float z1,
                        float x2, float y2, float z2,
                        float x3, float y3, float z3,
                        float nx, float ny, float nz) {
                builder.vertex(mat, x0, y0, z0).color(r, g, b, 255).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY)
                                .uv2(light).normal(normal, nx, ny, nz).endVertex();
                builder.vertex(mat, x1, y1, z1).color(r, g, b, 255).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY)
                                .uv2(light).normal(normal, nx, ny, nz).endVertex();
                builder.vertex(mat, x2, y2, z2).color(r, g, b, 255).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY)
                                .uv2(light).normal(normal, nx, ny, nz).endVertex();
                builder.vertex(mat, x3, y3, z3).color(r, g, b, 255).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY)
                                .uv2(light).normal(normal, nx, ny, nz).endVertex();
        }

        private void renderTrail(T entity, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn,
                        int packedLightIn) {
                List<Vec3> trail = entity.getTrailPositions();
                if (trail == null || trail.size() < 2)
                        return;

                ResourceLocation tex = getTextureLocation(entity);
                // Ensure we get a VertexConsumer to draw the trail and core highlight
                VertexConsumer builder = bufferIn.getBuffer(RenderType.entityTranslucent(tex));
                // Use eyes() render type for additive/emissive (glow) effect for the inner core
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
                float baseSize = 0.2f * entity.getScale(); // 基础宽度（full width）

                int hexColor = entity.getColor();
                int colorR = (hexColor >> 16) & 0xFF;
                int colorG = (hexColor >> 8) & 0xFF;
                int colorB = hexColor & 0xFF;

                final float sharpenStart = 0.75f; // 尾端从 75% 开始尖化

                // 预计算每个点的平滑切向（tangent），使用前后点平均
                Vec3[] tangents = new Vec3[count];
                for (int i = 0; i < count; i++) {
                        Vec3 prev = points.get(Math.max(0, i - 1));
                        Vec3 next = points.get(Math.min(count - 1, i + 1));
                        Vec3 t = next.subtract(prev);
                        double tlen = t.length();
                        if (tlen <= 1e-6) {
                                // fallback to forward direction
                                if (i < count - 1)
                                        t = points.get(i + 1).subtract(points.get(i));
                                else
                                        t = points.get(i).subtract(points.get(i - 1));
                        }
                        tangents[i] = t.normalize();
                }

                // 绘制每段：外层 + 内层高亮
                double offsetDist = 0.5 * entity.getScale();

                for (int i = 0; i < count - 1; i++) {
                        Vec3 p0 = points.get(i);
                        Vec3 p1 = points.get(i + 1);

                        p0 = p0.add(tangents[i].scale(offsetDist));
                        p1 = p1.add(tangents[i + 1].scale(offsetDist));

                        Vec3 r0 = p0.subtract(entityPos);
                        Vec3 r1 = p1.subtract(entityPos);

                        // 使用两端的切向平均以获得段切向（更平滑）
                        Vec3 tan0 = tangents[i];
                        Vec3 tan1 = tangents[i + 1];
                        Vec3 segTan = tan0.add(tan1).scale(0.5);
                        if (segTan.length() <= 1e-6)
                                segTan = p1.subtract(p0).normalize();
                        else
                                segTan = segTan.normalize();

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

                        // colors: high-energy trail desaturates toward white while alpha drops.
                        float lerpOuter0 = 0.42f + t0 * 0.46f;
                        float lerpOuter1 = 0.42f + t1 * 0.46f;
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

                        float lerpInner0 = 0.64f + t0 * 0.28f;
                        float lerpInner1 = 0.64f + t1 * 0.28f;
                        float ir0 = (colorR / 255f) * (1f - lerpInner0) + lerpInner0;
                        float ig0 = (colorG / 255f) * (1f - lerpInner0) + lerpInner0;
                        float ib0 = (colorB / 255f) * (1f - lerpInner0) + lerpInner0;
                        float ir1 = (colorR / 255f) * (1f - lerpInner1) + lerpInner1;
                        float ig1 = (colorG / 255f) * (1f - lerpInner1) + lerpInner1;
                        float ib1 = (colorB / 255f) * (1f - lerpInner1) + lerpInner1;
                        float iAlpha0 = (1f - t0) * 0.95f;
                        float iAlpha1 = (1f - t1) * 0.75f;
                        // Boost inner color to make it glow (then clamp to [0,1])
                        final float glowBoost = 1.18f;
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
                        // To prevent Z-fighting or culling, we will render double-sided quads
                        builder.vertex(mat, (float) o0a.x, (float) o0a.y, (float) o0a.z)
                                        .color((int) (or0 * 255), (int) (og0 * 255), (int) (ob0 * 255),
                                                        (int) (oAlpha0 * 255))
                                        .uv(0f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn)
                                        .normal(normal, 0f, 0f, 1f)
                                        .endVertex();
                        builder.vertex(mat, (float) o0b.x, (float) o0b.y, (float) o0b.z)
                                        .color((int) (or0 * 255), (int) (og0 * 255), (int) (ob0 * 255),
                                                        (int) (oAlpha0 * 255))
                                        .uv(0f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn)
                                        .normal(normal, 0f, 0f, 1f)
                                        .endVertex();
                        builder.vertex(mat, (float) o1b.x, (float) o1b.y, (float) o1b.z)
                                        .color((int) (or1 * 255), (int) (og1 * 255), (int) (ob1 * 255),
                                                        (int) (oAlpha1 * 255))
                                        .uv(1f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn)
                                        .normal(normal, 0f, 0f, 1f)
                                        .endVertex();
                        builder.vertex(mat, (float) o1a.x, (float) o1a.y, (float) o1a.z)
                                        .color((int) (or1 * 255), (int) (og1 * 255), (int) (ob1 * 255),
                                                        (int) (oAlpha1 * 255))
                                        .uv(1f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn)
                                        .normal(normal, 0f, 0f, 1f)
                                        .endVertex();

                        // backface outer
                        builder.vertex(mat, (float) o1a.x, (float) o1a.y, (float) o1a.z)
                                        .color((int) (or1 * 255), (int) (og1 * 255), (int) (ob1 * 255),
                                                        (int) (oAlpha1 * 255))
                                        .uv(1f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn)
                                        .normal(normal, 0f, 0f, -1f)
                                        .endVertex();
                        builder.vertex(mat, (float) o1b.x, (float) o1b.y, (float) o1b.z)
                                        .color((int) (or1 * 255), (int) (og1 * 255), (int) (ob1 * 255),
                                                        (int) (oAlpha1 * 255))
                                        .uv(1f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn)
                                        .normal(normal, 0f, 0f, -1f)
                                        .endVertex();
                        builder.vertex(mat, (float) o0b.x, (float) o0b.y, (float) o0b.z)
                                        .color((int) (or0 * 255), (int) (og0 * 255), (int) (ob0 * 255),
                                                        (int) (oAlpha0 * 255))
                                        .uv(0f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn)
                                        .normal(normal, 0f, 0f, -1f)
                                        .endVertex();
                        builder.vertex(mat, (float) o0a.x, (float) o0a.y, (float) o0a.z)
                                        .color((int) (or0 * 255), (int) (og0 * 255), (int) (ob0 * 255),
                                                        (int) (oAlpha0 * 255))
                                        .uv(0f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn)
                                        .normal(normal, 0f, 0f, -1f)
                                        .endVertex();

                        // draw inner highlight quad (narrower)
                        coreBuilder.vertex(mat, (float) c0a.x, (float) c0a.y, (float) c0a.z)
                                        .color((int) (ir0 * 255), (int) (ig0 * 255), (int) (ib0 * 255),
                                                        (int) (iAlpha0 * 255))
                                        .uv(0f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn)
                                        .normal(normal, 0f, 0f, 1f)
                                        .endVertex();
                        coreBuilder.vertex(mat, (float) c0b.x, (float) c0b.y, (float) c0b.z)
                                        .color((int) (ir0 * 255), (int) (ig0 * 255), (int) (ib0 * 255),
                                                        (int) (iAlpha0 * 255))
                                        .uv(0f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn)
                                        .normal(normal, 0f, 0f, 1f)
                                        .endVertex();
                        coreBuilder.vertex(mat, (float) c1b.x, (float) c1b.y, (float) c1b.z)
                                        .color((int) (ir1 * 255), (int) (ig1 * 255), (int) (ib1 * 255),
                                                        (int) (iAlpha1 * 255))
                                        .uv(1f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn)
                                        .normal(normal, 0f, 0f, 1f)
                                        .endVertex();
                        coreBuilder.vertex(mat, (float) c1a.x, (float) c1a.y, (float) c1a.z)
                                        .color((int) (ir1 * 255), (int) (ig1 * 255), (int) (ib1 * 255),
                                                        (int) (iAlpha1 * 255))
                                        .uv(1f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn)
                                        .normal(normal, 0f, 0f, 1f)
                                        .endVertex();

                        // backface inner
                        coreBuilder.vertex(mat, (float) c1a.x, (float) c1a.y, (float) c1a.z)
                                        .color((int) (ir1 * 255), (int) (ig1 * 255), (int) (ib1 * 255),
                                                        (int) (iAlpha1 * 255))
                                        .uv(1f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn)
                                        .normal(normal, 0f, 0f, -1f)
                                        .endVertex();
                        coreBuilder.vertex(mat, (float) c1b.x, (float) c1b.y, (float) c1b.z)
                                        .color((int) (ir1 * 255), (int) (ig1 * 255), (int) (ib1 * 255),
                                                        (int) (iAlpha1 * 255))
                                        .uv(1f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn)
                                        .normal(normal, 0f, 0f, -1f)
                                        .endVertex();
                        coreBuilder.vertex(mat, (float) c0b.x, (float) c0b.y, (float) c0b.z)
                                        .color((int) (ir0 * 255), (int) (ig0 * 255), (int) (ib0 * 255),
                                                        (int) (iAlpha0 * 255))
                                        .uv(0f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn)
                                        .normal(normal, 0f, 0f, -1f)
                                        .endVertex();
                        coreBuilder.vertex(mat, (float) c0a.x, (float) c0a.y, (float) c0a.z)
                                        .color((int) (ir0 * 255), (int) (ig0 * 255), (int) (ib0 * 255),
                                                        (int) (iAlpha0 * 255))
                                        .uv(0f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLightIn)
                                        .normal(normal, 0f, 0f, -1f)
                                        .endVertex();
                }
        }

        @Override
        public boolean shouldRender(T p_114491_, Frustum p_114492_, double p_114493_, double p_114494_,
                        double p_114495_) {
                return true;
        }
}
