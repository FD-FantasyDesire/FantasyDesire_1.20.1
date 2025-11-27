package tennouboshiuzume.mods.FantasyDesire.specialeffect.effests.puresnow;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.damagesource.FDDamageSource;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffects;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ColorUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.FDAttackManager;
import tennouboshiuzume.mods.FantasyDesire.utils.VecMathUtils;

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
        boolean mainActive = CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.RainbowFlux, player, "item.fantasydesire.pure_snow");
        if (mainActive) {
            int eachColorZone = 15;
            int totalSteps = eachColorZone * 7;
            long tickCount = player.tickCount;
            int timeStep = (int) (tickCount % totalSteps);
            int color = ColorUtils.getSmoothTransitionColor(timeStep, totalSteps, true);
            state.setColorCode(color);
            int stepsPerType = totalSteps / 7;
            int damageTypeIndex = ((timeStep + stepsPerType / 2) * 7 / totalSteps) % 7;
            fdState.setSpecialAttackEffect(damageTypes[damageTypeIndex]);
        }
    }

    //    虹羽七刃剑
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSlash(SlashBladeEvent.DoSlashEvent event) {
        ItemStack blade = event.getBlade();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade)) return;
        if (!(event.getUser() instanceof Player player)) return;
        if (!event.getUser().hasEffect(FDPotionEffects.RAINBOW_SEVEN_EDGE.get())) return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        if (!CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.RainbowFlux, player, "item.fantasydesire.pure_snow"))
            return;
        double damage = event.getDamage();
        String fdDamageType = fdState.getSpecialAttackEffect();
        if (fdDamageType != null && !fdDamageType.equals("Null")) {
            DamageSource fds = FDDamageSource.getEntityDamageSource(player.level(), FDDamageSource.fromString(fdDamageType), player);
            FDAttackManager.areaAttackWithSource(event.getUser(), KnockBacks.cancel.action, (float) (damage * 7), true, true, false, null, fds);
        }
        for (int i = 0; i < 7; i++) {
            int cl = ColorUtils.getSmoothTransitionColor(i, 7, true);
            EntitySlashEffect jc = new EntitySlashEffect(SlashBlade.RegistryEvents.SlashEffect, player.level());
            Vec3 adds = new Vec3(0,0,jc.getBbHeight()/2).yRot((float) Math.toRadians(360/7*i));
            adds = VecMathUtils.rotateAroundAxis(adds,new Vec3(0,0,1).yRot((float) Math.toRadians(-player.getYRot())),event.getRoll());
            Vec3 pos = player.position().add(adds);
            jc.setPos(pos.x, pos.y + player.getBbHeight() / 2, pos.z);
            jc.setOwner(null);
            jc.setRotationRoll(event.getRoll());
            jc.setYRot(player.getYRot());
            jc.setRotationOffset((float) 360 / 7 * i + player.getYRot());
            jc.setXRot(0);
            jc.setColor(cl);
            jc.setMute(true);
            jc.setIsCritical(false);
            jc.setDamage(0);
            jc.setKnockBack(KnockBacks.cancel);
            if (player != null) {
                player.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT).ifPresent((rank) -> {
                    jc.setRank(rank.getRankLevel(player.level().getGameTime()));
                });
            }
            player.level().addFreshEntity(jc);
        }
        event.setCanceled(true);
    }

    public static boolean AntiNTR(LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        ItemStack blade = entity.getMainHandItem();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade)) return false;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        return state.getTranslationKey().equals("item.fantasydesire.pure_snow");
    }
    //    棱光通量
    public static String[] damageTypes = {"wrath", "lust", "sloth", "gluttony", "gloom", "pride", "envy"};
}
