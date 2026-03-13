package tennouboshiuzume.mods.FantasyDesire.specialeffect.effects.twinblade;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialAttacks;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffects;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.AddonSlashUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;

@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TwinBladeEffects {
    // 双持共击
    @SubscribeEvent
    public static void onTwinSlash(SlashBladeEvent.DoSlashEvent event) {
        if (!(event.getUser() instanceof Player player))
            return;

        CapabilityUtils.BladeContext mainCtx = CapabilityUtils.SEConditionMatcher.of(player)
                .requireTranslation("item.fantasydesire.twin_blade")
                .requireSE(FDSpecialEffects.TwinSet)
                .match();

        CapabilityUtils.BladeContext offCtx = CapabilityUtils.SEConditionMatcher.of(player)
                .onlyOffhand()
                .requireTranslation("item.fantasydesire.twin_blade")
                .requireSE(FDSpecialEffects.TwinSet)
                .match();

        if (mainCtx == null || offCtx == null)
            return;
        if (mainCtx.fantasyState.getSpecialType().equals(offCtx.fantasyState.getSpecialType()))
            return;

        int offColor = offCtx.state.getColorCode();
        double damage = event.getDamage();
        System.out.println(mainCtx.state.getComboSeq());
        AddonSlashUtils.doAddonSlash(player, event.getRoll() - 180, player.getYRot(), 0, offColor, 0, Vec3.ZERO, false,
                false, damage, KnockBacks.cancel);
    }
}
