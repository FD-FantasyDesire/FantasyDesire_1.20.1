package tennouboshiuzume.mods.FantasyDesire.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.client.renderer.layers.LayerMainBlade;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tennouboshiuzume.mods.FantasyDesire.client.renderer.BladeMangRingRenderer;

/** 为本体的分件渲染事件补充持有者，覆盖第一、第三人称以及待机佩刀。 */
@Mixin(value = LayerMainBlade.class, remap = false)
public abstract class LayerMainBladeMixin {
    // 指定 LivingEntity 重载：本体的这个方法保留原名，Entity 桥接方法才参与原版重映射。
    @WrapOperation(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/util/LazyOptional;ifPresent(Lnet/minecraftforge/common/util/NonNullConsumer;)V"))
    private void fantasydesire$renderHeldWithContext(LazyOptional<ISlashBladeState> state,
            NonNullConsumer<ISlashBladeState> consumer, Operation<Void> original,
            PoseStack poseStack, MultiBufferSource buffer, int light, LivingEntity holder,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch) {
        BladeMangRingRenderer.withContext(holder, holder.getMainHandItem(), () -> original.call(state, consumer));
    }

    @WrapOperation(method = "renderStandbyBlade",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/util/LazyOptional;ifPresent(Lnet/minecraftforge/common/util/NonNullConsumer;)V"))
    private void fantasydesire$renderStandbyWithContext(LazyOptional<ISlashBladeState> state,
            NonNullConsumer<ISlashBladeState> consumer, Operation<Void> original,
            PoseStack poseStack, MultiBufferSource buffer, int light, ItemStack stack, LivingEntity holder) {
        BladeMangRingRenderer.withContext(holder, stack, () -> original.call(state, consumer));
    }
}
