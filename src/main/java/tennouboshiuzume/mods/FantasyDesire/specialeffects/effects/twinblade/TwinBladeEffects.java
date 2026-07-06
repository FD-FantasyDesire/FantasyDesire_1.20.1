package tennouboshiuzume.mods.FantasyDesire.specialeffects.effects.twinblade;

import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffectsRegistry;
import tennouboshiuzume.mods.FantasyDesire.utils.AddonSlashUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;

@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TwinBladeEffects {
    // 我觉得双刀的设计还是相对非常朴素，如果未来有想法再补充吧，目前将这两把刀定位在前中期也许更合适
    // 双持共击
    @SubscribeEvent
    public static void onTwinSlash(SlashBladeEvent.DoSlashEvent event) {
        if (!(event.getUser() instanceof Player player))
            return;
        CapabilityUtils.BladeContext mainCtx = CapabilityUtils.SEConditionMatcher.of(player)
                .requireTranslation("item.fantasydesire.twin_blade")
                .requireSE(FDSpecialEffectsRegistry.TwinSet)
                .match();
        CapabilityUtils.BladeContext offCtx = CapabilityUtils.SEConditionMatcher.of(player)
                .onlyOffhand()
                .requireTranslation("item.fantasydesire.twin_blade")
                .requireSE(FDSpecialEffectsRegistry.TwinSet)
                .match();
        if (mainCtx == null || offCtx == null)
            return;
        if (mainCtx.fantasyState.getSpecialType().equals(offCtx.fantasyState.getSpecialType()))
            return;
        int offColor = offCtx.state.getColorCode();
        double damage = event.getDamage();
        AddonSlashUtils.doAddonSlash(player, event.getRoll() - 180, player.getYRot(), 0, offColor, 0, Vec3.ZERO, false,
                false, damage, KnockBacks.cancel);
    }
}
