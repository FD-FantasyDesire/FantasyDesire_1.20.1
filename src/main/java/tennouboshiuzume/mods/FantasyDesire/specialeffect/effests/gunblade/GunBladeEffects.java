package tennouboshiuzume.mods.FantasyDesire.specialeffect.effests.gunblade;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
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
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffects;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;

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
//        if (CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.EnergyBullet, player, "item.fantasydesire.smart_pistol")) cost=6;
//        if (ammo<cost){
//            state.doChargeAction(player,0);
//            reload(player,blade,fdState);
//            return;
//        }
//        fdState.setSpecialCharge(ammo - cost);
//        System.out.println("fireing");
        for (int i=0;i<5;i++)
        {
            EntityFDPhantomSword ss = new EntityFDPhantomSword(FDEntitys.FDPhantomSword.get(),player.level());
            ss.setIsCritical(false);
            ss.setOwner(player);
            ss.setColor(state.getColorCode());
            ss.setRoll(0);
            ss.setDamage(1);
            ss.setSpeed(1);
            ss.setStandbyMode("WORLD");
            ss.setDelay(200);
            ss.setDelayTicks(20);
            RandomSource random = player.getRandom();
            ss.setScale(0.2f);
            // 设置待命角度：以玩家当前角度为基准展开
            ss.setStandbyYawPitch(-30 + i * 15f, 0);
            ss.setPos(player.position());
            ss.setOffset(new Vec3(0,player.getEyeHeight(),0.5f).yRot(-(float) Math.toRadians(-30f + i * 15f)));
            player.level().addFreshEntity(ss);
        }
        int color = state.getColorCode();

    }

    public static void reload(Player player,ItemStack blade, IFantasySlashBladeState fdState) {
        player.getCooldowns().addCooldown(blade.getItem(), 60);
        fdState.setSpecialCharge(fdState.getMaxSpecialCharge());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS, 0.5f, 2f);
        fdState.setSpecialCharge(fdState.getMaxSpecialCharge());
        return;
    }
}
