package tennouboshiuzume.mods.FantasyDesire.specialeffect.effests.gunblade;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.client.particle.GlowingLineParticleOptions;
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
        boolean ExplosiveOn = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.ExplosiveBullet, player, "item.fantasydesire.smart_pistol")
                && !CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.ExplosiveBullet, player, "item.fantasydesire.smart_pistol");
        boolean ThunderOn = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.ExplosiveBullet, player, "item.fantasydesire.smart_pistol")
                && !CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.ExplosiveBullet, player, "item.fantasydesire.smart_pistol");
        if (TripleOn) cost = 1;
        if (EnergyOn) cost = 3;
        if (!(player.level() instanceof ServerLevel)) return;
        if (ammo < cost) {
            reload(player, blade, fdState);
            return;
        }
        smartShot(player, state);
//        energyShot(player,state);
//        BFGShot(player,state);
        event.setCanceled(true);
    }

    //    智能子弹
    private static void smartShot(Player player, ISlashBladeState state) {
        EntityFDPhantomSword ss = new EntityFDPhantomSword(FDEntitys.FDPhantomSword.get(), player.level());
        ss.setIsCritical(false);
        ss.setOwner(player);
        ss.setColor(state.getColorCode());
        ss.setRoll(0);
        ss.setDamage(1);
        ss.setSpeed(1);
        ss.setStandbyMode("PLAYER");
        ss.setMovingMode("SEEK");
        ss.setDelay(200);
        ss.setDelayTicks(0);
        ss.setSeekDelay(5);
        ss.setNoClip(true);
        ss.setMultipleHit(true);
        ss.setFireSound(SoundEvents.WITHER_SHOOT, 1, 1.5f);
        ss.setHasTail(true);
        ss.setScale(0.2f);
        if (state.getTargetEntity(player.level()) != null) {
            ss.setTargetId(state.getTargetEntityId());
        } else if (TargetUtils.getLockTarget(player).isPresent()) {
            ss.setTargetId(TargetUtils.getLockTarget(player).get().getId());
        }
        ss.setPos(player.position().add(0,player.getEyeHeight(),0));
        ss.setCenterOffset(new Vec3(0, 0, 0));
        ss.setOffset(new Vec3(0, 0, 0.75f));
        player.level().addFreshEntity(ss);
    }

    //    能量子弹
    private static void energyShot(Player player, ISlashBladeState state) {
        EntityFDEnergyBullet ss = new EntityFDEnergyBullet(FDEntitys.FDEnergyBullet.get(), player.level());
        ss.setIsCritical(false);
        ss.setOwner(player);
        ss.setColor(state.getColorCode());
        ss.setRoll(0);
        ss.setDamage(1);
        ss.setSpeed(1);
        ss.setStandbyMode("PLAYER");
        ss.setMovingMode("NORMAL");
        ss.setDelay(200);
        ss.setDelayTicks(1);
        ss.setSeekDelay(15);
//        ss.setNoClip(true);
        ss.setMultipleHit(true);
        ss.setFireSound(SoundEvents.WITHER_SHOOT, 1, 1.5f);
        ss.setHasTail(true);
        ss.setScale(1f);
        if (state.getTargetEntity(player.level()) != null) {
            ss.setTargetId(state.getTargetEntityId());
        } else if (TargetUtils.getLockTarget(player).isPresent()) {
            ss.setTargetId(TargetUtils.getLockTarget(player).get().getId());
        }
        ss.setPos(player.position());
        ss.setCenterOffset(new Vec3(0, player.getEyeHeight(), 0));
        ss.setOffset(new Vec3(0, 0, 0.75f));
        player.level().addFreshEntity(ss);
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
        if (state.getTargetEntity(player.level()) != null) {
            ss.setTargetId(state.getTargetEntityId());
        } else if (TargetUtils.getLockTarget(player).isPresent()) {
            ss.setTargetId(TargetUtils.getLockTarget(player).get().getId());
        }
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
