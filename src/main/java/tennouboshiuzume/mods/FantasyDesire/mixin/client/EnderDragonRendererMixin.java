package tennouboshiuzume.mods.FantasyDesire.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tennouboshiuzume.mods.FantasyDesire.client.renderer.VoidFlameRenderer;

// 拼尽全力无法战胜末影龙大人，只能Mixin
/** 末影龙不经过 LivingEntityRenderer，在原版准备好龙模型与根姿态后补充表面采集。 */
@Mixin(EnderDragonRenderer.class)
public abstract class EnderDragonRendererMixin {
    @Shadow
    @Final
    private EnderDragonRenderer.DragonModel model;

    @Shadow
    @Final
    private static ResourceLocation DRAGON_LOCATION;

    @Inject(method = "render(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EnderDragonRenderer$DragonModel;prepareMobModel(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;FFF)V", shift = At.Shift.AFTER))
    private void fantasydesire$captureVoidFlame(EnderDragon dragon, float entityYaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo callback) {
        // 消散阶段的深度裁切由原版控制，避免完整附着模型重新填回已经消失的部位。
        if (dragon.dragonDeathTime > 0 || !dragon.isAlive()) {
            return;
        }
        // 只复用龙的身体模型，水晶光束、发光眼睛与死亡光芒不会重复进入采集。
        VoidFlameRenderer.capture(dragon, model, DRAGON_LOCATION, poseStack, packedLight, partialTick);
    }
}
