package tennouboshiuzume.mods.FantasyDesire.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tennouboshiuzume.mods.FantasyDesire.ability.FDGlidingRules;

@Mixin(LivingEntity.class)
public abstract class LivingEntityGlidingMixin {
    // 外层原版方法需要重映射；Forge 扩展的 ItemStack 方法保留原名。
    @ModifyExpressionValue(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z", remap = false))
    private boolean fantasydesire$canContinueGliding(boolean original) {
        return original || FDGlidingRules.hasAdditionalGlidingSource((LivingEntity) (Object) this);
    }

    @WrapOperation(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;elytraFlightTick(Lnet/minecraft/world/entity/LivingEntity;I)Z", remap = false))
    private boolean fantasydesire$tickGliding(ItemStack chestStack, LivingEntity entity, int flightTicks,
            Operation<Boolean> original) {
        if (FDGlidingRules.hasAdditionalGlidingSource(entity)) {
            // 主手能力免消耗；此分支也会跳过调用链内部的胸甲飞行逻辑。
            FDGlidingRules.tickAdditionalGliding(entity, flightTicks);
            return true;
        }
        // 沿包装链继续执行，不能直接调用胸甲方法绕过其他模组的包装。
        return original.call(chestStack, entity, flightTicks);
    }
}
