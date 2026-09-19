package tennouboshiuzume.mods.FantasyDesire.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import tennouboshiuzume.mods.FantasyDesire.client.renderer.VoidFlameRenderer;

/** 记录当前动画姿态，供实体批次完成后绘制表面侵蚀。 */
public class VoidFlameLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public VoidFlameLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch) {
        VoidFlameRenderer.capture(entity, getParentModel(), getTextureLocation(entity), poseStack,
                packedLight, partialTicks);
    }
}
