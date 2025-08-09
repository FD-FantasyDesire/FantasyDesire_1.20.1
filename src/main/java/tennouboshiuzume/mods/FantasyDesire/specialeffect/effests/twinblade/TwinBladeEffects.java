package tennouboshiuzume.mods.FantasyDesire.specialeffect.effests.twinblade;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffects;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.AddonSlashUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;

@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TwinBladeEffects {
    //    双持共击
    @SubscribeEvent
    public static void onTwinSlash(SlashBladeEvent.DoSlashEvent event) {
        ItemStack blade = event.getBlade();
        ItemStack offblade = event.getUser().getOffhandItem();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade) || !(offblade.getItem() instanceof ItemFantasySlashBlade))
            return;
        if (!(event.getUser() instanceof Player player)) return;
//        获取主手Capability
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
//        获取副手Capability
        ISlashBladeState offstate = CapabilityUtils.getBladeState(offblade);
        IFantasySlashBladeState offfdState = CapabilityUtils.getFantasyBladeState(offblade);
//        检查主手效果激活
        boolean mainActive = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.TwinSet, player, "item.fantasydesire.twin_blade");
//        检查副手效果激活
        boolean offActive = CapabilityUtils.isSpecialEffectActiveForItem(offstate, FDSpecialEffects.TwinSet, player, "item.fantasydesire.twin_blade");
//        检查两把刀是否为不同形态
        if (fdState.getSpecialType().equals(offfdState.getSpecialType())) return;
//        int color = state.getColorCode();
        int offcolor = offstate.getColorCode();
        if (mainActive && offActive) {
//            切斩
            AddonSlashUtils.doAddonSlash(player, event.getRoll() - 180, player.getYRot(), 0, offcolor, Vec3.ZERO, false, false, 0.1f, KnockBacks.cancel);
        }
    }
}

