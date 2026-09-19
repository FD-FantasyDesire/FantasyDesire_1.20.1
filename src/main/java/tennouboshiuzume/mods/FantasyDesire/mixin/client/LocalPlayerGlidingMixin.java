package tennouboshiuzume.mods.FantasyDesire.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tennouboshiuzume.mods.FantasyDesire.ability.FDGlidingRules;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerGlidingMixin {
    // 外层原版方法需要重映射；Forge 扩展的 ItemStack 方法保留原名。
    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z", remap = false))
    private boolean fantasydesire$canRequestGliding(boolean original) {
        // 按键条件、起飞流程和发包仍由原版负责。
        return original || FDGlidingRules.hasAdditionalGlidingSource((LocalPlayer) (Object) this);
    }
}
