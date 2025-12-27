package tennouboshiuzume.mods.FantasyDesire.specialeffect.effests.gunblade;

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
import tennouboshiuzume.mods.FantasyDesire.utils.TargetUtils;

import java.util.Comparator;
import java.util.List;
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
        if (player.isShiftKeyDown())
            return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        if (!state.getTranslationKey().equals("item.fantasydesire.smart_pistol"))
            return;
        int ammo = fdState.getSpecialCharge();
        boolean TripleOn = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.TripleBullet, player,
                "item.fantasydesire.smart_pistol")
                && state.getSlashArts().equals(FDSpecialAttacks.CHARGE_SHOT.get())
                && state.getSlashArts() == FDSpecialAttacks.CHARGE_SHOT.get();
        boolean EnergyOn = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.EnergyBullet, player,
                "item.fantasydesire.smart_pistol")
                && state.getSlashArts().equals(FDSpecialAttacks.OVER_CHARGE.get())
                && state.getSlashArts() == FDSpecialAttacks.OVER_CHARGE.get();
        boolean ExplosiveOn = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.ExplosiveBullet,
                player, "item.fantasydesire.smart_pistol");
        boolean ThunderOn = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.ThunderBullet, player,
                "item.fantasydesire.smart_pistol");

        int cost = TripleOn && !EnergyOn ? 1 : 3;
        int soulcost = 36;
        if (ammo < cost) {
            reload(player, blade, state, fdState, soulcost);
            return;
        }
        fdState.setSpecialCharge(fdState.getSpecialCharge() - cost);
        Random random = new Random();

        if (!(player.level() instanceof ServerLevel))
            return;

        if (TripleOn && !EnergyOn) {
            // 智能弹
            int volleyCount = ExplosiveOn ? 1 : 3;
            int inaccuracy = ExplosiveOn ? 3 : 15;
            int tailNodes = ExplosiveOn ? 48 : 8;
            float seekAngle = ExplosiveOn ? 6 : 18;
            float sweepRangeMult = ExplosiveOn ? 15 : 5;
            float speed = ExplosiveOn ? 0.33f : 1f;
            int delay = ExplosiveOn ? 300 : 100;
            int sweepLevel = blade.getEnchantmentLevel(Enchantments.SWEEPING_EDGE);
            // 横扫之刃附魔增加锁定距离
            float lockDistance = 15 + sweepLevel * sweepRangeMult;
            // 力量附魔增加伤害
            float damage = state.getBaseAttackModifier() + state.getAttackAmplifier()
                    + blade.getEnchantmentLevel(Enchantments.POWER_ARROWS) * 3;
            List<LivingEntity> targets = TargetUtils.getTargetsInSight(player, lockDistance, 30, true, null);
            targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
            for (int i = 0; i < volleyCount; i++) {
                EntityFDPhantomSword ss;
                if (ExplosiveOn) {
                    ss = new EntityRefinedMissile(FDEntitys.RefinedMissile.get(), player.level());
                } else {
                    ss = new EntityFDPhantomSword(FDEntitys.FDPhantomSword.get(), player.level());
                }
                ss.setIsCritical(false);
                ss.setOwner(player);
                ss.setColor(ExplosiveOn ? 0xFF0000 : state.getColorCode());
                ss.setRoll(random.nextInt(180));
                ss.setDamage(damage);
                ss.setSpeed(speed);
                ss.setStandbyMode("PLAYER");
                ss.setMovingMode("SEEK");
                ss.setDelay(delay + i);
                ss.setDelayTicks(i);
                ss.setSeekDelay(2 + i);
                ss.setSeekAngle(seekAngle);
                ss.setNoClip(!ExplosiveOn);
                ss.setMultipleHit(true);
                ss.setExpRadius(ExplosiveOn ? 2 : 0);
                ss.setStandbyYawPitch((float) random.nextGaussian() * inaccuracy,
                        (float) random.nextGaussian() * inaccuracy);
                ss.setFireSound(SoundEvents.WITHER_SHOOT, 1, 1.5f);
                ss.setHasTail(true);
                ss.setScale(0.2f);
                ss.setTailNodes(tailNodes);
                // 如果“绝肃爆裂弹头”效果激活，首先尝试从候选目标列表中找到没有 MISSILE_LOCKED 效果的实体。
                // 如果所有候选目标都已有该效果，则选择 目标锁定 剩余时间最短的那个实体。
                // 选中目标后，为其添加持续 60 ticks (3秒) 的 目标锁定 效果。
                // 如果已有选定的目标（例如拔刀剑本身锁定的目标），则优先使用该目标。
                Entity finalTarget = null;
                if (state.getTargetEntity(player.level()) != null) {
                    finalTarget = state.getTargetEntity(player.level());
                } else if (!targets.isEmpty()) {
                    LivingEntity chosenLiving = null;
                    if (ExplosiveOn) {
                        for (LivingEntity candidate : targets) {
                            if (!candidate.hasEffect(FDPotionEffects.MISSILE_LOCKED.get())) {
                                chosenLiving = candidate;
                                break;
                            }
                        }
                        if (chosenLiving == null) {
                            chosenLiving = targets.stream()
                                    .min(Comparator.comparingInt(e -> {
                                        MobEffectInstance effect = e.getEffect(FDPotionEffects.MISSILE_LOCKED.get());
                                        return effect != null ? effect.getDuration() : 0;
                                    })).orElse(targets.get(0));
                        }
                    } else {
                        chosenLiving = targets.get(i % targets.size());
                    }
                    finalTarget = chosenLiving;
                }

                if (finalTarget != null) {
                    ss.setTargetId(finalTarget.getId());
                    if (ExplosiveOn && finalTarget instanceof LivingEntity livingTarget) {
                        livingTarget.addEffect(new MobEffectInstance(FDPotionEffects.MISSILE_LOCKED.get(), 60, 0));
                    }
                }
                ss.setPos(player.position().add(new Vec3(0, player.getBbHeight() / 2, 0)));
                ss.setCenterOffset(new Vec3(0, player.getEyeHeight(), 0));
                ss.setOffset(new Vec3(0, 0, 0.75f));
                player.level().addFreshEntity(ss);
            }
        }

        if (EnergyOn && !TripleOn) {
            // 能量弹
            float damage = state.getBaseAttackModifier() + state.getAttackAmplifier()
                    + blade.getEnchantmentLevel(Enchantments.POWER_ARROWS) * 5;
            EntityFDEnergyBullet ss = new EntityFDEnergyBullet(FDEntitys.FDEnergyBullet.get(), player.level());
            ss.setIsCritical(false);
            ss.setOwner(player);
            ss.setColor(ThunderOn ? 0xFFFF00 : state.getColorCode());
            ss.setRoll(random.nextInt(180));
            ss.setDamage(damage);
            ss.setSpeed(5f);
            ss.setStandbyMode("PLAYER");
            ss.setMovingMode("NORMAL");
            ss.setDelay(60);
            ss.setDelayTicks(1);
            ss.setMultipleHit(true);
            ss.setExpRadius(ThunderOn ? 5 : 0);
            ss.setFireSound(SoundEvents.SHULKER_SHOOT, 1, 2f);
            ss.setHasTail(true);
            ss.setScale(0.5f);
            ss.setPos(player.position().add(new Vec3(0, player.getBbHeight() / 2, 0)));
            ss.setCenterOffset(new Vec3(0, player.getEyeHeight(), 0));
            ss.setOffset(new Vec3(0, 0, 0.75f));
            player.level().addFreshEntity(ss);
        }
        event.setCanceled(true);
    }

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
