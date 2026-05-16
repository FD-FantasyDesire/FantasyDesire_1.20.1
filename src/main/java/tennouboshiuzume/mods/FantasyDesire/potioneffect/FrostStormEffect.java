package tennouboshiuzume.mods.FantasyDesire.potioneffect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.utils.FDTargetSelector;

import java.util.Comparator;
import java.util.List;

import io.netty.util.internal.MathUtil;

public class FrostStormEffect extends MobEffect {
    public FrostStormEffect() {
        super(MobEffectCategory.HARMFUL, 0x99FFFF);
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity.level() instanceof ServerLevel serverLevel) {
            double r = Math.min(4 + amplifier * 3.0, 16);
            double yPos = entity.getY() + entity.getBbHeight() + Math.min(4 + amplifier, 8);
            // 冰封风暴边缘视觉粒子
            if (entity.tickCount % 5 == 0) {
                // Generate a ring of smoke
                int particles = (int) (r * 8);
                for (int i = 0; i < particles; i++) {
                    double angle = 2 * Math.PI * i / particles;
                    double x = entity.getX() + r * Math.cos(angle);
                    double z = entity.getZ() + r * Math.sin(angle);
                    serverLevel.sendParticles(ParticleTypes.CLOUD, x, yPos, z, 1, 0, 0, 0, 0);
                }
            }
            // 内部雪景粒子
            int snowflakeCount = 1 + amplifier * 2;
            for (int i = 0; i < snowflakeCount; i++) {
                double angle = entity.getRandom().nextDouble() * 2 * Math.PI;
                double distance = entity.getRandom().nextDouble() * r;
                double x = entity.getX() + distance * Math.cos(angle);
                double z = entity.getZ() + distance * Math.sin(angle);
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, x, yPos, z, 1, 0, 0, 0, 0.1 * amplifier);
            }

            // 每2tick生成幻影剑攻击
            if (entity.tickCount % 2 == 0) {
                // 使用FDTargetSelector获取光环范围内所有敌人（需要视线检测）
                double range = r;
                List<LivingEntity> enemies = FDTargetSelector.getNearbyLivingEntities(
                        entity, range, true, null);

                // 计算同时发射的幻影剑数量：基础1把，每2级+1把
                int swordCount = 1 + amplifier / 2;

                for (int s = 0; s < swordCount; s++) {
                    Vec3 spawnPos;
                    LivingEntity target = null;

                    if (!enemies.isEmpty()) {
                        // 从总范围r内的敌人中随机选择一个
                        target = enemies.get(entity.getRandom().nextInt(enemies.size()));
                        // 在敌人头顶r/4范围内随机选取生成位置
                        double quarterRange = r / 4.0;
                        double spawnAngle = entity.getRandom().nextDouble() * 2 * Math.PI;
                        double spawnDist = entity.getRandom().nextDouble() * quarterRange;
                        double spawnX = target.getX() + spawnDist * Math.cos(spawnAngle);
                        double spawnZ = target.getZ() + spawnDist * Math.sin(spawnAngle);
                        spawnPos = new Vec3(spawnX, yPos, spawnZ);
                    } else {
                        // 没有敌人：在光环范围内随机选取生成位置
                        double spawnAngle = entity.getRandom().nextDouble() * 2 * Math.PI;
                        double spawnDist = entity.getRandom().nextDouble() * r;
                        double spawnX = entity.getX() + spawnDist * Math.cos(spawnAngle);
                        double spawnZ = entity.getZ() + spawnDist * Math.sin(spawnAngle);
                        spawnPos = new Vec3(spawnX, yPos, spawnZ);
                    }

                    // 生成幻影剑
                    EntityFDPhantomSword ss = new EntityFDPhantomSword(FDEntitys.FDPhantomSword.get(), entity.level());
                    ss.setOwner(entity);
                    ss.setIsCritical(false);
                    ss.setDamage(3.0 + amplifier * 5.0);
                    ss.setScale(0.66f);
                    ss.setSpeed(3.0f);
                    ss.setDelay(20); // 存在时间 20 tick
                    ss.setParticleType(ParticleTypes.SNOWFLAKE);
                    ss.setHasTail(true);
                    ss.setStandbyMode("WORLD");
                    ss.setMovingMode("NORMAL");
                    ss.setNoClip(false);
                    ss.setColor(0x6699FF);
                    ss.setDelayTicks(0); // 无发射延迟，立即发射
                    ss.setSeekDelay(0); // 立即开始追踪

                    if (target != null) {
                        // 有敌人，射向目标敌人
                        Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0)
                                .subtract(spawnPos).normalize();
                        float yaw = (float) (Math.atan2(-toTarget.x, toTarget.z) * (180f / Math.PI));
                        float pitch = (float) (Math.asin(-toTarget.y) * (180f / Math.PI));
                        ss.setStandbyYawPitch(yaw, pitch);
                        ss.setTargetId(target.getId());
                    } else {
                        // 没有敌人，随机向下方发射，随机偏差角度10
                        float randomYaw = entity.getRandom().nextFloat() * 360.0f;
                        float basePitch = 85.0f; // 接近垂直向下
                        float pitchDeviation = (entity.getRandom().nextFloat() - 0.5f) * 20.0f; // ±10度偏差
                        float pitch = basePitch + pitchDeviation;
                        ss.setStandbyYawPitch(randomYaw, pitch);
                    }

                    // 生成位置
                    ss.setPos(spawnPos);
                    ss.tryInit();
                    entity.level().addFreshEntity(ss);

                    // 生成时在生成位置施放环形粒子模仿阴云效果（0.5格半径）
                    int particles = 8;
                    for (int i = 0; i < particles; i++) {
                        double angle = 2 * Math.PI * i / particles;
                        double x = spawnPos.x + 0.5 * Math.cos(angle);
                        double z = spawnPos.z + 0.5 * Math.sin(angle);
                        serverLevel.sendParticles(ParticleTypes.CLOUD, x, yPos, z, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }
}
