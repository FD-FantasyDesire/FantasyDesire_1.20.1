package tennouboshiuzume.mods.FantasyDesire.specialeffect.effests.twinblade;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialAttacks;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffects;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.specialattack.FDSlashArts;
import tennouboshiuzume.mods.FantasyDesire.utils.AddonSlashUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;


@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TwinBladeEffects {
    //    双持共击
    @SubscribeEvent
    public static void onTwinSlash(SlashBladeEvent.DoSlashEvent event) {
        if (!(event.getUser() instanceof Player player)) return;
        ItemStack blade = player.getMainHandItem();
        ItemStack offBlade = player.getOffhandItem();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade) || !(offBlade.getItem() instanceof ItemFantasySlashBlade)) return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        ISlashBladeState offState = CapabilityUtils.getBladeState(offBlade);
        IFantasySlashBladeState offFdState = CapabilityUtils.getFantasyBladeState(offBlade);
        boolean main = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.TwinSet, player, "item.fantasydesire.twin_blade");
        boolean off = CapabilityUtils.isSpecialEffectActiveForItem(offState, FDSpecialEffects.TwinSet, player, "item.fantasydesire.twin_blade");
        if (!main || !off) return;
        if (fdState.getSpecialType().equals(offFdState.getSpecialType())) return;
        int offColor = offState.getColorCode();
        double damage = event.getDamage();
        AddonSlashUtils.doAddonSlash(player, event.getRoll() - 180, player.getYRot(), 0, offColor, 0, Vec3.ZERO, false, false, damage, KnockBacks.cancel);
    }
//  用于SA施放的检定，必须双持同名拔刀并且形态不同且必须由玩家施放
    public static boolean AntiNTR(LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        ItemStack blade = entity.getMainHandItem();
        ItemStack offblade = entity.getOffhandItem();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade) || !(offblade.getItem() instanceof ItemFantasySlashBlade))
            return false;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        ISlashBladeState offState = CapabilityUtils.getBladeState(offblade);
        IFantasySlashBladeState offFdState = CapabilityUtils.getFantasyBladeState(offblade);
        boolean main = CapabilityUtils.isRightTranslationKey(state, "item.fantasydesire.twin_blade");
        boolean off = CapabilityUtils.isRightTranslationKey(offState, "item.fantasydesire.twin_blade");
        if (fdState.getSpecialType().equals(offFdState.getSpecialType())) return false;
        return main && off;
    }
    public static void ConvertForm(LivingEntity entity,ItemStack blade) {
        if (!(blade.getItem() instanceof ItemFantasySlashBlade)) return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        if (state.getTranslationKey().equals("item.fantasydesire.twin_blade")){
            if (fdState.getSpecialType().equals("TwinBladeL")){
                state.setTexture(new ResourceLocation(FantasyDesire.MODID,"models/twinbladeright.png"));
                state.setSlashArtsKey(FDSpecialAttacks.TWIN_SYSTEM_R.getId());
                state.setColorCode(0xFF0089);
                fdState.setSpecialType("TwinBladeR");
            } else if (fdState.getSpecialType().equals("TwinBladeR")) {
                state.setTexture(new ResourceLocation(FantasyDesire.MODID,"models/twinbladeleft.png"));
                state.setSlashArtsKey(FDSpecialAttacks.TWIN_SYSTEM_L.getId());
                state.setColorCode(0x00C8FF);
                fdState.setSpecialType("TwinBladeL");
            }
            entity.playSound(SoundEvents.RESPAWN_ANCHOR_CHARGE,1,2);
        }
    }
}

