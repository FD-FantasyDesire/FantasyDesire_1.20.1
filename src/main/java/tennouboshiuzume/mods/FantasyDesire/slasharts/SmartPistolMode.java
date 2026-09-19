package tennouboshiuzume.mods.FantasyDesire.slasharts;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.config.FDConfig;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDBFG;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.init.FDSlashArtRegistry;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffectsRegistry;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.FDTargetSelector;

public class SmartPistolMode {
    // 数值来自 FDConfig（服务端同步配置），使用处实时读取
    private static final FDConfig.OverCharge OVER_CHARGE = FDConfig.OVER_CHARGE;
    private static final FDConfig.TripleBullet TRIPLE_BULLET = FDConfig.TRIPLE_BULLET;
    private static final FDConfig.ExplosiveBullet EXPLOSIVE_BULLET = FDConfig.EXPLOSIVE_BULLET;
    private static final float NORMAL_BULLET_EXPLOSION_RADIUS = 0.0F;
    // 齐发自身的伤害与弹速固定，锁距和爆裂弹头参数继续复用弹种配置。
    private static final float CHARGE_SHOT_DAMAGE_RATIO = 3.0F;
    private static final float CHARGE_SHOT_ENCHANT_MULT_PER_LEVEL = 0.10F;
    private static final float CHARGE_SHOT_SPEED = 1.0F;
    private static final float CHARGE_SHOT_EXPLOSIVE_SPEED = 0.33F;
    private static final float BFG_REFINE_LINEAR = 0.1F;
    private static final float BFG_ENCHANT_MULT_PER_LEVEL = 0.25F;
    /** 非宙霆模式的额外伤害倍率；宙霆模式保持 1 倍并启用连锁。 */
    private static final float BFG_NON_THUNDER_DAMAGE_MULT = 1.5F;

    public static boolean AntiNTR(LivingEntity entity) {
        return CapabilityUtils.SEConditionMatcher.of(entity)
                .requireTranslation("item.fantasydesire.smart_pistol")
                .match() != null;
    }

    public static void TransformToA(ISlashBladeState state, IFantasySlashBladeState fdState) {
        state.setTexture(new ResourceLocation(FantasyDesire.MODID + ":models/smartpistol.png"));
        state.setSlashArtsKey(FDSlashArtRegistry.CHARGE_SHOT.getId());
        state.setColorCode(0x00FFFF);
    }

    public static void TransformToB(ISlashBladeState state, IFantasySlashBladeState fdState) {
        state.setTexture(new ResourceLocation(FantasyDesire.MODID + ":models/smartpistol_oc.png"));
        state.setSlashArtsKey(FDSlashArtRegistry.OVER_CHARGE.getId());
        state.setColorCode(0x99FF00);
    }

    public static void BFGShot(LivingEntity entity, ISlashBladeState state, IFantasySlashBladeState fdState) {
        if (!(entity instanceof Player player))
            return;

        int ammo = fdState.getSpecialCharge();
        if (ammo <= 0)
            return;

        ItemStack blade = player.getMainHandItem();
        boolean thunderOn = CapabilityUtils.SEConditionMatcher.of(blade, player)
                .requireSE(FDSpecialEffectsRegistry.ThunderBullet)
                .match() != null;
        float baseDamage = state.getBaseAttackModifier() + state.getAttackAmplifier();
        int refine = state.getRefine();
        float refineBonus = (float) (refine * BFG_REFINE_LINEAR + Math.sqrt(refine) * 1.5f);
        int enchantLevel = blade.getEnchantmentLevel(Enchantments.POWER_ARROWS);
        float enchantMultiplier = 1.0f + (enchantLevel * BFG_ENCHANT_MULT_PER_LEVEL);
        float finalDamage = (float) ((baseDamage + refineBonus) * enchantMultiplier * ammo
                * (thunderOn ? 1.0f : BFG_NON_THUNDER_DAMAGE_MULT));

        EntityFDBFG ss = new EntityFDBFG(FDEntitys.FDBFG.get(), player.level());
        ss.setIsCritical(false);
        ss.setOwner(player);
        ss.setColor(state.getColorCode());
        ss.setRoll(0);
        ss.setDamage(finalDamage);
        ss.setSpeed(1);
        ss.setStandbyMode(EntityFDPhantomSword.StandbyMode.PLAYER);
        ss.setMovingMode(EntityFDPhantomSword.MovingMode.NORMAL);
        ss.setDelay(200);
        ss.setDelayTicks(0);
        ss.setSeekDelay(15);
        ss.setScale(2f);
        ss.setExpRadius(OVER_CHARGE.bfgExpRadius());
        ss.setMultipleHit(thunderOn);
        ss.setChaining(thunderOn);
        ss.setFireSound(SoundEvents.WITHER_SHOOT, 1, 1.5f);
        ss.setHasTail(false);
        ss.setPos(player.position());
        ss.setCenterOffset(new Vec3(0, player.getEyeHeight(), 0));
        ss.setOffset(new Vec3(0, 0, 0.75f));
        ss.tryInit();
        player.level().addFreshEntity(ss);
        fdState.setSpecialCharge(0);
    }

    public static void dumpAmmo(LivingEntity entity, ISlashBladeState state,
            IFantasySlashBladeState fdState) {
        if (!(entity instanceof Player player))
            return;
        ItemStack blade = entity.getMainHandItem();
        boolean explosiveOn = CapabilityUtils.SEConditionMatcher.of(blade, player)
                .requireSE(FDSpecialEffectsRegistry.ExplosiveBullet)
                .match() != null;
        int ammo = fdState.getSpecialCharge();
        if (ammo <= 0)
            return;

        int volleyCount = explosiveOn ? ammo : ammo * 3;
        double ratio = CHARGE_SHOT_DAMAGE_RATIO;

        float baseDamage = state.getBaseAttackModifier() + state.getAttackAmplifier();
        int refine = state.getRefine();
        float refineBonus = (float) (refine * 0.1f + Math.sqrt(refine) * 1.5f);
        int enchantLevel = blade.getEnchantmentLevel(Enchantments.POWER_ARROWS);
        float enchantMultiplier = 1.0f + (enchantLevel * CHARGE_SHOT_ENCHANT_MULT_PER_LEVEL);
        float finalDamage = (float) ((baseDamage + refineBonus) * enchantMultiplier * ratio);

        int sweepLevel = blade.getEnchantmentLevel(Enchantments.SWEEPING_EDGE);
        float sweepRangeMult = explosiveOn ? EXPLOSIVE_BULLET.sweepRangeMult()
                : TRIPLE_BULLET.sweepRangeMult();
        float lockDistance = (explosiveOn ? EXPLOSIVE_BULLET.lockBase()
                : TRIPLE_BULLET.lockBase()) + sweepLevel * sweepRangeMult;

        java.util.List<LivingEntity> targets = FDTargetSelector
                .getTargetsInSight(
                        player, lockDistance, 30, true, null);
        targets.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(player)));

        int color = state.getColorCode();
        float expRadius = explosiveOn
                ? EXPLOSIVE_BULLET.expRadiusBase() + enchantLevel
                : NORMAL_BULLET_EXPLOSION_RADIUS;
        finalDamage *= explosiveOn ? EXPLOSIVE_BULLET.damageMult() : 1;

        float speed = explosiveOn ? CHARGE_SHOT_EXPLOSIVE_SPEED : CHARGE_SHOT_SPEED;
        int tailNodes = explosiveOn ? 48 : 8;

        double phi = Math.PI * (3.0 - Math.sqrt(5.0));
        double radius = 1.5;
        // 施放SA以球面分布，所以需要设置地形穿透
        for (int i = 0; i < volleyCount; i++) {
            EntityFDPhantomSword ss = explosiveOn
                    ? new tennouboshiuzume.mods.FantasyDesire.entity.EntityRefinedMissile(
                            FDEntitys.RefinedMissile.get(), player.level())
                    : new EntityFDPhantomSword(FDEntitys.FDPhantomSword.get(), player.level());
            ss.setIsCritical(false);
            ss.setOwner(player);
            ss.setColor(color);
            ss.setDamage(finalDamage);
            ss.setSpeed(speed);
            ss.setStandbyMode(EntityFDPhantomSword.StandbyMode.PLAYER);
            ss.setMovingMode(EntityFDPhantomSword.MovingMode.SEEK);
            ss.setDelay(100 + i);
            ss.setDelayTicks(0);
            ss.setSeekDelay(10);
            ss.setSeekAngle(18);
            ss.setNoClip(true);
            ss.setMultipleHit(true);
            ss.setExpRadius(expRadius);
            ss.setFireSound(SoundEvents.WITHER_SHOOT, 1, 1.5f);
            ss.setHasTail(true);
            ss.setScale(explosiveOn ? 0.2f : 0.5f);
            ss.setTailNodes(tailNodes);

            double y = 1 - (i / (double) (Math.max(volleyCount - 1, 1))) * 2;
            double radiusAtY = Math.sqrt(1 - y * y);
            double theta = phi * i;
            double x = Math.cos(theta) * radiusAtY;
            double z = Math.sin(theta) * radiusAtY;
            Vec3 dir = new Vec3(x, y, z).normalize();

            Vec3 spawnPos = player.position().add(0, player.getBbHeight() / 2.0, 0).add(dir.scale(radius));
            ss.setOffset(dir.scale(radius));
            ss.setCenterOffset(new Vec3(0, player.getBbHeight() / 2, 0));
            ss.setPos(spawnPos.x(), spawnPos.y(), spawnPos.z());

            float yaw = (float) Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90;
            float pitch = (float) Math.toDegrees(Math.asin(-dir.y));
            ss.setStandbyYawPitch(yaw, pitch);

            net.minecraft.world.entity.Entity locked = state.getTargetEntity(player.level());
            if (locked == null && !targets.isEmpty()) {
                if (!explosiveOn) {
                    locked = targets.get(i % targets.size());
                } else {
                    locked = targets.stream()
                            .filter(e -> e.getEffect(FDPotionEffects.MISSILE_LOCKED.get()) == null)
                            .findAny()
                            .orElseGet(() -> targets.stream()
                                    .min(java.util.Comparator.comparingInt(e -> {
                                        MobEffectInstance effect = e.getEffect(FDPotionEffects.MISSILE_LOCKED.get());
                                        return effect != null ? effect.getAmplifier() : Integer.MAX_VALUE;
                                    }))
                                    .orElse(targets.get(0)));
                }
            }

            if (locked instanceof LivingEntity living) {
                ss.setTargetId(locked.getId());
                if (explosiveOn) {
                    MobEffectInstance existing = living.getEffect(FDPotionEffects.MISSILE_LOCKED.get());
                    int amp = existing == null ? 0 : Math.min(existing.getAmplifier() + 1, 18);
                    living.forceAddEffect(new MobEffectInstance(
                            FDPotionEffects.MISSILE_LOCKED.get(),
                            60, amp, false, false, true), null);
                }
            }

            player.level().addFreshEntity(ss);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
        fdState.setSpecialCharge(0);
    }
}
