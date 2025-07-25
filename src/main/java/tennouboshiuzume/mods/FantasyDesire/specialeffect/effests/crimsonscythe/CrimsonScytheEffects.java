package tennouboshiuzume.mods.FantasyDesire.specialeffect.effests.crimsonscythe;

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
import tennouboshiuzume.mods.FantasyDesire.utils.VecMathUtils;

@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CrimsonScytheEffects {
    //    狩魂 爪刃斩击
    @SubscribeEvent
    public static void onTriSlash(SlashBladeEvent.DoSlashEvent event) {
        ItemStack blade = event.getBlade();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade))
            return;
        if (!(event.getUser() instanceof Player player)) return;
//        获取主手Capability
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
//        检查主手效果激活
        boolean mainActive = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.CrimsonStrike, player, "item.fantasydesire.crimson_scythe");
        int color = state.getColorCode();
        if (mainActive) {
            Vec3 forward = VecMathUtils.getDirectionVector(player.getYRot(), 0);
            Vec3 baseForwardUp = VecMathUtils.getDirectionVector(player.getYRot(), -25);
            Vec3 baseForwardDown = VecMathUtils.getDirectionVector(player.getYRot(), +25);
            Vec3 rotatedUp = VecMathUtils.rotateAroundAxis(baseForwardUp, forward, -event.getRoll());
            Vec3 rotatedDown = VecMathUtils.rotateAroundAxis(baseForwardDown, forward, -event.getRoll());
            float[] upYawPitch = VecMathUtils.getYawPitchFromVec(rotatedUp);
            float[] downYawPitch = VecMathUtils.getYawPitchFromVec(rotatedDown);
            AddonSlashUtils.doAddonSlash(player, event.getRoll(), upYawPitch[0], upYawPitch[1], color, Vec3.ZERO, false, false, 0.1f, KnockBacks.cancel);
            AddonSlashUtils.doAddonSlash(player, event.getRoll(), downYawPitch[0], downYawPitch[1], color, Vec3.ZERO, false, false, 0.1f, KnockBacks.cancel);
        }
    }

    @SubscribeEvent
    public static void onHitEvent(SlashBladeEvent.HitEvent event){
        ItemStack blade = event.getBlade();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade))
            return;
        if (!(event.getUser() instanceof Player player)) return;
//        获取主手Capability
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        boolean mainActive = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.BloodDrain, player, "item.fantasydesire.crimson_scythe");
        if (mainActive){
            state.setProudSoulCount(2000);
        }
    }
}

