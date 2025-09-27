package tennouboshiuzume.mods.FantasyDesire.specialeffect.effests.gunblade;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDDriveEx;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
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
        if (CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.TripleBullet, player, "item.fantasydesire.smart_pistol")) cost=1;
        if (!(player.level()instanceof ServerLevel)) return;
        for (int i=0;i<5;i++){}
        event.setCanceled(true);
//        {
//            EntityFDPhantomSword ss = new EntityFDPhantomSword(FDEntitys.FDPhantomSword.get(),player.level());
//            ss.setIsCritical(false);
//            ss.setOwner(player);
//            ss.setColor(state.getColorCode());
//            ss.setRoll(0);
//            ss.setDamage(1);
//            ss.setSpeed(1);
//            ss.setStandbyMode("PLAYER");
//            ss.setMovingMode("SEEK");
//            ss.setDelay(200);
//            ss.setDelayTicks(20+2*i);
//            ss.setSeekDelay(25+2*i);
//            ss.setNoClip(true);
//            ss.setMultipleHit(true);
//            ss.setFireSound(SoundEvents.WITHER_SHOOT,1,1.5f);
//            ss.setParticleType(ParticleTypes.END_ROD);
//            RandomSource random = player.getRandom();
//            ss.setScale(0.2f);
//            if (state.getTargetEntity(player.level())!=null){
//                ss.setTargetId(state.getTargetEntityId());
//            }else if (TargetUtils.getLockTarget(player).isPresent()){
//                ss.setTargetId(TargetUtils.getLockTarget(player).get().getId());
//            }
//            // 设置待命角度：以玩家当前角度为基准展开
//            ss.setStandbyYawPitch(-30 + i * 15f, 0);
//            ss.setPos(player.position());
//            ss.setCenterOffset(new Vec3(0,player.getEyeHeight(),0));
//            ss.setOffset(new Vec3(0,0,0.75f).yRot(-(float) Math.toRadians(-30f + i * 15f)));
//            player.level().addFreshEntity(ss);
//        }
//        {
//            EntityFDDriveEx ss = new EntityFDDriveEx(FDEntitys.FDDriveEx.get(),player.level());
//            ss.setIsCritical(false);
//            ss.setOwner(player);
//            ss.setColor(state.getColorCode());
//            ss.setRoll(0);
//            ss.setDamage(1);
//            ss.setSpeed(1);
//            ss.setStandbyMode("PLAYER");
//            ss.setDelay(200);
//            ss.setDelayTicks(20+2*i);
//            ss.setNoClip(true);
//            ss.setFireSound(SoundEvents.WITHER_SHOOT,1,1.5f);
//            ss.setParticleType(ParticleTypes.END_ROD);
//            ss.setScale(1f);
//            if (state.getTargetEntity(player.level())!=null){
//                ss.setTargetId(state.getTargetEntityId());
//            }else if (TargetUtils.getLockTarget(player).isPresent()){
//                ss.setTargetId(TargetUtils.getLockTarget(player).get().getId());
//            }
//            // 设置待命角度：以玩家当前角度为基准展开
//            ss.setStandbyYawPitch(-30 + i * 15f, 0);
//            ss.setPos(player.position());
//            ss.setCenterOffset(new Vec3(0,player.getEyeHeight(),0));
//            ss.setOffset(new Vec3(0,0,0.75f).yRot(-(float) Math.toRadians(-30f + i * 15f)));
//            player.level().addFreshEntity(ss);
//        }

    }

    public static void reload(Player player,ItemStack blade, IFantasySlashBladeState fdState) {
        player.getCooldowns().addCooldown(blade.getItem(), 60);
        fdState.setSpecialCharge(fdState.getMaxSpecialCharge());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS, 0.5f, 2f);
        fdState.setSpecialCharge(fdState.getMaxSpecialCharge());
        return;
    }
}
