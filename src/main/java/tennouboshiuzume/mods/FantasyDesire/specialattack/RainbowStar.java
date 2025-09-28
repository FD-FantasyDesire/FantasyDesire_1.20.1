package tennouboshiuzume.mods.FantasyDesire.specialattack;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDRainbowPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ColorUtils;

import java.util.Random;

public class RainbowStar {
    public static void RainbowStar(LivingEntity player, ItemStack blade){
        if (blade.getItem() instanceof ItemSlashBlade){
            ISlashBladeState state = CapabilityUtils.getBladeState(blade);
            if (state.getTranslationKey().equals("item.fantasydesire.pure_snow")){
                if (player.level().isClientSide()) return;
                if (!(player instanceof Player)) return;
                float baseModif = state.getDamage();
                float magicDamage = 1.0f + (baseModif / 2.0f);
                Vec3 pos = player.position().add(new Vec3(0,40,0));
                Random random = new Random();
                for (int i = 0; i < 21; i++) {
                    EntityFDRainbowPhantomSword ss = new EntityFDRainbowPhantomSword(FDEntitys.FDRainbowPhantomSword.get(),player.level());
                    ss.setDelay(200);
                    ss.setPos(pos.add(new Vec3(random.nextGaussian()*3,random.nextGaussian()*3,random.nextGaussian()*3)));
                    ss.setDelayTicks(5+i);
                    ss.setStandbyMode("WORLD");
                    ss.setStandbyYawPitch((float) random.nextInt(360), 90 +(float) random.nextGaussian()*3);
                    ss.setColor(ColorUtils.getSmoothTransitionColor(i,21,true));
                    ss.setDamage(magicDamage);
                    ss.setRoll(random.nextInt(360));
                    ss.setScale(2f);
                    ss.setSpeed(5f);
                    ss.setOwner(player);
                    player.level().addFreshEntity(ss);
                }
            }
        }
    }
}
