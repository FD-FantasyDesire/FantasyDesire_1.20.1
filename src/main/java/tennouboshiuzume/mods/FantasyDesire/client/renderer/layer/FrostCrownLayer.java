package tennouboshiuzume.mods.FantasyDesire.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;

public class FrostCrownLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public FrostCrownLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    private static final ResourceLocation MODEL = new ResourceLocation("slashblade", "model/util/ss.obj");

    private static final ResourceLocation TEXTURE = new ResourceLocation("slashblade", "model/util/ss.png");

    private static final float BASE_SCALE = 0.0075f;

    @Override
    public void render(PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {

        boolean hasFrostStorm = player.hasEffect(FDPotionEffects.FROST_STORM.get());

        if (!hasFrostStorm) {
            return;
        }

        poseStack.pushPose();
        this.getParentModel().head.translateAndRotate(poseStack);

        // Manual translation up to top of head (-Y is UP)
        poseStack.translate(0.0D, -0.65D, 0.0D);

        renderCentralBlade(poseStack, buffer, packedLight, ageInTicks);
        renderRingBlades(poseStack, buffer, packedLight, ageInTicks);

        poseStack.popPose();
    }

    private void renderCentralBlade(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float ageInTicks) {
        poseStack.pushPose();

        // Slightly in front (-Z)
        poseStack.translate(0.0D, -0.05D, -0.1D);

        // Optional slight up/down sway
        float swayY = Mth.sin(ageInTicks * 0.1f) * 0.02f;
        poseStack.translate(0.0D, swayY, 0.0D);

        // Point forward
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));

        // Tilt upward
        poseStack.mulPose(Axis.XP.rotationDegrees(15f));

        float scale = BASE_SCALE * 0.3f;
        poseStack.scale(scale, scale, scale);

        BladeRenderState.setCol(0xFFFFFF, false);

        renderModel(poseStack, buffer, packedLight);

        poseStack.popPose();
    }

    private void renderRingBlades(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float ageInTicks) {
        int bladeCount = 7;
        float arcAngle = bladeCount * 40f;
        float radius = 0.25f;

        float[] bladeScales = { 0.15f, 0.2f, 0.15f, 0.10f, 0.15f, 0.2f, 0.15f };
        float[] bladeTilt = { 10, 10, 10, -15, 10, 10, 10 };

        float startAngle = -arcAngle / 2f;
        float step = arcAngle / (bladeCount - 1);

        float swayAngle = Mth.sin(ageInTicks * 0.05f) * 5f;

        for (int i = 0; i < bladeCount; i++) {
            poseStack.pushPose();

            float angle = startAngle + i * step + swayAngle;
            float radians = (float) Math.toRadians(angle);

            float offsetX = Mth.sin(radians) * radius;
            float offsetZ = Mth.cos(radians) * radius;

            poseStack.translate(offsetX, 0.0D, offsetZ);

            // Align rotation along Y axis with angle (face outward)
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));

            // Tilt upward
            poseStack.mulPose(Axis.XP.rotationDegrees(bladeTilt[i]));

            float scale = BASE_SCALE * bladeScales[i];
            poseStack.scale(scale, scale, scale);

            BladeRenderState.setCol(0x99FFFF, false);

            renderModel(poseStack, buffer, packedLight);

            poseStack.popPose();
        }
    }

    private void renderModel(PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {

        WavefrontObject model = BladeModelManager.getInstance().getModel(MODEL);

        BladeRenderState.renderOverridedLuminous(
                ItemStack.EMPTY,
                model,
                "ss",
                TEXTURE,
                poseStack,
                buffer,
                packedLight);
    }
}