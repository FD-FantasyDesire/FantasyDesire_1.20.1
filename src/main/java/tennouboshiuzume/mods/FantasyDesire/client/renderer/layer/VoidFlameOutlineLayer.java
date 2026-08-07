package tennouboshiuzume.mods.FantasyDesire.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import tennouboshiuzume.mods.FantasyDesire.client.renderer.VoidFlameOutlineRenderer;

/**
 * 复用活体渲染器已经完成动画的基础模型，生成轮廓遮罩。
 */
public class VoidFlameOutlineLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public VoidFlameOutlineLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch) {
        VoidFlameOutlineRenderer.renderSilhouette(entity, this.getParentModel(), poseStack, packedLight);
    }
}
