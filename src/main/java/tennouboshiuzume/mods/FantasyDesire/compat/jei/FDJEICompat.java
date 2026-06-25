package tennouboshiuzume.mods.FantasyDesire.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.NotNull;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.init.FDItemsRegistry;
import tennouboshiuzume.mods.FantasyDesire.init.FDRecipeSerializerRegistry;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.IFantasySlashBladeState;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.recipe.SpecialTransformRecipe;

import java.util.List;

@JeiPlugin
public class FDJEICompat implements IModPlugin {

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return FantasyDesire.prefix("jei_compat");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(FDItemsRegistry.FANTASY_SLASHBLADE.get(),
                FDJEICompat::syncFantasySlashBlade);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new SpecialTransformCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level != null) {
            RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
            List<SpecialTransformRecipe> recipes = recipeManager
                    .getAllRecipesFor(FDRecipeSerializerRegistry.SPECIAL_TRANSFORM_TYPE.get());
            registration.addRecipes(SpecialTransformCategory.RECIPE_TYPE, recipes);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(SlashBladeItems.PROUDSOUL_CRYSTAL.get()),
                SpecialTransformCategory.RECIPE_TYPE);
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

        // 获取模型和纹理
        String model = stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.getModel().toString())
                .orElse("");
        String texture = stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.getTexture().toString())
                .orElse("");

        // 获取特殊类型
        String specialType = stack.getCapability(ItemFantasySlashBlade.FDBLADESTATE)
                .map(IFantasySlashBladeState::getSpecialType)
                .orElse("");

        // 组合成唯一标识符
        StringBuilder uid = new StringBuilder(bladeName);
        if (!model.isBlank()) {
            uid.append(":").append(model);
        }
        if (!texture.isBlank()) {
            uid.append(":").append(texture);
        }
        if (!specialType.isBlank() && !specialType.equals("Null")) {
            uid.append(":").append(specialType);
        }

        return uid.toString();
    }
}