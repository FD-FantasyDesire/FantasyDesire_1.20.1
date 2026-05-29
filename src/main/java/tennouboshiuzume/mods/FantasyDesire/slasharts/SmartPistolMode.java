package tennouboshiuzume.mods.FantasyDesire.slasharts;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDBFG;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;
import tennouboshiuzume.mods.FantasyDesire.init.FDSlashArtRegistry;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;

public class SmartPistolMode {
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

    private static void BFGShot(Player player, ISlashBladeState state) {
        EntityFDBFG ss = new EntityFDBFG(FDEntitys.FDBFG.get(), player.level());
        ss.setIsCritical(false);
        ss.setOwner(player);
        ss.setColor(state.getColorCode());
        ss.setRoll(0);
        ss.setDamage(1);
        ss.setSpeed(1);
        ss.setStandbyMode(EntityFDPhantomSword.StandbyMode.PLAYER);
        ss.setMovingMode(EntityFDPhantomSword.MovingMode.NORMAL);
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
