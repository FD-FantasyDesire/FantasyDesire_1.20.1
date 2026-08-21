package tennouboshiuzume.mods.FantasyDesire.specialeffects.effects.puresnow;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import org.apache.commons.lang3.RandomUtils;

import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.BladeStandEntity;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.mehvahdjukaar.moonlight.api.events.forge.LightningStruckBlockEvent;
import net.minecraft.advancements.critereon.LightningStrikeTrigger;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.config.FDConfig;
import tennouboshiuzume.mods.FantasyDesire.damagesource.FDDamageSource;
import tennouboshiuzume.mods.FantasyDesire.data.builtin.FantasySlashBladeBuiltInRegistry;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDRainbowPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDSlashEffect;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffectsRegistry;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ColorUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.FDAttackManager;
import tennouboshiuzume.mods.FantasyDesire.utils.ItemUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.VecMathUtils;

@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PureSnowEffects {
    // 数值来自 FDConfig（服务端同步配置），使用处实时读取
    private static final FDConfig.RainbowFlux RAINBOW_FLUX = FDConfig.RAINBOW_FLUX;
    private static final FDConfig.ColorFlux COLOR_FLUX = FDConfig.COLOR_FLUX;
    private static final String TRANSLATION_KEY = "item.fantasydesire.pure_snow";

    // 虹光通量
    @SubscribeEvent
    public static void updateEvent(SlashBladeEvent.UpdateEvent event) {
        ItemStack blade = event.getBlade();
        if (!(blade.getItem() instanceof ItemSlashBlade))
            return;
        if (!(event.getEntity() instanceof Player player))
            return;
        if (player.level().isClientSide())
            return;
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(blade, player)
                .requireSE(FDSpecialEffectsRegistry.RainbowFlux)
                .match();
        if (ctx == null)
            return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        int eachColorZone = 15;
        int totalSteps = eachColorZone * 7;
        long tickCount = player.tickCount;
        int timeStep = (int) (tickCount % totalSteps);
        int color = ColorUtils.getSmoothTransitionColor(timeStep, totalSteps, true);
        state.setColorCode(color);
        if (!(blade.getItem() instanceof ItemFantasySlashBlade))
            return;
        // 设置特殊攻击效果为当前颜色对应的伤害类型，专属效果
        CapabilityUtils.BladeContext seCtx = CapabilityUtils.SEConditionMatcher.of(blade, player)
                .requireTranslation(TRANSLATION_KEY)
                .requireSE(FDSpecialEffectsRegistry.RainbowFlux)
                .match();
        if (seCtx != null) {
            IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
            int stepsPerType = totalSteps / 7;
            int damageTypeIndex = ((timeStep + stepsPerType / 2) * 7 / totalSteps) % 7;
            fdState.setSpecialAttackEffect(damageTypes[damageTypeIndex]);
        }
    }

    // 虹羽七刃剑 SA或者棱光通量获得 配合虹光通量二效
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSlash(SlashBladeEvent.DoSlashEvent event) {
        ItemStack blade = event.getBlade();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade))
            return;
        if (!(event.getUser() instanceof Player player))
            return;
        if (!event.getUser().hasEffect(FDPotionEffects.RAINBOW_SEVEN_EDGE.get()))
            return;
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(blade, player)
                .requireTranslation(TRANSLATION_KEY)
                .requireSE(FDSpecialEffectsRegistry.RainbowFlux)
                .match();
        if (ctx == null)
            return;

        double damage = event.getDamage();
        String fdDamageType = fdState.getSpecialAttackEffect();
        if (fdDamageType != null && !fdDamageType.equals("Null")) {
            DamageSource fds = FDDamageSource.getEntityDamageSource(player.level(),
                    FDDamageSource.fromString(fdDamageType), player);
            FDAttackManager.areaAttackWithSource(event.getUser(), KnockBacks.cancel.action,
                    (float) (damage * RAINBOW_FLUX.areaDamageMult()), true,
                    true, false, null, fds);
        }
        for (int i = 0; i < 7; i++) {
            int cl = ColorUtils.getSmoothTransitionColor(i, 7, true);
            EntityFDSlashEffect jc = new EntityFDSlashEffect(FDEntitys.FDSlashEffect.get(), player.level());
            Vec3 adds = new Vec3(0, 0, jc.getBbHeight() / 2).yRot((float) Math.toRadians(360 / 7 * i));
            adds = VecMathUtils.rotateAroundAxis(adds,
                    new Vec3(0, 0, 1).yRot((float) Math.toRadians(-player.getYRot())), event.getRoll());
            Vec3 pos = player.position().add(adds);
            jc.setPos(pos.x, pos.y + player.getBbHeight() / 2, pos.z);
            jc.setOwner(null);
            jc.setRotationRoll(event.getRoll());
            jc.setYRot(player.getYRot());
            jc.setRotationOffset((float) 360 / 7 * i + player.getYRot());
            jc.setXRot(0);
            jc.setColor(cl);
            jc.setMute(true);
            jc.setIsCritical(false);
            jc.setDamage(0);
            jc.setKnockBack(KnockBacks.cancel);
            jc.setDisableSLevelCritParticles(true); // Disable S.Level crit particles
            if (player != null) {
                player.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT).ifPresent((rank) -> {
                    jc.setRank(rank.getRankLevel(player.level().getGameTime()));
                });
            }
            player.level().addFreshEntity(jc);
        }
        event.setCanceled(true);
    }

    // 全色汇流
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHit(SlashBladeEvent.HitEvent event) {
        if (!(event.getUser() instanceof Player player))
            return;
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(event.getBlade(), player)
                .requireTranslation(TRANSLATION_KEY)
                .requireSE(FDSpecialEffectsRegistry.ColorFlux)
                .match();
        if (ctx == null)
            return;
        IFantasySlashBladeState fdState = ctx.fantasyState;
        ISlashBladeState state = ctx.state;
        CapabilityUtils.addSpecialCharge(fdState, 1);
        LivingEntity target = event.getTarget();
        if (fdState.getSpecialCharge() >= fdState.getMaxSpecialCharge()) {
            CapabilityUtils.tryConsumeSpecialCharge(fdState, fdState.getMaxSpecialCharge());
            if (target == null)
                return;
            float baseModif = state.getDamage();
            float magicDamage = COLOR_FLUX.swordDamageBase()
                    + (baseModif * COLOR_FLUX.swordDamageAttackRatio());
            float directionAngel = RandomUtils.nextBoolean() ? 120f : -120f;
            for (int i = 0; i < 7; i++) {
                Vec3 direction = new Vec3(0, 0, 1)
                        .yRot((float) Math.toRadians(360 / 7 * i + RandomUtils.nextInt(0, 15))).scale(1.5);
                Vec3 spawnPos = target.position().add(0, target.getBbHeight() / 2, 0).add(direction);
                Vec3 lookVec = target.position().add(0, target.getBbHeight() / 2, 0).subtract(spawnPos);
                float[] yawPitch = VecMathUtils.getYawPitchFromVec(lookVec);
                float yaw = yawPitch[0] + directionAngel;
                float pitch = yawPitch[1];
                EntityFDPhantomSword ss = new EntityFDPhantomSword(
                        FDEntitys.FDPhantomSword.get(), player.level());
                ss.setDelay(200);
                ss.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                ss.setDelayTicks(0);
                ss.setSeekDelay(2);
                ss.setStandbyMode(EntityFDPhantomSword.StandbyMode.WORLD);
                ss.setStandbyYawPitch(yaw, pitch);
                ss.setColor(state.getColorCode());
                ss.setDamage(magicDamage);
                ss.setRoll(45f);
                ss.setSeekAngle(36);
                ss.setDamageType(FDDamageSource.fromString(damageTypes[i]).location().toString());
                ss.setScale(0.75f);
                ss.setSpeed(1f);
                ss.setHasTail(true);
                ss.setNoClip(true);
                ss.setOwner(player);
                ss.setTargetId(target.getId());
                ss.setMovingMode(EntityFDPhantomSword.MovingMode.SEEK);
                ss.tryInit();
                player.level().addFreshEntity(ss);
            }
        }
        if (player.hasEffect(FDPotionEffects.RAINBOW_SEVEN_EDGE.get())) {
            HitEffect(target, (float) (1 + COLOR_FLUX.hitEffectRadiusChargeScale() * fdState.getSpecialCharge()),
                    state.getColorCode());
        }
    }

    private static void HitEffect(Entity entity, float radiusMult, int color) {
        Random rd = new Random();
        float halfLength = (entity.getBbHeight() + entity.getBbWidth()) / 3 * radiusMult;
        Vec3 base = new Vec3(0, 0, halfLength)
                .yRot((float) Math.toRadians(rd.nextInt(360))).xRot((float) Math.toRadians(rd.nextInt(360)));
        Vec3 start = entity.position().add(base).add(0, entity.getBbHeight() / 2, 0);
        Vec3 end = entity.position().add(base.scale(-1)).add(0, entity.getBbHeight() / 2, 0);
        ParticleUtils.spwanBladeRiftParticles(entity.level(), start, end, 20, 1.5f, 0.03f,
                0xFFFFFF,
                color);
    }

    // 棱光通量
    @SubscribeEvent
    public static void summonedSwordHit(SlashBladeEvent.SummonedSwordOnHitEntityEvent event) {
        if (event.getSummonedSword().getOwner() instanceof LivingEntity attacker) {
            CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher
                    .of(attacker.getMainHandItem(), attacker)
                    .requireTranslation(TRANSLATION_KEY)
                    .requireSE(FDSpecialEffectsRegistry.PrismFlux)
                    .match();
            if (ctx != null) {
                attacker.addEffect(new MobEffectInstance(FDPotionEffects.RAINBOW_SEVEN_EDGE.get(), 20 * 3, 0, false,
                        false));
            }
        }
    }

    // 雷击合成
    // 为什么闪电攻击不选择ItemFrame及其扩展类，，，
    @SubscribeEvent
    public static void bladeStandThunderStrike(LightningStruckBlockEvent event) {
        AABB area = event.getEntity().getBoundingBox().inflate(1.0);
        List<BladeStandEntity> stands = event.getLevel().getEntitiesOfClass(BladeStandEntity.class, area);
        if (stands.isEmpty())
            return;
        for (BladeStandEntity stand : stands) {
            ItemStack blade = stand.getItem();
            ISlashBladeState state = CapabilityUtils.getBladeState(blade);
            if (!state.hasSpecialEffect(FDSpecialEffectsRegistry.RainbowFlux.getId()))
                return;
            BlockPos beaconPos = stand.blockPosition().below();
            BlockState blockState = stand.level().getBlockState(beaconPos);
            if (!(blockState.getBlock() instanceof BeaconBlock))
                return;
            ItemStack targetBlade = FantasyDesire.getBladeAsRegistry(
                    stand.level(),
                    FantasySlashBladeBuiltInRegistry.PureSnow);
            ISlashBladeState targetState = CapabilityUtils.getBladeState(targetBlade);
            if (state.getTranslationKey().equals(targetState.getTranslationKey()))
                return;
            stand.setItem(ItemUtils.dataBakeBlade(blade, targetBlade));
            for (int i = 0; i < 27; i++) {
                Vec3 base = new Vec3(0, 0, 8);
                Vec3 start = stand.position().add(0, stand.getBbHeight() / 2, 0);
                Vec3 end = base.yRot((float) Math.toRadians(RandomUtils.nextInt(0, 360)))
                        .xRot((float) Math.toRadians(RandomUtils.nextInt(0, 360))).add(start);
                ParticleUtils.LightBoltParticles(stand.level(), start, end,
                        ColorUtils.getSmoothTransitionColor(i, 27, false),
                        0.1f, 60, 1f, true, 0.8, 8);
            }
            if ((stand.level() instanceof ServerLevel serverLevel)) {
                serverLevel.setWeatherParameters(
                        6000, 0,
                        false, false);
            }
        }
    }

    private static String[] damageTypes = { "wrath", "lust", "sloth", "gluttony", "gloom", "pride", "envy" };
}