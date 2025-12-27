package tennouboshiuzume.mods.FantasyDesire.specialattack;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;

public class EchoingVoid {

    public static boolean AntiNTR(LivingEntity entity) {
        if (!(entity instanceof Player player))
            return false;
        ItemStack blade = entity.getMainHandItem();
        if (!(blade.getItem() instanceof ItemSlashBlade))
            return false;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        return state.getTranslationKey().equals("item.fantasydesire.starless_night");
    }

    // AddonSlashUtils.doAddonFDSlash(player, event.getRoll(), player.getYRot(), 0,
    // state.getColorCode(), 0, Vec3.ZERO,
    // false,
    // false, event.getDamage(), KnockBacks.cancel, 10f);
}
