package tennouboshiuzume.mods.FantasyDesire.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.init.FDItemsRegistry;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;

@JeiPlugin
public class FDJEICompat implements IModPlugin {

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return FantasyDesire.prefix("jei_compat");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(FDItemsRegistry.FANTASY_SLASHBLADE.get(), FDJEICompat::syncFantasySlashBlade);
    }

    public static String syncFantasySlashBlade(ItemStack stack, UidContext context) {
        // 同步nbt到BladeState Cap
        stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(cap -> {
            if (stack.getOrCreateTag().contains("bladeState")) {
                cap.deserializeNBT(stack.getOrCreateTag().getCompound("bladeState"));
            }
        });

        // 同步nbt到FantasyBladeState Cap
        stack.getCapability(ItemFantasySlashBlade.FDBLADESTATE).ifPresent(cap -> {
            if (stack.getOrCreateTag().contains("fdBladeState")) {
                cap.deserializeNBT(stack.getOrCreateTag().getCompound("fdBladeState"));
            }
        });

        // 获取刀的名称
        String bladeName = stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getTranslationKey)
                .orElse("");

        // 获取特殊类型
        String specialType = stack.getCapability(ItemFantasySlashBlade.FDBLADESTATE)
                .map(IFantasySlashBladeState::getSpecialType)
                .orElse("");

        // 如果有特殊类型，组合成唯一标识符
        if (!specialType.isBlank() && !specialType.equals("Null")) {
            return bladeName + ":" + specialType;
        }

        return bladeName;
    }
}