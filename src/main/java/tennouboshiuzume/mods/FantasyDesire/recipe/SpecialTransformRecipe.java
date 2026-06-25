package tennouboshiuzume.mods.FantasyDesire.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mods.flammpfeil.slashblade.SlashBladeConfig;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.data.FantasySlashBladeDefinition;
import tennouboshiuzume.mods.FantasyDesire.init.FDRecipeSerializerRegistry;

import java.util.Objects;

public class SpecialTransformRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final NonNullList<Ingredient> inputs;
    private final ItemStack result;
    private final ResourceLocation outputBlade;
    private final String tooltip;
    private static final ResourceLocation FANTASY_SLASHBLADE = new ResourceLocation(FantasyDesire.MODID,
            "fantasyslashblade");

    public SpecialTransformRecipe(ResourceLocation id, NonNullList<Ingredient> inputs, ItemStack result,
            ResourceLocation outputBlade, String tooltip) {
        this.id = id;
        this.inputs = inputs;
        this.result = result;
        this.outputBlade = outputBlade;
        this.tooltip = tooltip != null ? tooltip : "";
    }

    @Override
    public boolean matches(Container pContainer, Level pLevel) {
        return false; // Typically matched via JEI or custom container
    }

    private ItemStack getResultBlade(ResourceLocation outputBlade, RegistryAccess access) {
        if (outputBlade == null)
            return this.result.copy();

        Item bladeItem = ForgeRegistries.ITEMS.containsKey(outputBlade) ? ForgeRegistries.ITEMS.getValue(outputBlade)
                : ForgeRegistries.ITEMS.getValue(FANTASY_SLASHBLADE);

        ItemStack bladeStack = Objects
                .requireNonNullElseGet(bladeItem, () -> ForgeRegistries.ITEMS.getValue(FANTASY_SLASHBLADE))
                .getDefaultInstance();

        if (!Objects.equals(ForgeRegistries.ITEMS.getKey(bladeStack.getItem()), outputBlade)) {
            if (outputBlade.getNamespace().equals(FantasyDesire.MODID)) {
                ResourceKey<FantasySlashBladeDefinition> fantasyBladeKey = ResourceKey.create(
                        FantasySlashBladeDefinition.REGISTRY_KEY, outputBlade);
                bladeStack = access.registryOrThrow(FantasySlashBladeDefinition.REGISTRY_KEY)
                        .getOrThrow(fantasyBladeKey).getBlade();
            } else {
                ResourceKey<SlashBladeDefinition> bladeKey = ResourceKey.create(
                        SlashBladeDefinition.REGISTRY_KEY, outputBlade);
                bladeStack = access.registryOrThrow(SlashBladeDefinition.REGISTRY_KEY)
                        .getOrThrow(bladeKey).getBlade();
            }
        }
        return bladeStack;
    }

    @Override
    public @NotNull ItemStack assemble(Container pContainer, RegistryAccess pRegistryAccess) {
        ItemStack resultStack = getResultItem(pRegistryAccess).copy();

        if (this.outputBlade != null && resultStack.getItem() instanceof ItemSlashBlade) {
            var resultState = resultStack.getCapability(ItemSlashBlade.BLADESTATE).orElse(null);
            if (resultState != null) {
                boolean sumRefine = SlashBladeConfig.DO_CRAFTING_SUM_REFINE.get();
                int proudSoul = resultState.getProudSoulCount();
                int killCount = resultState.getKillCount();
                int refine = resultState.getRefine();

                for (int i = 0; i < pContainer.getContainerSize(); i++) {
                    ItemStack stack = pContainer.getItem(i);
                    if (stack.isEmpty() || !(stack.getItem() instanceof ItemSlashBlade)) {
                        continue;
                    }
                    var ingredientState = stack.getCapability(ItemSlashBlade.BLADESTATE).orElse(null);
                    if (ingredientState != null) {
                        proudSoul += ingredientState.getProudSoulCount();
                        killCount += ingredientState.getKillCount();
                        if (sumRefine) {
                            refine += ingredientState.getRefine();
                        } else {
                            refine = Math.max(refine, ingredientState.getRefine());
                        }
                        updateEnchantment(resultStack, stack);
                    }
                    var fdIngredientState = stack.getCapability(
                            tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade.FDBLADESTATE)
                            .orElse(null);
                    var fdResultState = resultStack.getCapability(
                            tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade.FDBLADESTATE)
                            .orElse(null);
                    if (fdIngredientState != null && fdResultState != null) {
                        fdResultState.setSpecialCharge(
                                Math.max(fdResultState.getSpecialCharge(), fdIngredientState.getSpecialCharge()));
                        resultStack.getOrCreateTag().put("fdBladeState", fdResultState.serializeNBT());
                    }
                }
                resultState.setProudSoulCount(proudSoul);
                resultState.setKillCount(killCount);
                resultState.setRefine(refine);
                resultStack.getOrCreateTag().put("bladeState", resultState.serializeNBT());
            }
        }
        return resultStack;
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
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(RegistryAccess pRegistryAccess) {
        if (this.outputBlade != null && pRegistryAccess != null) {
            return getResultBlade(this.outputBlade, pRegistryAccess);
        }
        return this.result;
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return this.id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return FDRecipeSerializerRegistry.SPECIAL_TRANSFORM.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return FDRecipeSerializerRegistry.SPECIAL_TRANSFORM_TYPE.get();
    }

    public NonNullList<Ingredient> getIngredients() {
        return inputs;
    }

    public ResourceLocation getOutputBlade() {
        return outputBlade;
    }

    public String getTooltip() {
        return tooltip;
    }

    public static class Serializer implements RecipeSerializer<SpecialTransformRecipe> {
        @Override
        public @NotNull SpecialTransformRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
            JsonArray ingredientsArray = GsonHelper.getAsJsonArray(pSerializedRecipe, "ingredients");
            NonNullList<Ingredient> inputs = NonNullList.withSize(3, Ingredient.EMPTY);
            for (int i = 0; i < ingredientsArray.size() && i < 3; i++) {
                inputs.set(i, Ingredient.fromJson(ingredientsArray.get(i)));
            }
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(pSerializedRecipe, "result"));
            ResourceLocation blade = null;
            if (pSerializedRecipe.has("blade")) {
                blade = new ResourceLocation(GsonHelper.getAsString(pSerializedRecipe, "blade"));
            }
            String tooltip = GsonHelper.getAsString(pSerializedRecipe, "tooltip", "");
            return new SpecialTransformRecipe(pRecipeId, inputs, result, blade, tooltip);
        }

        @Override
        public SpecialTransformRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
            NonNullList<Ingredient> inputs = NonNullList.withSize(3, Ingredient.EMPTY);
            for (int i = 0; i < inputs.size(); i++) {
                inputs.set(i, Ingredient.fromNetwork(pBuffer));
            }
            ItemStack result = pBuffer.readItem();
            ResourceLocation blade = null;
            if (pBuffer.readBoolean()) {
                blade = pBuffer.readResourceLocation();
            }
            String tooltip = pBuffer.readUtf();
            return new SpecialTransformRecipe(pRecipeId, inputs, result, blade, tooltip);
        }

        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, SpecialTransformRecipe pRecipe) {
            for (Ingredient ingredient : pRecipe.inputs) {
                ingredient.toNetwork(pBuffer);
            }
            pBuffer.writeItem(pRecipe.result);
            pBuffer.writeBoolean(pRecipe.outputBlade != null);
            if (pRecipe.outputBlade != null) {
                pBuffer.writeResourceLocation(pRecipe.outputBlade);
            }
            pBuffer.writeUtf(pRecipe.tooltip);
        }
    }
}
