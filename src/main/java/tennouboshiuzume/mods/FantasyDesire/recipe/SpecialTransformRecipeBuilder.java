package tennouboshiuzume.mods.FantasyDesire.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import tennouboshiuzume.mods.FantasyDesire.init.FDRecipeSerializerRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SpecialTransformRecipeBuilder implements RecipeBuilder {
    private final ResourceLocation blade;
    private final List<Ingredient> ingredients = new ArrayList<>();
    private final Advancement.Builder advancement = Advancement.Builder.advancement();
    private String tooltip = "";

    public SpecialTransformRecipeBuilder(ResourceLocation blade) {
        this.blade = blade;
    }

    public static SpecialTransformRecipeBuilder transform(ResourceLocation blade) {
        return new SpecialTransformRecipeBuilder(blade);
    }

    public SpecialTransformRecipeBuilder addIngredient(Ingredient ingredient) {
        if (this.ingredients.size() < 3) {
            this.ingredients.add(ingredient);
        }
        return this;
    }

    public SpecialTransformRecipeBuilder addTooltip(String key) {
        this.tooltip = key;
        return this;
    }

    @Override
    public SpecialTransformRecipeBuilder unlockedBy(String pCriterionName, CriterionTriggerInstance pCriterionTrigger) {
        this.advancement.addCriterion(pCriterionName, pCriterionTrigger);
        return this;
    }

    @Override
    public SpecialTransformRecipeBuilder group(@Nullable String pGroupName) {
        return this;
    }

    @Override
    public Item getResult() {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation("fantasydesire", "fantasyslashblade"));
    }

    @Override
    public void save(Consumer<FinishedRecipe> pFinishedRecipeConsumer, ResourceLocation pRecipeId) {
        this.advancement.parent(new ResourceLocation("recipes/root"))
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pRecipeId))
                .rewards(AdvancementRewards.Builder.recipe(pRecipeId)).requirements(RequirementsStrategy.OR);

        pFinishedRecipeConsumer
                .accept(new Result(pRecipeId, this.blade, this.ingredients, this.tooltip, this.advancement,
                        new ResourceLocation(pRecipeId.getNamespace(), "recipes/combat/" + pRecipeId.getPath())));
    }

    public static class Result implements FinishedRecipe {
        private final ResourceLocation id;
        private final ResourceLocation bladeId;
        private final List<Ingredient> ingredients;
        private final String tooltip;
        private final Advancement.Builder advancement;
        private final ResourceLocation advancementId;

        public Result(ResourceLocation id, ResourceLocation bladeId,
                List<Ingredient> ingredients, String tooltip,
                Advancement.Builder advancement, ResourceLocation advancementId) {
            this.id = id;
            this.bladeId = bladeId;
            this.ingredients = ingredients;
            this.tooltip = tooltip;
            this.advancement = advancement;
            this.advancementId = advancementId;
        }

        @Override
        public void serializeRecipeData(JsonObject pJson) {
            JsonArray jsonArray = new JsonArray();
            for (Ingredient ingredient : this.ingredients) {
                jsonArray.add(ingredient.toJson());
            }
            pJson.add("ingredients", jsonArray);

            JsonObject resultObj = new JsonObject();
            resultObj.addProperty("item", "fantasydesire:fantasyslashblade");
            pJson.add("result", resultObj);

            if (this.bladeId != null) {
                pJson.addProperty("blade", this.bladeId.toString());
            }

            if (this.tooltip != null && !this.tooltip.isEmpty()) {
                pJson.addProperty("tooltip", this.tooltip);
            }
        }

        @Override
        public ResourceLocation getId() {
            return this.id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return FDRecipeSerializerRegistry.SPECIAL_TRANSFORM.get();
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return this.advancement.serializeToJson();
        }

        @Nullable
        @Override
        public ResourceLocation getAdvancementId() {
            return this.advancementId;
        }
    }
}
