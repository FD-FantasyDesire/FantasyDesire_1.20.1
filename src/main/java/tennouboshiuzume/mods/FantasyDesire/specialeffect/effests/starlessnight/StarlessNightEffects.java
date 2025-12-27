package tennouboshiuzume.mods.FantasyDesire.specialeffect.effests.starlessnight;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.init.FDPotionEffects;
import tennouboshiuzume.mods.FantasyDesire.init.FDSpecialEffects;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.ParticleUtils;

import java.util.Set;

@Mod.EventBusSubscriber(modid = FantasyDesire.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StarlessNightEffects {
    // 回响打击
    @SubscribeEvent
    public static void OnHit(SlashBladeEvent.HitEvent event) {
        ItemStack blade = event.getBlade();
        if (!(blade.getItem() instanceof ItemFantasySlashBlade))
            return;
        if (!(event.getUser() instanceof Player player))
            return;
        ISlashBladeState state = CapabilityUtils.getBladeState(blade);
        IFantasySlashBladeState fdState = CapabilityUtils.getFantasyBladeState(blade);
        if (!state.getTranslationKey().equals("item.fantasydesire.starless_night"))
            return;
        LivingEntity target = event.getTarget();
        if (CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.VoidStrike, player,
                "item.fantasydesire.starless_night")) {
            stackVoidStrike(target);
        }
        if (CapabilityUtils.isSpecialEffectActiveForItem(state, FDSpecialEffects.EchoingStrike, player,
                "item.fantasydesire.starless_night")) {
            if (ALLOWED_COMBOS.contains(state.getComboSeq().getPath())) {
                target.playSound(SoundEvents.TRIDENT_THUNDER, 1f, 0.8f);
                ParticleUtils.generateRingParticles(ParticleTypes.END_ROD, target.level(), target.getX(),
                        target.getY() + target.getBbHeight() / 4, target.getZ(), 10, 48);
            }
        }
    }

    private static void stackVoidStrike(LivingEntity entity) {
        int duration = 60;
        int amplifier = 0;
        MobEffect voidStrike = FDPotionEffects.VOID_STRIKE.get();
        // 如果已有这个效果，叠加等级
        MobEffectInstance current = entity.getEffect(voidStrike);
        if (current != null) {
            amplifier = Math.min(current.getAmplifier() + 1, 5);
            duration = current.getDuration();
        }
        entity.addEffect(new MobEffectInstance(voidStrike, duration, amplifier));
    }

    private static final Set<String> ALLOWED_COMBOS = Set.of(
            "aerial_cleave",
            "combo_a4",
            "combo_a5",
            "combo_b1_end",
            "combo_b1_end2",
            "combo_b1_end3",
            "combo_b_end",
            "combo_b_end2",
            "combo_b_end3",
            "combo_c",
            "aerial_rave_b4",
            "rising_star");
}
