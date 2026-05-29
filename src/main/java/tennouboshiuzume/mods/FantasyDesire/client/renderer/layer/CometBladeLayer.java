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
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffectsRegistry;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;

public class CometBladeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public CometBladeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    private static final ResourceLocation MODEL = new ResourceLocation("slashblade", "model/util/ss.obj");

    private static final ResourceLocation TEXTURE = new ResourceLocation("slashblade", "model/util/ss.png");

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

        boolean hasTyrant = CapabilityUtils.SEConditionMatcher.of(player)
                .requireTranslation("item.fantasydesire.chikeflare")
                .requireSE(FDSpecialEffectsRegistry.TyrantStrike)
                .match() != null;

        boolean hasShield = CapabilityUtils.SEConditionMatcher.of(player)
                .requireTranslation("item.fantasydesire.chikeflare")
                .requireSE(FDSpecialEffectsRegistry.SoulShield)
                .match() != null;

        boolean hasImmortal = CapabilityUtils.SEConditionMatcher.of(player)
                .allowBothHands()
                .requireTranslation("item.fantasydesire.chikeflare")
                .requireSE(FDSpecialEffectsRegistry.ImmortalSoul)
                .match() != null;

        boolean hasCometElytra = player.hasEffect(FDPotionEffects.COMET_ELYTRA.get());

        if (!hasTyrant && !hasShield && !hasImmortal && !hasCometElytra) {
            return;
        }

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        // 常态下的SE指示器
        // Main：暴君一击
        // Obrit：灵魂护盾
        // Outer：不屈之魂
        // 使用鞘翅飞行时，转化为剑翼
        if (hasTyrant) {
            renderMainBlade(poseStack, buffer, packedLight, player, ageInTicks);
        }
        if (!player.isFallFlying()) {
            if (hasShield) {
                renderOrbitBlades(poseStack, buffer, packedLight, player, ageInTicks);
            }
            if (hasImmortal) {
                renderOuterOrbitBlades(poseStack, buffer, packedLight, player, ageInTicks);
            }
        } else {
            if (hasCometElytra && hasShield) {
                // 啊，，，我真的不会写基于Layer的拖尾，如果你在看这行代码并且知道怎么写，请PR我，谢谢
                renderWingBlades(poseStack, buffer, packedLight, ageInTicks, player, partialTicks);
            }
        }
        poseStack.popPose();
    }

    private void renderMainBlade(PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player,
            float ageInTicks) {

        poseStack.pushPose();

        poseStack.translate(0.0D, 0.35D, 0.45D);

        poseStack.mulPose(Axis.XP.rotationDegrees(90f));

        poseStack.mulPose(Axis.YP.rotationDegrees(180f));

        applyBladeScale(poseStack, 0.0075f);
        applyBladeColor(0x88AAFF);

        renderModel(poseStack, buffer, packedLight);

        poseStack.popPose();
    }

    private void applyBladeScale(PoseStack poseStack, float scale) {
        poseStack.scale(scale, scale, scale);
    }

    private void applyBladeColor(int color) {
        BladeRenderState.setCol(color, false);
    }

    private void renderWingBlades(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float ageInTicks,
            AbstractClientPlayer player, float partialTicks) {
        float speed = 2.0f;
        // 使用 partialTicks 平滑插值，避免卡顿
        float time = (player.tickCount + partialTicks) * speed;

        for (int i = 0; i < 8; i++) {
            poseStack.pushPose();

            // 1. Centered around the player's waist
            poseStack.translate(0.0D, 0.7D, 0.0D);

            // 2. Rotate around Y axis (Yaw) for orbit
            float angle = time + (i * (360f / 8f));
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));

            // Push outward
            poseStack.translate(0.0D, 0.0D, 1.0D);

            // 4. Tilt outwards to form a skirt/cone shape
            poseStack.mulPose(Axis.XP.rotationDegrees(-60f));

            applyBladeScale(poseStack, 0.0075f * 0.6f);
            applyBladeColor(0x00FFFF);

            renderModel(poseStack, buffer, packedLight);
            poseStack.popPose();
        }
    }

    private void renderOrbitBlades(PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player,
            float ageInTicks) {

        float radius = 1f;
        float speed = 2.0f;
        float time = ageInTicks * speed;

        for (int i = 0; i < 8; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.35D, 0.65D);
            float baseAngle = i * 45f;
            float angle = baseAngle + time;
            // Calculate cross-star shape instead of circular orbit
            float radians = (float) Math.toRadians(angle);
            float sin = Mth.sin(radians);
            float cos = Mth.cos(radians);
            float currentRadius = radius * (float) (1.0 - Math.pow(Math.sin(2 * radians), 2) * 0.5);

            float offsetX = cos * currentRadius;
            float offsetY = -sin * currentRadius;

            poseStack.translate(offsetX, offsetY, 0);

            poseStack.mulPose(Axis.XP.rotationDegrees(90f));

            poseStack.mulPose(Axis.YN.rotationDegrees(angle - 90f));

            applyBladeScale(poseStack, 0.0075f * 0.3f);

            applyBladeColor(0x00FFFF);

            renderModel(poseStack, buffer, packedLight);

            poseStack.popPose();
        }
    }

    private void renderOuterOrbitBlades(PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player,
            float ageInTicks) {

        float radius = 0.8f;
        float speed = 2.0f;
        float time = ageInTicks * speed;

        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.35D, 0.55D);
            float baseAngle = i * 90f;
            float angle = baseAngle + time;
            float offsetX = Mth.cos((float) Math.toRadians(angle)) * radius;
            float offsetY = -Mth.sin((float) Math.toRadians(angle)) * radius;

            poseStack.translate(offsetY, offsetX, 0);

            poseStack.mulPose(Axis.XP.rotationDegrees(90f));

            poseStack.mulPose(Axis.YP.rotationDegrees(angle));

            poseStack.mulPose(Axis.ZP.rotationDegrees(angle * 4.0f));

            applyBladeScale(poseStack, 0.0075f * 0.5f);

            applyBladeColor(0xFFFF00);

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