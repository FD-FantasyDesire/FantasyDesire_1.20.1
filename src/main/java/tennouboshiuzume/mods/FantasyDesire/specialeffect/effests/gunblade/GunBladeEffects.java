package tennouboshiuzume.mods.FantasyDesire.specialeffect.effests.gunblade;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDBFG;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDEnergyBullet;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
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
    //    填弹上膛
    @SubscribeEvent
    public static void onSlash(SlashBladeEvent.DoSlashEvent event) {
        ItemStack blade = event.getBlade();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade))
            return;
        if (!(event.getUser() instanceof Player player)) return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        if (!state.getTranslationKey().equals("item.fantasydesire.smart_pistol")) return;
        int ammo = fdState.getSpecialCharge();
        int cost = 0;
        boolean TripleOn = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.TripleBullet, player, "item.fantasydesire.smart_pistol")
                && state.getSlashArts().equals(FDSpecialAttacks.CHARGE_SHOT.get()) && state.getSlashArts() == FDSpecialAttacks.CHARGE_SHOT.get();
        boolean EnergyOn = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.EnergyBullet, player, "item.fantasydesire.smart_pistol")
                && state.getSlashArts().equals(FDSpecialAttacks.OVER_CHARGE.get()) && state.getSlashArts() == FDSpecialAttacks.OVER_CHARGE.get();
        boolean ExplosiveOn = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.ExplosiveBullet, player, "item.fantasydesire.smart_pistol");
        boolean ThunderOn = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.ExplosiveBullet, player, "item.fantasydesire.smart_pistol");

        cost = TripleOn && !EnergyOn ? 1 : 3;

        if (ammo < cost) {
            reload(player, blade, fdState);
            return;
        }
        fdState.setSpecialCharge(fdState.getSpecialCharge() - cost);
        Random random = new Random();
        if (!(player.level() instanceof ServerLevel)) return;

        if (TripleOn && !EnergyOn) {
//            智能弹
            int volleyCount = ExplosiveOn ? 1 : 3;
            int inaccuracy = ExplosiveOn ? 3 : 15;
            int sweepLevel = blade.getEnchantmentLevel(Enchantments.SWEEPING_EDGE);
            float lockDistance = 15 + sweepLevel * 5;
            float damage = state.getBaseAttackModifier() + state.getAttackAmplifier() + blade.getEnchantmentLevel(Enchantments.POWER_ARROWS) * 3;
            List<LivingEntity> targets = TargetUtils.getTargetsInSight(player, lockDistance, 30, true, null);
            targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
            for (int i = 0; i < volleyCount; i++) {
                EntityFDPhantomSword ss = new EntityFDPhantomSword(FDEntitys.FDPhantomSword.get(), player.level());
                ss.setIsCritical(false);
                ss.setOwner(player);
                ss.setColor(ExplosiveOn ? 0xFF0000 : state.getColorCode());
                ss.setRoll(random.nextInt(180));
                ss.setDamage(damage);
                ss.setSpeed(1f);
                ss.setStandbyMode("PLAYER");
                ss.setMovingMode("SEEK");
                ss.setDelay(200 + i);
                ss.setDelayTicks(i);
                ss.setSeekDelay(2 + i);
                ss.setNoClip(!ExplosiveOn);
                ss.setMultipleHit(true);
                ss.setExpRadius(ExplosiveOn ? 5 : 0);
                ss.setStandbyYawPitch((float) random.nextGaussian() * inaccuracy, (float) random.nextGaussian() * inaccuracy);
                ss.setFireSound(SoundEvents.WITHER_SHOOT, 1, 1.5f);
                ss.setHasTail(true);
                ss.setScale(0.2f);
                if (state.getTargetEntity(player.level()) != null) {
                    ss.setTargetId(state.getTargetEntityId());
                } else if (!targets.isEmpty()) {
                    ss.setTargetId(targets.get(i % targets.size()).getId());
                }
                ss.setPos(player.position());
                ss.setCenterOffset(new Vec3(0, player.getEyeHeight(), 0));
                ss.setOffset(new Vec3(0, 0, 0.75f));
                player.level().addFreshEntity(ss);
            }
        }

        if (EnergyOn && !TripleOn) {
//            能量弹
            float damage = state.getBaseAttackModifier() + state.getAttackAmplifier() + blade.getEnchantmentLevel(Enchantments.POWER_ARROWS) * 5;
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
            ss.setPos(player.position());
            ss.setCenterOffset(new Vec3(0, player.getEyeHeight(), 0));
            ss.setOffset(new Vec3(0, 0, 0.75f));
            player.level().addFreshEntity(ss);
        }
        event.setCanceled(true);
    }

    private static void BFGShot(Player player, ISlashBladeState state) {
        EntityFDBFG ss = new EntityFDBFG(FDEntitys.FDBFG.get(), player.level());
        ss.setIsCritical(false);
        ss.setOwner(player);
        ss.setColor(state.getColorCode());
        ss.setRoll(0);
        ss.setDamage(1);
        ss.setSpeed(1);
        ss.setStandbyMode("PLAYER");
        ss.setMovingMode("NORMAL");
        ss.setDelay(200);
        ss.setDelayTicks(0);
        ss.setSeekDelay(15);
        ss.setScale(2f);
        ss.setExpRadius(4f);
        ss.setMultipleHit(true);
        ss.setFireSound(SoundEvents.WITHER_SHOOT, 1, 1.5f);
        ss.setHasTail(true);
        ss.setPos(player.position());
        ss.setCenterOffset(new Vec3(0, player.getEyeHeight(), 0));
        ss.setOffset(new Vec3(0, 0, 0.75f));
        player.level().addFreshEntity(ss);
    }

    public static void reload(Player player, ItemStack blade, IFantasySlashBladeState fdState) {
        player.getCooldowns().addCooldown(blade.getItem(), 60);
        fdState.setSpecialCharge(fdState.getMaxSpecialCharge());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS, 0.5f, 2f);
    }
}
