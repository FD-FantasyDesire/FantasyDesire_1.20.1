package tennouboshiuzume.mods.FantasyDesire.potioneffect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.slasharts.FreezeZero;
import tennouboshiuzume.mods.FantasyDesire.specialeffects.effects.overcold.OverColdEffects;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.FDTargetSelector;

import java.util.List;

public class FrostStormEffect extends MobEffect {
    public FrostStormEffect() {
        super(MobEffectCategory.HARMFUL, 0x99FFFF);
    }

    public static float getFieldRadius(int amplifier) {
        return Math.max(4.0F, Math.min(4.0F + amplifier * 3.0F, 16.0F));
    }

    public static Vec3 getFieldCenter(LivingEntity entity) {
        return getFieldCenter(entity, 1.0F);
    }

    public static Vec3 getFieldCenter(LivingEntity entity, float partialTick) {
        return entity.getPosition(partialTick).add(0.0, entity.getBbHeight() * 0.5, 0.0);
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity.level() instanceof ServerLevel serverLevel) {
            double r = getFieldRadius(amplifier);
            double yPos = entity.getY() + entity.getBbHeight() + Math.min(4 + amplifier, 8);
            // 冰封风暴边缘视觉粒子
            if (entity.tickCount % 5 == 0) {
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
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, x, yPos, z, 1, 0, 0, 0, 0.1);
            }

            // 每2tick生成幻影剑攻击，需要手持该武器
            if (entity.tickCount % 2 == 0) {
                CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher
                        .of(entity)
                        .requireTranslation("item.fantasydesire.over_cold")
                        .match();

                if (ctx == null) {
                    entity.removeEffect(this);
                    return;
                }
                int evolutionTier = OverColdEffects.getEvolutionTier(ctx.fantasyState.getSpecialType());
                // 取差值扣减进化点数
                if (!CapabilityUtils.tryConsumeSpecialCharge(ctx.fantasyState,
                        Math.max(amplifier - evolutionTier, 0), entity, null)) {
                    entity.removeEffect(this);
                    return;
                }
                double range = r;
                List<LivingEntity> enemies = FDTargetSelector.getNearbyLivingEntities(
                        entity, getFieldCenter(entity), range, true, null);
                int swordCount = 1 + amplifier / 2;
                for (int s = 0; s < swordCount; s++) {
                    Vec3 spawnPos;
                    LivingEntity target = null;

                    if (!enemies.isEmpty()) {
                        for (LivingEntity tarEntity : enemies) {
                            tarEntity.addEffect(
                                    new MobEffectInstance(FDPotionEffects.FROST_BITE.get(), 20 * 5, evolutionTier,
                                            true, false, true));
                        }
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
                    ss.setSpeed(2.0f);
                    ss.setDelay(20);
                    ss.setParticleType(ParticleTypes.SNOWFLAKE);
                    ss.setHasTail(true);
                    ss.setStandbyMode(EntityFDPhantomSword.StandbyMode.WORLD);
                    ss.setMovingMode(EntityFDPhantomSword.MovingMode.NORMAL);
                    ss.setNoClip(false);
                    ss.setColor(0x6699FF);
                    ss.setDelayTicks(0);
                    ss.setSeekDelay(0);

                    if (target != null) {
                        Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0)
                                .subtract(spawnPos).normalize();
                        float yaw = (float) (Math.atan2(-toTarget.x, toTarget.z) * (180f / Math.PI));
                        float pitch = (float) (Math.asin(-toTarget.y) * (180f / Math.PI));
                        ss.setStandbyYawPitch(yaw, pitch);
                        ss.setTargetId(target.getId());
                    } else {
                        float randomYaw = entity.getRandom().nextFloat() * 360.0f;
                        float basePitch = 85.0f;
                        float pitchDeviation = (entity.getRandom().nextFloat() - 0.5f) * 20.0f; // ±10度偏差
                        float pitch = basePitch + pitchDeviation;
                        ss.setStandbyYawPitch(randomYaw, pitch);
                    }

                    ss.setPos(spawnPos);
                    ss.tryInit();
                    entity.level().addFreshEntity(ss);
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
