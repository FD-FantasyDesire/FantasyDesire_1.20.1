package tennouboshiuzume.mods.FantasyDesire.slasharts;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDRainbowPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ColorUtils;

import java.util.Random;

public class RainbowStar {
    public static boolean AntiNTR(LivingEntity entity) {
        return CapabilityUtils.SEConditionMatcher.of(entity)
                .requireTranslation("item.fantasydesire.pure_snow")
                .match() != null;
    }

    public static void RainbowStar(LivingEntity player, ItemStack blade) {
        if (blade.getItem() instanceof ItemSlashBlade) {
            ISlashBladeState state = CapabilityUtils.getBladeState(blade);
            if (state.getTranslationKey().equals("item.fantasydesire.pure_snow")) {
                if (player.level().isClientSide())
                    return;
                if (!(player instanceof Player))
                    return;
                float baseModif = state.getDamage();
                float magicDamage = 1.0f + (baseModif / 2.0f);
                Random random = new Random();
                
                // 检查玩家是否潜行
                boolean isSneaking = player.isShiftKeyDown();
                
                for (int i = 0; i < 21; i++) {
                    float yaw, pitch;
                    Vec3 pos;
                    
                    if (!isSneaking) {
                        // 当不潜行时，保持原始生成位置但朝向玩家视线方向并添加随机偏移
                        Vec3 lookVec = player.getLookAngle();
                        Vec3 playerPos = player.position();
                        
                        // 使用射线追踪找到第一个方块落点
                        Vec3 rayStart = playerPos.add(0, player.getEyeHeight(), 0);
                        Vec3 rayEnd = rayStart.add(lookVec.scale(32.0)); // 射线追踪最多32格
                        BlockHitResult rayResult = player.level().clip(new ClipContext(rayStart, rayEnd,
                                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                        
                        // 如果找到落点则使用落点，否则使用射线终点
                        Vec3 targetPoint = rayResult.getType() == HitResult.Type.BLOCK ? rayResult.getLocation() : rayEnd;
                        
                        // 在水平面上添加8格半径内的随机偏移
                        double offsetX = (random.nextDouble() - 0.5) * 16; // -8到+8
                        double offsetZ = (random.nextDouble() - 0.5) * 16; // -8到+8
                        targetPoint = targetPoint.add(offsetX, 0, offsetZ); // 保持Y高度不变
                        
                        // 计算从原始生成点到新目标点的方向
                        Vec3 originalSpawnPos = playerPos.add(new Vec3(0, 24, 0));
                        Vec3 directionToTarget = targetPoint.subtract(originalSpawnPos).normalize();
                        
                        // 计算朝向新目标的偏航角和俯仰角
                        yaw = (float) (Math.atan2(directionToTarget.z, directionToTarget.x) * 180.0 / Math.PI) - 90;
                        double horizontalDistance = Math.sqrt(directionToTarget.x * directionToTarget.x + directionToTarget.z * directionToTarget.z);
                        pitch = (float) -(Math.atan2(directionToTarget.y, horizontalDistance) * 180.0 / Math.PI);
                        
                        // 应用与原始逻辑相似的随机变化
                        yaw += (float) (random.nextGaussian() * 3);
                        pitch += (float) random.nextGaussian() * 3;
                        
                        // 保持原始生成位置并添加小的随机偏移
                        pos = originalSpawnPos.add(
                                new Vec3(random.nextGaussian() * 3, random.nextGaussian() * 3, random.nextGaussian() * 3));
                    } else {
                        // 潜行时的原始逻辑
                        yaw = (float) random.nextInt(360);
                        pitch = 90 + (float) random.nextGaussian() * 3;
                        Vec3 originalPos = player.position().add(new Vec3(0, 32, 0));
                        pos = originalPos.add(
                                new Vec3(random.nextGaussian() * 3, random.nextGaussian() * 3, random.nextGaussian() * 3));
                    }
                    
                    EntityFDRainbowPhantomSword ss = new EntityFDRainbowPhantomSword(
                            FDEntitys.FDRainbowPhantomSword.get(), player.level());
                    ss.setDelay(200);
                    ss.setPos(pos);
                    ss.setDelayTicks(5 + i);
                    ss.setStandbyMode("WORLD");
                    ss.setStandbyYawPitch(yaw, pitch);
                    ss.setYRot(yaw);
                    ss.setXRot(pitch);
                    ss.setColor(ColorUtils.getSmoothTransitionColor(i, 21, true));
                    ss.setDamage(magicDamage);
                    ss.setRoll(random.nextInt(360));
                    ss.setScale(2f);
                    ss.setSpeed(5f);
                    ss.setHasTail(true);
                    ss.setOwner(player);
                    player.level().addFreshEntity(ss);
                }
                player.addEffect(new MobEffectInstance(FDPotionEffects.RAINBOW_SEVEN_EDGE.get(), 20 * 14, 0));
            }
        }
    }
}