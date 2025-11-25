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
        Boolean TripleOn = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.TripleBullet, player, "item.fantasydesire.smart_pistol")&&state.getSlashArts().equals(FDSpecialAttacks.CHARGE_SHOT.get());
        Boolean EnergyOn = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.EnergyBullet, player, "item.fantasydesire.smart_pistol")&&state.getSlashArts().equals(FDSpecialAttacks.OVER_CHARGE.get());
        if (TripleOn) cost = 1;
        if (EnergyOn) cost = 3;
        if (!(player.level()instanceof ServerLevel)) return;
        if (ammo<cost) reload(player,blade,fdState);

        Vec3 start = player.getEyePosition();

// 玩家视线方向（单位向量）
        Vec3 look = player.getLookAngle();

// 终点 = 起点 + 视线方向 * 15
        Vec3 end = start.add(look.scale(15.0));
//        if (player.level() instanceof ServerLevel serverLevel) {
//            Vec3 finalLocation = start.add(end.subtract(start).scale(0.5)).yRot((float) Math.toRadians(-30f));
//            GlowingLineParticleOptions opts = new GlowingLineParticleOptions(start, end, state.getColorCode(), 0.1f, 0.75f,false,5);
//            serverLevel.sendParticles(opts, finalLocation.x, finalLocation.y, finalLocation.z, 1, 0, 0, 0, 0);
////            ParticleUtils.LightBoltParticles(serverLevel,start,end,opts.color,opts.thickness,5,opts.alpha,false,1,16);
//        }
        {
            EntityFDPhantomSword ss = new EntityFDPhantomSword(FDEntitys.FDPhantomSword.get(),player.level());
            ss.setIsCritical(false);
            ss.setOwner(player);
            ss.setColor(state.getColorCode());
            ss.setRoll(0);
            ss.setDamage(1);
            ss.setSpeed(1);
            ss.setStandbyMode("PLAYER");
            ss.setMovingMode("SEEK");
            ss.setDelay(200);
            ss.setDelayTicks(20);
            ss.setSeekDelay(25);
            ss.setNoClip(true);
            ss.setMultipleHit(true);
            ss.setFireSound(SoundEvents.WITHER_SHOOT,1,1.5f);
            ss.setHasTail(true);
            ss.setScale(0.2f);
            if (state.getTargetEntity(player.level())!=null){
                ss.setTargetId(state.getTargetEntityId());
            }else if (TargetUtils.getLockTarget(player).isPresent()){
                ss.setTargetId(TargetUtils.getLockTarget(player).get().getId());
            }
            ss.setPos(player.position());
            ss.setCenterOffset(new Vec3(0,player.getEyeHeight(),0));
            ss.setOffset(new Vec3(0,0,0.75f));
            player.level().addFreshEntity(ss);
        }
        event.setCanceled(true);
    }

    public static void reload(Player player,ItemStack blade, IFantasySlashBladeState fdState) {
        player.getCooldowns().addCooldown(blade.getItem(), 60);
        fdState.setSpecialCharge(fdState.getMaxSpecialCharge());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS, 0.5f, 2f);
        return;
    }
}
