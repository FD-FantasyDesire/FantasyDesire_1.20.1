package tennouboshiuzume.mods.FantasyDesire.specialeffects.effects.overcold;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffectsRegistry;
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
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(blade, null)
                .requireTranslation("item.fantasydesire.over_cold")
                .requireSE(FDSpecialEffectsRegistry.EvolutionIce)
                .match();
        if (ctx == null)
            return;
        ISlashBladeState state = ctx.state;
        IFantasySlashBladeState fdState = ctx.fantasyState;
        int evolutionTier = getEvolutionTier(fdState.getSpecialType());
        int finalMultiple = evolutionTier > 1 ? 3 : 1;
        // 如果已进化到2+，则使进化点数收集率增加
        CapabilityUtils.addSpecialCharge(fdState, event.getOriginCount() * finalMultiple);

        // 满值处理进化
        if (fdState.getSpecialCharge() >= fdState.getMaxSpecialCharge() && evolutionTier != 3) {
            switch (evolutionTier) {
                case 0:
                    state.setModel(new ResourceLocation(FantasyDesire.MODID, "models/overcold_1.obj"));
                    state.setBaseAttackModifier(4.0f);
                    fdState.setSpecialChargeName("Evolution_1");
                    fdState.setMaxSpecialCharge(3000);
                    fdState.setSpecialType("OverCold_1");
                    break;
                case 1:
                    state.setModel(new ResourceLocation(FantasyDesire.MODID, "models/overcold_2.obj"));
                    state.setBaseAttackModifier(7.2f);
                    fdState.setSpecialChargeName("Evolution_2");
                    fdState.setMaxSpecialCharge(30000);
                    fdState.setSpecialType("OverCold_2");
                    break;
                case 2:
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
    public static void OnHit(SlashBladeEvent.HitEvent event) {
        CapabilityUtils.BladeContext ctx = CapabilityUtils.SEConditionMatcher.of(event.getBlade(), event.getUser())
                .requireTranslation("item.fantasydesire.over_cold")
                .requireSE(FDSpecialEffectsRegistry.ColdLeak)
                .match();
        if (ctx == null)
            return;
        IFantasySlashBladeState fdState = ctx.fantasyState;
        LivingEntity target = event.getTarget();
        int evolutionTier = getEvolutionTier(fdState.getSpecialType());
        if (evolutionTier == 3) {
            CapabilityUtils.addSpecialCharge(fdState, 2);
        }
        // 施加/刷新效果
        target.addEffect(
                new MobEffectInstance(FDPotionEffects.FROST_BITE.get(), 120, evolutionTier));
    }

    public static int getEvolutionTier(String specialType) {
        return switch (specialType) {
            case "OverCold_1" -> 1;
            case "OverCold_2" -> 2;
            case "OverCold_3" -> 3;
            default -> 0;
        };
    }
}
