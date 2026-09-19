package tennouboshiuzume.mods.FantasyDesire.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.monster.Slime;
import tennouboshiuzume.mods.FantasyDesire.client.renderer.VoidFlameRenderer;

/** 史莱姆外壳是独立模型，按原版 SlimeOuterLayer 的姿态流程补充表面采集。 */
public class VoidFlameSlimeOuterLayer extends RenderLayer<Slime, SlimeModel<Slime>> {
    private final SlimeModel<Slime> outerModel;

    public VoidFlameSlimeOuterLayer(RenderLayerParent<Slime, SlimeModel<Slime>> parent, EntityModelSet models) {
        super(parent);
        outerModel = new SlimeModel<>(models.bakeLayer(ModelLayers.SLIME_OUTER));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Slime entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        getParentModel().copyPropertiesTo(outerModel);
        outerModel.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
        outerModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // 继承渲染器已应用的尺寸与挤压变换，片元继续使用原外壳纹理的 Alpha。
        VoidFlameRenderer.capture(entity, outerModel, getTextureLocation(entity), poseStack,
                packedLight, partialTicks);
    }
}
