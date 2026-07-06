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
import tennouboshiuzume.mods.FantasyDesire.client.particle.FlatSpreadingRingParticleOptions;
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
    // 配置项目
    // 灵魂之盾
    private static final float COUNTER_CHANCE_MIN = 5.0f;
    private static final float COUNTER_CHANCE_MAX = 95.0f;
    private static final float COUNTER_CHANCE_CAP_RATIO = 0.75f;
    private static final float COUNTER_FAIL_DAMAGE_CAP = 5.0f;
    private static final int COUNTER_FAIL_CHARGE_CAP = 60;
    private static final float COUNTER_FAIL_OVERFLOW_TO_CHARGE = 1.0f;
    private static final int COUNTER_FIXED_CHARGE = 6;
    private static final int COUNTER_SUCCESS_CHARGE_CAP = 60;

    // 不屈之魂
    private static final float IMMORTAL_REVIVE_CHARGE_RATIO = 0.20f;
    private static final int IMMORTAL_REVIVE_SOUL_COST = 1000;

    // 暴君一击
    private static final float TYRANT_TRIGGER_RATIO = 0.95f;
    private static final float TYRANT_CONSUME_RATIO = 0.20f;
    private static final float TYRANT_HEALTH_PERCENT = 0.08f;
    private static final float TYRANT_CHARGE_DAMAGE_SCALE = 0.25f;
    private static final double TYRANT_PHANTOM_DAMAGE = 1.0D;

    // 灵魂之盾 攻击阶段触发
    @SubscribeEvent
    public static void OnBypassAttack(LivingAttackEvent event) {
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
        float counterProgress = Mth.clamp(chargeRatio / COUNTER_CHANCE_CAP_RATIO, 0.0f, 1.0f);
        float counterChance = COUNTER_CHANCE_MIN
                + counterProgress * (COUNTER_CHANCE_MAX - COUNTER_CHANCE_MIN);
        CapabilityUtils.addSpecialCharge(fdState, COUNTER_FIXED_CHARGE);
        if (!MathUtils.RandomCheck(counterChance))
            return;
        Vec3 VecToAttacker = VecMathUtils.calculateDirectionVec(player, attacker);
        float[] YP = VecMathUtils.getYawPitchFromVec(VecToAttacker);
        // 自动反击
        player.playSound(SoundEvents.SHIELD_BLOCK, 0.5F, 2.0F);
        AddonSlashUtils.doAddonFDSlash(player, random.nextInt(180), YP[0], YP[1], 0x00FFFF, 0, VecToAttacker,
                false, false, 0.33f * event.getAmount(), KnockBacks.cancel, 1.5f, 60,
                FDDamageSource.DIMENSION.location().toString());
        int chargeAmount = (int) (event.getAmount());
        CapabilityUtils.addSpecialCharge(fdState, Math.min(chargeAmount, COUNTER_SUCCESS_CHARGE_CAP));
        event.setCanceled(true);
    }

    // 伤害结算触发
    @SubscribeEvent
    public static void OnBypassAttack(LivingDamageEvent event) {
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
        if (incomingDamage <= COUNTER_FAIL_DAMAGE_CAP)
            return;
        float overflow = incomingDamage - COUNTER_FAIL_DAMAGE_CAP;
        int chargeAmount = Mth.ceil(overflow * COUNTER_FAIL_OVERFLOW_TO_CHARGE);
        chargeAmount = Mth.clamp(chargeAmount, 0, COUNTER_FAIL_CHARGE_CAP);
        CapabilityUtils.addSpecialCharge(fdState, chargeAmount);
        // 减缓防反失败后的伤害
        event.setAmount(COUNTER_FAIL_DAMAGE_CAP);
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
        if (!CapabilityUtils.tryConsumeProudSoul(state, IMMORTAL_REVIVE_SOUL_COST, player, null)) {
            return;
        }
        // 永久提升拔刀剑0.67攻击力，最大耐久+5，回复50%血量，清除所有Debuff，获得6秒再生5级和抗性5级
        state.setBaseAttackModifier(baseattack + 0.67f);
        state.setMaxDamage(state.getMaxDamage() + 9);
        CapabilityUtils.addSpecialCharge(fdState,
                Mth.ceil(fdState.getMaxSpecialCharge() * IMMORTAL_REVIVE_CHARGE_RATIO));
        player.setHealth(player.getMaxHealth() / 2.0F);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 6, 4));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 6, 4));
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
        if (CapabilityUtils.getSpecialChargePercentage(fdState) < TYRANT_TRIGGER_RATIO && ctxCheat == null)
            return;
        int consumeAmount = Mth.ceil(fdState.getMaxSpecialCharge() * TYRANT_CONSUME_RATIO);
        if (!CapabilityUtils.tryConsumeSpecialCharge(fdState, consumeAmount) && ctxCheat == null)
            return;
        LivingEntity target = event.getTarget();
        RandomSource random = target.getRandom();
        float damage = target.getMaxHealth() * TYRANT_HEALTH_PERCENT + consumeAmount * TYRANT_CHARGE_DAMAGE_SCALE;
        spawnTyrantStrikePhantomSword(player, target, state, random, damage + TYRANT_PHANTOM_DAMAGE);
    }

    // SA联动 彗星猛击
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
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(entity)
                .allowBothHands()
                .requireTranslation("item.fantasydesire.chikeflare")
                .match();
        float weaponDamage = 0;
        if (ctx != null) {
            weaponDamage = (ctx.state.getBaseAttackModifier() + ctx.state.getAttackAmplifier()) * 10f;
        }
        float explosionRadius = 15.0f;
        ServerLevel serverLevel = (ServerLevel) entity.level();
        for (int i = 0; i < 30; i++) {
            double r = Math.sqrt(serverLevel.random.nextDouble()) * explosionRadius;
            double theta = serverLevel.random.nextDouble() * 2 * Math.PI;
            double px = entity.getX() + r * Math.cos(theta);
            double pz = entity.getZ() + r * Math.sin(theta);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, px, entity.getY() + 0.1, pz, 1, 0, 0, 0, 0);
        }
        // 使该范围内敌人受到体力值上限10%+ 本次撞击伤害 + weaponDamage的次元伤害
        List<LivingEntity> enemies = FDTargetSelector.getNearbyLivingEntities(entity, explosionRadius, false, null);
        for (LivingEntity target : enemies) {
            float damage = target.getMaxHealth() * 0.1f + event.getAmount() + weaponDamage;
            target.hurt(FDDamageSource.entityDamageSource(serverLevel, FDDamageSource.DIMENSION, entity), damage);
            spawnTyrantStrikePhantomSword(entity, target, ctx.state, target.getRandom(),
                    target.getMaxHealth() * 0.1f + event.getAmount() + weaponDamage);
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
