package tennouboshiuzume.mods.FantasyDesire.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tennouboshiuzume.mods.FantasyDesire.ability.FDGlidingRules;

// 怎么Mojang不把鞘翅属性塞ability里，搞半天还要用一次Mixin，我真的好讨厌你
@Mixin(Player.class)
public abstract class PlayerGlidingMixin {
    // 外层原版方法需要重映射；Forge 扩展的 ItemStack 方法保留原名。
    @ModifyExpressionValue(method = "tryToStartFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z", remap = false))
    private boolean fantasydesire$canStartGliding(boolean original) {
        // 保留胸甲与其他模组的判断，只补充主手能力提供的许可。
        return original || FDGlidingRules.hasAdditionalGlidingSource((Player) (Object) this);
    }
}
