package tennouboshiuzume.mods.FantasyDesire.specialeffect.effects.overcold;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
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
        // 使用新的 SEConditionMatcher，只检查翻译键
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(blade, null)
                .requireTranslation("item.fantasydesire.over_cold")
                .match();
        if (ctx == null)
            return;
        ISlashBladeState state = ctx.state;
        IFantasySlashBladeState fdState = ctx.fantasyState;
        fdState.setSpecialCharge(
                Math.min(fdState.getSpecialCharge() + event.getOriginCount(), fdState.getMaxSpecialCharge()));

        // 满值处理进化
        if (fdState.getSpecialCharge() >= fdState.getMaxSpecialCharge()) {
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
