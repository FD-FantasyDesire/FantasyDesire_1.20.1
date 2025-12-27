package tennouboshiuzume.mods.FantasyDesire.utils;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;

import java.util.Optional;

public class CapabilityUtils {
    public static final Capability<IFantasySlashBladeState> FDBLADESTATE = CapabilityManager
            .get(new CapabilityToken<IFantasySlashBladeState>() {
            });
    public static final Capability<ISlashBladeState> BLADESTATE = CapabilityManager
            .get(new CapabilityToken<ISlashBladeState>() {
            });

    public static ISlashBladeState getBladeState(ItemStack blade) {
        Optional<ISlashBladeState> StateOpt = blade.getCapability(BLADESTATE).resolve();
        return StateOpt.get();
    }

    public static IFantasySlashBladeState getFantasyBladeState(ItemStack blade) {
        Optional<IFantasySlashBladeState> fdStateOpt = blade.getCapability(FDBLADESTATE).resolve();
        return fdStateOpt.get();
    }

    public static boolean isSpecialEffectActive(ISlashBladeState state, RegistryObject<SpecialEffect> effect,
            LivingEntity entity) {
        int level = 0;
        if (entity instanceof Player player) {
            level = player.experienceLevel;
        } else if (entity instanceof LivingEntity) {
            level = effect.get().getRequestLevel();
        }
        return (SpecialEffect.isEffective(effect.getId(), level)
                && state.hasSpecialEffect(effect.getId()));
    }

    public static boolean isRightTranslationKey(ISlashBladeState state, String itemTranslationKey) {
        return state.getTranslationKey().equals(itemTranslationKey);
    }

    public static boolean isSpecialEffectActiveForItem(ISlashBladeState state, RegistryObject<SpecialEffect> effect,
            LivingEntity entity, String itemTranslationKey) {
        return isSpecialEffectActive(state, effect, entity) && state.getTranslationKey().equals(itemTranslationKey);
    }

}
