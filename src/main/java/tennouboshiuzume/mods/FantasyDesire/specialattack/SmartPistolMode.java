package tennouboshiuzume.mods.FantasyDesire.specialattack;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDBFG;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;

public class SmartPistolMode {
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
}
