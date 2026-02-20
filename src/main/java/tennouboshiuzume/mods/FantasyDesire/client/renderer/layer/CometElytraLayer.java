package tennouboshiuzume.mods.FantasyDesire.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import com.mojang.math.Axis;

public class CometElytraLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public CometElytraLayer(RenderLayerParent<AbstractClientPlayer,
                PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

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

        if (!player.hasEffect(FDPotionEffects.COMET_ELYTRA.get())) {
            return;
        }

        poseStack.pushPose();

        // 绑定到身体骨骼
        this.getParentModel().body.translateAndRotate(poseStack);

        // 插在背上
        poseStack.translate(0.0D, 0.25D, 0.2D);

        poseStack.mulPose(Axis.XP.rotationDegrees(200F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(15F));

        float scale = 0.0075f;
        poseStack.scale(scale, scale, scale);

        // 这里创建一个“虚拟实体数据容器”
        EntityFDPhantomSword dummy = new EntityFDPhantomSword(FDEntitys.FDPhantomSword.get(), player.level());

        dummy.setColor(0x66CCFF);
        PhantomSwordModelRenderer.renderModel(dummy, poseStack, buffer, packedLight);

        poseStack.popPose();
    }
    public class PhantomSwordModelRenderer {

        public static void renderModel(EntityFDPhantomSword entity,
                                       PoseStack poseStack,
                                       MultiBufferSource buffer,
                                       int packedLight) {

            WavefrontObject model = BladeModelManager.getInstance().getModel(entity.getModelLoc());

            BladeRenderState.setCol(entity.getColor(), false);

            BladeRenderState.renderOverridedLuminous(
                    ItemStack.EMPTY,
                    model,
                    "ss",
                    entity.getTextureLoc(),
                    poseStack,
                    buffer,
                    packedLight
            );
        }
    }
}
