package tennouboshiuzume.mods.FantasyDesire.specialeffect.effests.overcold;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;

@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OverColdEffects {
    // 冰川进化序列 进化事件
    @SubscribeEvent
    public static void OnAddProudSoul(SlashBladeEvent.AddProudSoulEvent event) {
        ItemStack blade = event.getBlade();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade))
            return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        if (state.getTranslationKey().equals("item.fantasydesire.over_cold")) {
            fdState.setSpecialCharge(
                    Math.min(fdState.getSpecialCharge() + event.getOriginCount(), fdState.getMaxSpecialCharge()));
        }
        if (fdState.getSpecialCharge() == fdState.getMaxSpecialCharge()) {
            switch (fdState.getSpecialType()) {
                case "OverCold_0":
                    state.setModel(new ResourceLocation(FantasyDesire.MODID, "models/overcold_1.obj"));
                    state.setBaseAttackModifier(4.0f);
                    fdState.setSpecialChargeName("Evolution_1");
                    fdState.setMaxSpecialCharge(3000);
                    fdState.setSpecialType("OverCold_1");
                    break;
                case "OverCold_1":
                    state.setModel(new ResourceLocation(FantasyDesire.MODID, "models/overcold_2.obj"));
                    state.setBaseAttackModifier(7.2f);
                    fdState.setSpecialChargeName("Evolution_2");
                    fdState.setMaxSpecialCharge(30000);
                    fdState.setSpecialType("OverCold_2");
                    break;
                case "OverCold_2":
                    state.setModel(new ResourceLocation(FantasyDesire.MODID, "models/overcold_3.obj"));
                    state.setBaseAttackModifier(13.0f);
                    fdState.setSpecialChargeName("Evolution_3");
                    fdState.setMaxSpecialCharge(Integer.MAX_VALUE);
                    fdState.setSpecialType("OverCold_3");
                    break;
                default:
                    break;
            }
        }
    }

    @SubscribeEvent
    public static void OnUpdate(SlashBladeEvent.UpdateEvent event) {
        ItemStack blade = event.getBlade();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade))
            return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
    }
}
