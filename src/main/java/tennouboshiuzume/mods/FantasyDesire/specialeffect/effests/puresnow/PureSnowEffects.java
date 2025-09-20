package tennouboshiuzume.mods.FantasyDesire.specialeffect.effests.puresnow;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffects;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ColorUtils;

import java.awt.*;

@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PureSnowEffects {
//    虹光通量
    @SubscribeEvent
    public static void updateEvent(SlashBladeEvent.UpdateEvent event) {
        ItemStack blade = event.getBlade();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade))
            return;
        if (!(event.getEntity() instanceof Player player)) return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        if (!state.getTranslationKey().equals("item.fantasydesire.pure_snow")) return;
        boolean mainActive = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.RainbowFlux, player, "item.fantasydesire.pure_snow");
        if (mainActive) {
            int color = ColorUtils.getSmoothTransitionColor(player.level().getDayTime() % 126, 126, true);
            state.setColorCode(color);
            int timeStep = (int) ((player.level().getDayTime() + 8) % 126);
            int damageTypeIndex = (timeStep / 18);
            fdState.setSpecialAttackEffect(damageTypes[damageTypeIndex]);
        }
    }
//    虹羽七刃剑
    @SubscribeEvent
    public static void onSlash(SlashBladeEvent.DoSlashEvent event) {
        ItemStack blade = event.getBlade();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade))
            return;
        if (!(event.getUser() instanceof Player player)) return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        if (!state.getTranslationKey().equals("item.fantasydesire.pure_snow")) return;
        if (!CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.RainbowFlux, player, "item.fantasydesire.pure_snow")) return;
        for (int i = 0; i < 7; i++) {
            int cl = ColorUtils.getSmoothTransitionColor(i,7,true);
            EntitySlashEffect jc = new EntitySlashEffect(SlashBlade.RegistryEvents.SlashEffect, player.level());
            Vec3 pos = player.position().add(new Vec3(0,0,jc.getBbHeight()/2).yRot(-(float) Math.toRadians(360 /7*i+player.getYRot())));
            jc.setPos(pos.x, pos.y+player.getBbHeight()/2, pos.z);
            jc.setOwner(player);
            jc.setRotationRoll(0);
            jc.setYRot((float) 360 /7*i+player.getYRot());
            jc.setXRot(0);
            jc.setColor(cl);
            jc.setRotationRoll(0f);
            jc.setMute(true);
            jc.setIsCritical(false);
            jc.setDamage(1);
            jc.setKnockBack(KnockBacks.cancel);
            if (player != null) {
                player.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT).ifPresent((rank) -> {
                    jc.setRank(rank.getRankLevel(player.level().getGameTime()));
                });
            }
            player.level().addFreshEntity(jc);
        }

    }
//    棱光通量
    public static String[] damageTypes = {"wrath", "lust", "sloth", "gluttony", "gloom", "pride", "envy"};

}
