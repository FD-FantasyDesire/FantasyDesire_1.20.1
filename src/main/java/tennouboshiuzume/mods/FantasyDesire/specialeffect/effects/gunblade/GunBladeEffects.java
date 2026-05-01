package tennouboshiuzume.mods.FantasyDesire.specialeffect.effects.gunblade;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDEnergyBullet;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityRefinedMissile;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialAttacks;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffects;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.FDTargetSelector;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GunBladeEffects {
    // 填弹上膛
    // 非玩家无法使用，Human Use Tools
    @SubscribeEvent
    public static void onSlash(SlashBladeEvent.DoSlashEvent event) {
        ItemStack blade = event.getBlade();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade))
            return;
        if (!(event.getUser() instanceof Player player))
            return;
        // shift不触发SE
        if (player.isShiftKeyDown())
            return;

        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(blade, player)
                .requireTranslation("item.fantasydesire.smart_pistol")
                .match();

        if (ctx == null)
            return;

        ISlashBladeState state = ctx.state;
        IFantasySlashBladeState fdState = ctx.fantasyState;

        int ammo = fdState.getSpecialCharge();
        boolean TripleOn = CapabilityUtils.SEConditionMatcher.of(blade, player)
                .requireSE(FDSpecialEffects.TripleBullet)
                .requireSA(FDSpecialAttacks.CHARGE_SHOT.get())
                .match() != null;
        boolean EnergyOn = CapabilityUtils.SEConditionMatcher.of(blade, player)
                .requireSE(FDSpecialEffects.EnergyBullet)
                .requireSA(FDSpecialAttacks.OVER_CHARGE.get())
                .match() != null;
        boolean ExplosiveOn = CapabilityUtils.SEConditionMatcher.of(blade, player)
                .requireSE(FDSpecialEffects.ExplosiveBullet)
                .match() != null;
        boolean ThunderOn = CapabilityUtils.SEConditionMatcher.of(blade, player)
                .requireSE(FDSpecialEffects.ThunderBullet)
                .match() != null;

        int cost = TripleOn && !EnergyOn ? 1 : 2;
        int soulcost = 36;
        // 简单装填检测
        if (ammo < cost) {
            // 符合装填条件时，取消斩击进入装填冷却
            // 否则使用常规拔刀近战攻击
            if (reload(player, blade, state, fdState, soulcost))
                event.setCanceled(true);
            return;
        }
        fdState.setSpecialCharge(fdState.getSpecialCharge() - cost);
        Random random = new Random();
        if (!(player.level() instanceof ServerLevel))
            return;
        if (TripleOn && !EnergyOn) {
            shootSmartBullets(player, blade, state, fdState,
                    ExplosiveOn, random);
        }
        if (EnergyOn && !TripleOn) {
            shootEnergyBullet(player, blade, state,
                    ThunderOn, random);
        }
        event.setCanceled(true);
    }

    private static void shootSmartBullets(Player player,
                                          ItemStack blade,
                                          ISlashBladeState state,
                                          IFantasySlashBladeState fdState,
                                          boolean explosive,
                                          Random random) {

        if (!(player.level() instanceof ServerLevel)) return;
        int volleyCount = explosive ? 1 : 3;
        int inaccuracy = explosive ? 3 : 15;
        int tailNodes = explosive ? 48 : 8;
        float seekAngle = explosive ? 6 : 18;
        float sweepRangeMult = explosive ? 15 : 5;
        float speed = explosive ? 0.33f : 1f;
        int delay = explosive ? 300 : 100;

        int sweepLevel = blade.getEnchantmentLevel(Enchantments.SWEEPING_EDGE);
        float lockDistance = (explosive ? 35 : 15) + sweepLevel * sweepRangeMult;

        float damage = state.getBaseAttackModifier()
                + state.getAttackAmplifier()
                + blade.getEnchantmentLevel(Enchantments.POWER_ARROWS) * 3;

        List<LivingEntity> targets = FDTargetSelector.getTargetsInSight(
                player, lockDistance, 30, true, null);

        targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));

        for (int i = 0; i < volleyCount; i++) {

            EntityFDPhantomSword ss = explosive
                    ? new EntityRefinedMissile(FDEntitys.RefinedMissile.get(), player.level())
                    : new EntityFDPhantomSword(FDEntitys.FDPhantomSword.get(), player.level());

            setupSmartBullet(ss, player, state, random,
                    explosive, damage, speed, delay, i,
                    inaccuracy, seekAngle, tailNodes);

            Entity target = selectTarget(player, state, targets, explosive, i);

            if (target instanceof LivingEntity living) {
                ss.setTargetId(target.getId());
                if (explosive) {
                    applyMissileLock(living);
                }
            }

            spawnProjectile(player, ss);
        }
    }
    private static void shootEnergyBullet(Player player,
                                          ItemStack blade,
                                          ISlashBladeState state,
                                          boolean thunder,
                                          Random random) {

        if (!(player.level() instanceof ServerLevel)) return;

        float damage = state.getBaseAttackModifier()
                + state.getAttackAmplifier()
                + blade.getEnchantmentLevel(Enchantments.POWER_ARROWS) * 5;

        EntityFDEnergyBullet bullet =
                new EntityFDEnergyBullet(FDEntitys.FDEnergyBullet.get(), player.level());

        bullet.setIsCritical(false);
        bullet.setOwner(player);
        bullet.setColor(thunder ? 0xFFFF00 : state.getColorCode());
        bullet.setRoll(random.nextInt(180));
        bullet.setDamage(damage);
        bullet.setNoClip(true);
        bullet.setSpeed(5f);

        bullet.setStandbyMode("PLAYER");
        bullet.setMovingMode("NORMAL");

        bullet.setDelay(60);
        bullet.setDelayTicks(1);

        bullet.setMultipleHit(true);
        bullet.setExpRadius(thunder ? 5 : 0);

        bullet.setFireSound(SoundEvents.SHULKER_SHOOT, 1, 2f);
        bullet.setHasTail(true);
        bullet.setScale(0.5f);

        spawnProjectile(player, bullet);
    }

    private static void spawnProjectile(Player player, Entity ss) {
        ss.setPos(player.position());
        if (ss instanceof EntityFDPhantomSword sword) {
            sword.setCenterOffset(new Vec3(0, player.getEyeHeight(), 0));
            sword.setOffset(new Vec3(0, 0, 0.75f));
            sword.tryInit();
        }
        player.level().addFreshEntity(ss);
    }
    private static void setupSmartBullet(EntityFDPhantomSword ss,
                                         Player player,
                                         ISlashBladeState state,
                                         Random random,
                                         boolean explosive,
                                         float damage,
                                         float speed,
                                         int delay,
                                         int index,
                                         int inaccuracy,
                                         float seekAngle,
                                         int tailNodes) {

        ss.setIsCritical(false);
        ss.setOwner(player);
        ss.setColor(explosive ? 0xFF0000 : state.getColorCode());
        ss.setRoll(random.nextInt(180));
        ss.setDamage(damage);
        ss.setSpeed(speed);
        ss.setStandbyMode("PLAYER");
        ss.setMovingMode("SEEK");
        ss.setDelay(delay + index);
        ss.setDelayTicks(index);
        ss.setSeekDelay(2 + index);
        ss.setSeekAngle(seekAngle);
        ss.setNoClip(!explosive);
        ss.setMultipleHit(true);
        ss.setExpRadius(explosive ? 2 : 0);
        ss.setStandbyYawPitch(
                (float) random.nextGaussian() * inaccuracy,
                (float) random.nextGaussian() * inaccuracy);
        ss.setFireSound(SoundEvents.WITHER_SHOOT, 1, 1.5f);
        ss.setHasTail(true);
        ss.setScale(0.2f);
        ss.setTailNodes(tailNodes);
    }
    private static Entity selectTarget(Player player,
                                       ISlashBladeState state,
                                       List<LivingEntity> targets,
                                       boolean explosive,
                                       int index) {

        Entity locked = state.getTargetEntity(player.level());
        if (locked != null) return locked;

        if (targets.isEmpty()) return null;

        if (!explosive) {
            return targets.get(index % targets.size());
        }

        return targets.stream()
                .filter(e -> e.getEffect(FDPotionEffects.MISSILE_LOCKED.get()) == null)
                .findAny()
                .orElseGet(() ->
                        targets.stream()
                                .min(Comparator.comparingInt(e -> {
                                    MobEffectInstance effect = e.getEffect(FDPotionEffects.MISSILE_LOCKED.get());
                                    return effect != null ? effect.getAmplifier() : Integer.MAX_VALUE;
                                }))
                                .orElse(targets.get(0))
                );
    }
    private static void applyMissileLock(LivingEntity target) {
        MobEffectInstance existing = target.getEffect(FDPotionEffects.MISSILE_LOCKED.get());

        int amp = existing == null ? 0 : Math.min(existing.getAmplifier() + 1, 18);

        target.addEffect(new MobEffectInstance(
                FDPotionEffects.MISSILE_LOCKED.get(),
                60,
                amp,
                false, false, true
        ));
    }
    // 填弹上膛
    public static boolean reload(Player player, ItemStack blade, ISlashBladeState state,
            IFantasySlashBladeState fdState, int soulcost) {
        if (state.getProudSoulCount() < soulcost) {
            return false;
        }
        state.setProudSoulCount(state.getProudSoulCount() - soulcost);
        player.getCooldowns().addCooldown(blade.getItem(), 60);
        fdState.setSpecialCharge(fdState.getMaxSpecialCharge());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PISTON_CONTRACT,
                SoundSource.PLAYERS, 0.5f, 2f);
        return true;
    }
}
