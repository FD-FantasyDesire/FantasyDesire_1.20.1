package tennouboshiuzume.mods.FantasyDesire.specialeffects.effects.chikeflare;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.particle.FlatSpreadingRingParticleOptions;
import tennouboshiuzume.mods.FantasyDesire.config.FDConfig;
import tennouboshiuzume.mods.FantasyDesire.damagesource.FDDamageSource;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDSpearPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffectsRegistry;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.utils.*;

import java.util.List;

@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChikeFlareEffects {
    // 数值全部来自 FDConfig（服务端同步配置），使用处实时读取，不缓存
    private static final FDConfig.SoulShield SOUL_SHIELD = FDConfig.SOUL_SHIELD;
    private static final FDConfig.ImmortalSoul IMMORTAL_SOUL = FDConfig.IMMORTAL_SOUL;
    private static final FDConfig.TyrantStrike TYRANT_STRIKE = FDConfig.TYRANT_STRIKE;
    private static final FDConfig.CometElytra COMET_ELYTRA = FDConfig.COMET_ELYTRA;

    // 灵魂之盾 攻击阶段触发
    @SubscribeEvent
    public static void OnBypassAttack(LivingAttackEvent event) {
        if (event.getSource().is(DamageTypes.GENERIC_KILL))
            return;
        if (!(event.getEntity() instanceof Player player))
            return;
        if (player.level().isClientSide())
            return;
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(player)
                .requireTranslation("item.fantasydesire.chikeflare")
                .requireSE(FDSpecialEffectsRegistry.SoulShield)
                .match();
        if (ctx == null)
            return;
        LivingEntity attacker = getLivingAttacker(event.getSource(), player);
        if (attacker == null)
            return;
        IFantasySlashBladeState fdState = ctx.fantasyState;
        RandomSource random = player.getRandom();
        float chargeRatio = CapabilityUtils.getSpecialChargePercentage(fdState);
        float counterProgress = Mth.clamp(chargeRatio / SOUL_SHIELD.counterChanceCapRatio(), 0.0f, 1.0f);
        float counterChance = SOUL_SHIELD.counterChanceMin()
                + counterProgress * (SOUL_SHIELD.counterChanceMax() - SOUL_SHIELD.counterChanceMin());
        CapabilityUtils.addSpecialCharge(fdState, SOUL_SHIELD.counterFixedCharge());
        if (!MathUtils.RandomCheck(counterChance))
            return;
        Vec3 VecToAttacker = VecMathUtils.calculateDirectionVec(player, attacker);
        float[] YP = VecMathUtils.getYawPitchFromVec(VecToAttacker);
        // 自动反击
        player.playSound(SoundEvents.SHIELD_BLOCK, 0.5F, 2.0F);
        AddonSlashUtils.doAddonFDSlash(player, random.nextInt(180), YP[0], YP[1], 0x00FFFF, 0, VecToAttacker,
                false, false, SOUL_SHIELD.counterSuccessDamageRatio() * event.getAmount(), KnockBacks.cancel, 1.5f, 60,
                FDDamageSource.DIMENSION.location().toString());
        int chargeAmount = (int) (event.getAmount());
        CapabilityUtils.addSpecialCharge(fdState, Math.min(chargeAmount, SOUL_SHIELD.counterSuccessChargeCap()));
        event.setCanceled(true);
    }

    // 伤害结算触发
    @SubscribeEvent
    public static void OnBypassAttack(LivingDamageEvent event) {
        if (event.getSource().is(DamageTypes.GENERIC_KILL))
            return;
        if (!(event.getEntity() instanceof Player player))
            return;
        if (player.level().isClientSide())
            return;
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(player)
                .allowBothHands()
                .requireTranslation("item.fantasydesire.chikeflare")
                .requireSE(FDSpecialEffectsRegistry.SoulShield)
                .match();
        if (ctx == null)
            return;
        LivingEntity attacker = getLivingAttacker(event.getSource(), player);
        if (attacker == null)
            return;
        IFantasySlashBladeState fdState = ctx.fantasyState;
        float incomingDamage = event.getAmount();
        // 最终伤害大于5
        if (incomingDamage <= SOUL_SHIELD.counterFailDamageCap())
            return;
        float overflow = incomingDamage - SOUL_SHIELD.counterFailDamageCap();
        int chargeAmount = Mth.ceil(overflow * SOUL_SHIELD.counterFailOverflowToCharge());
        chargeAmount = Mth.clamp(chargeAmount, 0, SOUL_SHIELD.counterFailChargeCap());
        CapabilityUtils.addSpecialCharge(fdState, chargeAmount);
        // 减缓防反失败后的伤害
        event.setAmount(SOUL_SHIELD.counterFailDamageCap());
    }

    // 不屈之魂
    @SubscribeEvent
    public static void OnDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;
        if (player.level().isClientSide())
            return;
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(player)
                .allowBothHands()
                .requireTranslation("item.fantasydesire.chikeflare")
                .requireSE(FDSpecialEffectsRegistry.ImmortalSoul)
                .match();
        if (ctx == null)
            return;
        ISlashBladeState state = ctx.state;
        IFantasySlashBladeState fdState = ctx.fantasyState;
        float baseattack = state.getBaseAttackModifier();
        if (!CapabilityUtils.tryConsumeProudSoul(state, IMMORTAL_SOUL.reviveSoulCost(), player, null)) {
            return;
        }
        // 永久提升拔刀剑0.67攻击力，最大耐久+5，回复50%血量，清除所有Debuff，获得6秒再生5级和抗性5级
        state.setBaseAttackModifier(baseattack + IMMORTAL_SOUL.baseAttackBonus());
        state.setMaxDamage(state.getMaxDamage() + IMMORTAL_SOUL.maxDamageBonus());
        CapabilityUtils.addSpecialCharge(fdState,
                Mth.ceil(fdState.getMaxSpecialCharge() * IMMORTAL_SOUL.reviveChargeRatio()));
        player.heal(player.getMaxHealth() * IMMORTAL_SOUL.healthRatio());
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, IMMORTAL_SOUL.regenDuration(),
                IMMORTAL_SOUL.regenAmplifier()));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, IMMORTAL_SOUL.resistDuration(),
                IMMORTAL_SOUL.resistAmplifier()));
        event.setCanceled(true);
    }

    // 暴君一击
    @SubscribeEvent
    public static void OnHit(SlashBladeEvent.HitEvent event) {
        if (!(event.getUser() instanceof Player player))
            return;
        if (player.level().isClientSide())
            return;
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(player)
                .allowBothHands()
                .requireTranslation("item.fantasydesire.chikeflare")
                .requireSE(FDSpecialEffectsRegistry.TyrantStrike)
                .match();
        CapabilityUtils.BladeContext ctxCheat = CapabilityUtils.SEConditionMatcher.of(player)
                .allowBothHands()
                .requireTranslation("item.fantasydesire.chikeflare")
                .requireSE(FDSpecialEffectsRegistry.CheatRumble)
                .match();
        if (ctx == null)
            return;
        ISlashBladeState state = ctx.state;
        IFantasySlashBladeState fdState = ctx.fantasyState;
        if (CapabilityUtils.getSpecialChargePercentage(fdState) < TYRANT_STRIKE.triggerRatio() && ctxCheat == null)
            return;
        int consumeAmount = Mth.ceil(fdState.getMaxSpecialCharge() * TYRANT_STRIKE.consumeRatio());
        if (!CapabilityUtils.tryConsumeSpecialCharge(fdState, consumeAmount) && ctxCheat == null)
            return;
        LivingEntity target = event.getTarget();
        RandomSource random = target.getRandom();
        float damage = target.getMaxHealth() * TYRANT_STRIKE.healthPercent()
                + consumeAmount * TYRANT_STRIKE.chargeDamageScale();
        spawnTyrantStrikePhantomSword(player, target, state, random, damage + TYRANT_STRIKE.phantomDamage());
    }

    // SA 鞘翅滑翔 联动 彗星猛击
    @SubscribeEvent
    public static void OnElytraClashBlock(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player entity))
            return;
        if (!event.getSource().is(DamageTypes.FLY_INTO_WALL) && !event.getSource().is(DamageTypes.FALL))
            return;
        if (!entity.hasEffect(FDPotionEffects.COMET_ELYTRA.get()))
            return;
        if (entity.level().isClientSide())
            return;
        // 保留撞击伤害作为猛击倍率，但不承受该次坠落或撞墙伤害。
        float impactDamage = event.getAmount();
        event.setCanceled(true);
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(entity)
                .allowBothHands()
                .requireTranslation("item.fantasydesire.chikeflare")
                .match();
        float weaponDamage = 0;
        if (ctx != null) {
            weaponDamage = (ctx.state.getBaseAttackModifier() + ctx.state.getAttackAmplifier())
                    * COMET_ELYTRA.clashWeaponDamageMult();
        }
        float explosionRadius = COMET_ELYTRA.clashExplosionRadius();
        ServerLevel serverLevel = (ServerLevel) entity.level();

        // 使该范围内敌人受到体力值上限10%+ 本次撞击伤害 + weaponDamage的次元伤害
        List<LivingEntity> enemies = FDTargetSelector.getNearbyLivingEntities(entity, explosionRadius, false, null);
        for (LivingEntity target : enemies) {
            float damage = target.getMaxHealth() * COMET_ELYTRA.clashHealthPercent() + impactDamage
                    + weaponDamage;
            target.hurt(FDDamageSource.entityDamageSource(serverLevel, FDDamageSource.DIMENSION, entity), damage);
            if (ctx != null) {
                spawnTyrantStrikePhantomSword(entity, target, ctx.state, target.getRandom(),
                        target.getMaxHealth() * COMET_ELYTRA.clashHealthPercent() + impactDamage + weaponDamage);
            }
        }
        serverLevel.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2, 1);
        FlatSpreadingRingParticleOptions particleOptions = new FlatSpreadingRingParticleOptions(
                0xFFFF00, explosionRadius, 1.5f, 20);
        ParticleUtils.sendForceParticles(serverLevel, particleOptions,
                entity.getX(), entity.getY() + 0.1, entity.getZ(), 1, 0, 0, 0, 0, 64.0);
        entity.removeEffect(FDPotionEffects.COMET_ELYTRA.get());
    }

    public static void spawnTyrantStrikePhantomSword(Player player, LivingEntity target, ISlashBladeState state,
            RandomSource random, double damage) {
        float yaw = (float) random.nextInt(360);
        float pitch = 90f + (float) (random.nextGaussian() * 5f);
        float roll = (float) (random.nextInt(360) - 180);
        Vec3 basePos = new Vec3(0, 0, 1);
        Vec3 spawnPos = target.position().add(0, target.getBbHeight() / 2, 0)
                .add(basePos
                        .xRot((float) Math.toRadians(pitch))
                        .yRot((float) Math.toRadians(yaw))
                        .scale(30f));
        Vec3 lookVec = target.position().add(0, target.getBbHeight() / 2, 0).subtract(spawnPos).normalize();
        float lookYaw = (float) (Math.atan2(-lookVec.x, lookVec.z) * (180f / Math.PI));
        float lookPitch = (float) (Math.asin(-lookVec.y) * (180f / Math.PI));
        FlatSpreadingRingParticleOptions particleOptions = new FlatSpreadingRingParticleOptions(
                0xFFFF00, 3, 0.5f, 5);
        ParticleUtils.sendForceParticles((ServerLevel) player.level(),
                particleOptions, spawnPos.x, spawnPos.y + 0.1, spawnPos.z, 1, 0, 0,
                0, 0, 64.0);
        EntityFDSpearPhantomSword ss = new EntityFDSpearPhantomSword(
                tennouboshiuzume.mods.FantasyDesire.init.FDEntitys.FDSpearPhantomSword.get(), player.level());
        ss.setIsCritical(false);
        ss.setOwner(player);
        ss.setColor(state.getColorCode());
        ss.setRoll(roll);
        ss.setDamage(damage);
        ss.setSpeed(5);
        ss.setStandbyMode(EntityFDPhantomSword.StandbyMode.WORLD);
        ss.setMovingMode(EntityFDPhantomSword.MovingMode.SEEK);
        ss.setDamageType(FDDamageSource.DIMENSION.location().toString());
        ss.setSeekAngle(36);
        ss.setDelay(100);
        ss.setDelayTicks(0);
        ss.setNoClip(true);
        ss.setHasTail(true);
        ss.setScale(target.getBbHeight());
        ss.setTargetId(target.getId());
        ss.setStandbyYawPitch(lookYaw, lookPitch);
        ss.setPos(spawnPos);
        ss.tryInit();
        player.level().addFreshEntity(ss);
    }

    private static LivingEntity getLivingAttacker(DamageSource source, Player player) {
        if (source == null)
            return null;

        if (source.getEntity() instanceof LivingEntity attacker && attacker != player)
            return attacker;

        if (source.getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof LivingEntity owner
                && owner != player) {
            return owner;
        }

        return null;
    }
}
