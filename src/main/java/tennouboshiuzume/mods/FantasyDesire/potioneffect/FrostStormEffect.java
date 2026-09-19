package tennouboshiuzume.mods.FantasyDesire.potioneffect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.config.FDConfig;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDAttributes;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.specialeffects.effects.overcold.OverColdEffects;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.FDTargetSelector;

import java.util.List;
import java.util.UUID;

public class FrostStormEffect extends MobEffect {
    // 数值来自 FDConfig（服务端同步配置），使用处实时读取
    private static final FDConfig.FrostStorm FROST_STORM = FDConfig.FROST_STORM;
    /** 半径属性修改器 UUID（客户端渲染器凭此检测激活） */
    public static final UUID RADIUS_MODIFIER_UUID = UUID.fromString("f3a9c7d1-4f2b-4e8a-9c1d-5b7e3a2f6b40");
    /** 强度属性修改器 UUID */
    public static final UUID STRENGTH_MODIFIER_UUID = UUID.fromString("8e5d2b9a-6c4f-4a1e-b8d3-7f0a9e2c5d60");
    private static final float INITIAL_STORM_STRENGTH = 1.0F;

    public FrostStormEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x99FFFF);
    }

    public static float getFieldRadius(int amplifier) {
        float base = FROST_STORM.radiusBase();
        float radius = base + Math.max(amplifier, 0) * FROST_STORM.radiusPerAmp();
        return Math.min(radius, FROST_STORM.radiusCap());
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
            // 强度从 1 开始，风暴持续期间随时间增长，并同步到属性供伤害与客户端读取。
            float strength = growStormStrength(entity);
            syncStormAttributes(entity, amplifier, strength);
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
                        entity, getFieldCenter(entity, 1.0F), range, true, null);
                int swordCount = 1 + amplifier / 2;
                for (int s = 0; s < swordCount; s++) {
                    Vec3 spawnPos;
                    LivingEntity target = null;

                    if (!enemies.isEmpty()) {
                        for (LivingEntity tarEntity : enemies) {
                            tarEntity.addEffect(
                                    new MobEffectInstance(FDPotionEffects.FROST_BITE.get(),
                                            FROST_STORM.biteDuration(), evolutionTier,
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
                    // 属性最终值包含风暴成长和其他加值修改器。
                    double effectiveStrength = FDAttributes.getStormStrength(entity);
                    ss.setDamage((2.0 + amplifier * 2.0) * effectiveStrength);
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

    /**
     * 服务端：把当前效果的半径/强度写入实体自定义属性修改器（值不变时跳过，避免每 tick 发同步包）。
     * 客户端渲染器据此检测激活并获取半径（见 FrostStormFieldRenderer）。
     */
    public static void syncStormAttributes(LivingEntity entity, int amplifier, float strength) {
        AttributeInstance radiusAttr = entity.getAttribute(FDAttributes.FROST_STORM_RADIUS.get());
        AttributeInstance strengthAttr = entity.getAttribute(FDAttributes.FROST_STORM_STRENGTH.get());
        if (radiusAttr == null || strengthAttr == null) {

            return;
        }
        float radius = getFieldRadius(amplifier);
        AttributeModifier radiusMod = radiusAttr.getModifier(RADIUS_MODIFIER_UUID);
        if (radiusMod == null || radiusMod.getAmount() != radius) {
            radiusAttr.removeModifier(RADIUS_MODIFIER_UUID);
            radiusAttr.addTransientModifier(new AttributeModifier(RADIUS_MODIFIER_UUID,
                    "fd_frost_storm_radius", radius, AttributeModifier.Operation.ADDITION));

        }
        // 属性基值为 1，修改器只记录药水持续期间产生的成长量。
        double growthAmount = Math.max(0.0D, strength - INITIAL_STORM_STRENGTH);
        AttributeModifier strengthMod = strengthAttr.getModifier(STRENGTH_MODIFIER_UUID);
        if (strengthMod == null || strengthMod.getAmount() != growthAmount) {
            strengthAttr.removeModifier(STRENGTH_MODIFIER_UUID);
            strengthAttr.addTransientModifier(new AttributeModifier(STRENGTH_MODIFIER_UUID,
                    "fd_frost_storm_strength", growthAmount, AttributeModifier.Operation.ADDITION));

        }
    }

    /** 首次获得风暴时强度为 1，之后每 tick 增长，达到配置上限后保持。 */
    private static float growStormStrength(LivingEntity entity) {
        AttributeInstance strengthAttr = entity.getAttribute(FDAttributes.FROST_STORM_STRENGTH.get());
        double currentGrowth = 0.0D;
        if (strengthAttr != null) {
            AttributeModifier modifier = strengthAttr.getModifier(STRENGTH_MODIFIER_UUID);
            if (modifier != null) {
                currentGrowth = modifier.getAmount();
            }
        }
        float cap = Math.max(FROST_STORM.strengthCap(), INITIAL_STORM_STRENGTH);
        return (float) Math.min(INITIAL_STORM_STRENGTH + currentGrowth
                + FROST_STORM.strengthGrowthPerSecond() / 20.0F, cap);
    }

    /** 效果移除/过期时清理属性修改器，避免客户端残留渲染 */
    @Override
    public void removeAttributeModifiers(LivingEntity entity,
            net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        AttributeInstance radiusAttr = entity.getAttribute(FDAttributes.FROST_STORM_RADIUS.get());
        if (radiusAttr != null) {
            radiusAttr.removeModifier(RADIUS_MODIFIER_UUID);
        }
        AttributeInstance strengthAttr = entity.getAttribute(FDAttributes.FROST_STORM_STRENGTH.get());
        if (strengthAttr != null) {
            strengthAttr.removeModifier(STRENGTH_MODIFIER_UUID);
        }
    }

}
