package tennouboshiuzume.mods.FantasyDesire.recipe;

import mods.flammpfeil.slashblade.SlashBladeConfig;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.data.FantasySlashBladeDefinition;

import java.util.Objects;

public class FantasySlashBladeShapedRecipe extends ShapedRecipe {

    public static final RecipeSerializer<FantasySlashBladeShapedRecipe> SERIALIZER = new FantasySlashBladeShapedRecipeSerializer<>(
            RecipeSerializer.SHAPED_RECIPE, FantasySlashBladeShapedRecipe::new);

    private static final ResourceLocation FANTASY_SLASHBLADE = new ResourceLocation(FantasyDesire.MODID, "fantasyslashblade");

    private final ResourceLocation outputBlade;

    public FantasySlashBladeShapedRecipe(ShapedRecipe compose, ResourceLocation outputBlade) {
        super(compose.getId(), compose.getGroup(), compose.category(), compose.getWidth(), compose.getHeight(),
                compose.getIngredients(), getResultBlade(outputBlade));
        this.outputBlade = outputBlade;
    }

    private static ItemStack getResultBlade(ResourceLocation outputBlade) {
        Item bladeItem = ForgeRegistries.ITEMS.containsKey(outputBlade) ? ForgeRegistries.ITEMS.getValue(outputBlade)
                : ForgeRegistries.ITEMS.getValue(FANTASY_SLASHBLADE);

        return Objects.requireNonNullElseGet(bladeItem, () -> ForgeRegistries.ITEMS.getValue(FANTASY_SLASHBLADE)).getDefaultInstance();
    }

    public ResourceLocation getOutputBlade() {
        return outputBlade;
    }

    private ResourceKey<SlashBladeDefinition> getOutputBladeKey() {
        return ResourceKey.create(SlashBladeDefinition.REGISTRY_KEY, outputBlade);
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess access) {
        ItemStack result = FantasySlashBladeShapedRecipe.getResultBlade(this.getOutputBlade());

        if (!Objects.equals(ForgeRegistries.ITEMS.getKey(result.getItem()), getOutputBlade())) {
            ResourceLocation bladeLocation = this.getOutputBlade();
            if (bladeLocation.getNamespace().equals(FantasyDesire.MODID)) {
                ResourceKey<FantasySlashBladeDefinition> fantasyBladeKey = ResourceKey.create(
                        FantasySlashBladeDefinition.REGISTRY_KEY, bladeLocation);
                result = access.registryOrThrow(FantasySlashBladeDefinition.REGISTRY_KEY)
                        .getOrThrow(fantasyBladeKey).getBlade();
            } else {
                result = access.registryOrThrow(SlashBladeDefinition.REGISTRY_KEY)
                        .getOrThrow(getOutputBladeKey()).getBlade();
            }
        }

        return result;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingContainer container, @NotNull RegistryAccess access) {
        var result = this.getResultItem(access);
        if (!(result.getItem() instanceof ItemSlashBlade)) {
            result = new ItemStack(ForgeRegistries.ITEMS.getValue(FANTASY_SLASHBLADE));
        }

        var resultState = result.getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(NullPointerException::new);
        boolean sumRefine = SlashBladeConfig.DO_CRAFTING_SUM_REFINE.get();
        int proudSoul = resultState.getProudSoulCount();
        int killCount = resultState.getKillCount();
        int refine = resultState.getRefine();
        for (var stack : container.getItems()) {
            if (!(stack.getItem() instanceof ItemSlashBlade)) {
                continue;
            }
            var ingredientState = stack.getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(NullPointerException::new);

            proudSoul += ingredientState.getProudSoulCount();
            killCount += ingredientState.getKillCount();
            if (sumRefine) {
                refine += ingredientState.getRefine();
            } else {
                refine = Math.max(refine, ingredientState.getRefine());
            }
            updateEnchantment(result, stack);
        }
        resultState.setProudSoulCount(proudSoul);
        resultState.setKillCount(killCount);
        resultState.setRefine(refine);
        result.getOrCreateTag().put("bladeState", resultState.serializeNBT());

        return result;
    }

    private void updateEnchantment(ItemStack result, ItemStack ingredient) {
        var newItemEnchants = result.getAllEnchantments();
        var oldItemEnchants = ingredient.getAllEnchantments();
        for (Enchantment enchantIndex : oldItemEnchants.keySet()) {

            int destLevel = newItemEnchants.getOrDefault(enchantIndex, 0);
            int srcLevel = oldItemEnchants.get(enchantIndex);

            srcLevel = Math.max(srcLevel, destLevel);
            srcLevel = Math.min(srcLevel, enchantIndex.getMaxLevel());

            boolean canApplyFlag = enchantIndex.canApplyAtEnchantingTable(result);
            if (canApplyFlag) {
                for (Enchantment curEnchantIndex : newItemEnchants.keySet()) {
                    if (curEnchantIndex != enchantIndex
                            && !enchantIndex.isCompatibleWith(curEnchantIndex)) {
                        canApplyFlag = false;
                        break;
                    }
                }
                if (canApplyFlag) {
                    newItemEnchants.put(enchantIndex, srcLevel);
                }
            }
        }
        EnchantmentHelper.setEnchantments(newItemEnchants, result);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}