package tennouboshiuzume.mods.FantasyDesire.recipe;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.CraftingRecipeBuilder;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class NbtShapedRecipeBuilder extends CraftingRecipeBuilder implements RecipeBuilder {

    private final RecipeCategory category;

    private final ItemStack resultStack;

    private final Item result;

    private final int count;

    private final List<String> rows = Lists.newArrayList();

    private final Map<Character, Ingredient> key = Maps.newLinkedHashMap();

    private final Advancement.Builder advancement = Advancement.Builder.recipeAdvancement();

    @Nullable
    private String group;

    private boolean showNotification = true;

    private NbtShapedRecipeBuilder(RecipeCategory category, ItemStack result) {
        this.category = category;
        this.resultStack = result.copy();
        this.result = result.getItem();
        this.count = result.getCount();
    }

    public static NbtShapedRecipeBuilder nbtShaped(RecipeCategory category, ItemStack result) {
        Objects.requireNonNull(result, "result ItemStack must not be null");
        return new NbtShapedRecipeBuilder(category, result);
    }

    public NbtShapedRecipeBuilder pattern(String pattern) {
        if (!this.rows.isEmpty() && pattern.length() != this.rows.get(0).length()) {
            throw new IllegalArgumentException("Pattern must be the same width on every line!");
        }
        this.rows.add(pattern);
        return this;
    }

    public NbtShapedRecipeBuilder define(Character symbol, ItemLike item) {
        return this.define(symbol, Ingredient.of(item));
    }

    public NbtShapedRecipeBuilder define(Character symbol, TagKey<Item> tag) {
        return this.define(symbol, Ingredient.of(tag));
    }

    public NbtShapedRecipeBuilder define(Character symbol, Ingredient ingredient) {
        if (this.key.containsKey(symbol)) {
            throw new IllegalArgumentException("Symbol '" + symbol + "' is already defined!");
        }
        if (symbol == ' ') {
            throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
        }
        this.key.put(symbol, ingredient);
        return this;
    }

    @Override
    public @NotNull NbtShapedRecipeBuilder unlockedBy(@NotNull String criterionName,
            @NotNull CriterionTriggerInstance criterion) {
        this.advancement.addCriterion(criterionName, criterion);
        return this;
    }

    @Override
    public @NotNull NbtShapedRecipeBuilder group(@Nullable String group) {
        this.group = group;
        return this;
    }

    public NbtShapedRecipeBuilder showNotification(boolean show) {
        this.showNotification = show;
        return this;
    }

    @Override
    public @NotNull Item getResult() {
        return this.result;
    }

    @Override
    public void save(@NotNull Consumer<FinishedRecipe> consumer) {
        this.save(consumer, Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(this.getResult())));
    }

    @Override
    public void save(Consumer<FinishedRecipe> consumer, @NotNull ResourceLocation id) {
        this.ensureValid(id);
        this.advancement.parent(ROOT_RECIPE_ADVANCEMENT)
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
                .rewards(AdvancementRewards.Builder.recipe(id))
                .requirements(RequirementsStrategy.OR);
        consumer.accept(new Result(id, this.resultStack, this.category, this.group == null ? "" : this.group,
                this.rows, this.key, this.advancement,
                id.withPrefix("recipes/" + this.category.getFolderName() + "/"), this.showNotification));
    }

    private void ensureValid(ResourceLocation id) {
        if (this.rows.isEmpty()) {
            throw new IllegalStateException("No pattern is defined for shaped recipe " + id + "!");
        }

        Set<Character> set = Sets.newHashSet(this.key.keySet());
        set.remove(' ');

        for (String row : this.rows) {
            for (int i = 0; i < row.length(); ++i) {
                char c = row.charAt(i);
                if (!this.key.containsKey(c) && c != ' ') {
                    throw new IllegalStateException(
                            "Pattern in recipe " + id + " uses undefined symbol '" + c + "'");
                }
                set.remove(c);
            }
        }

        if (!set.isEmpty()) {
            throw new IllegalStateException(
                    "Ingredients are defined but not used in pattern for recipe " + id);
        }

        if (this.rows.size() == 1 && this.rows.get(0).length() == 1) {
            throw new IllegalStateException(
                    "Shaped recipe " + id
                            + " only takes in a single item - should it be a shapeless recipe instead?");
        }

        if (this.advancement.getCriteria().isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + id);
        }
    }

    public static class Result extends CraftingRecipeBuilder.CraftingResult {

        private final ResourceLocation id;

        private final ItemStack resultStack;

        private final String group;

        private final List<String> pattern;

        private final Map<Character, Ingredient> key;

        private final Advancement.Builder advancement;

        private final ResourceLocation advancementId;

        private final boolean showNotification;

        public Result(ResourceLocation id, ItemStack resultStack, RecipeCategory category,
                String group, List<String> pattern, Map<Character, Ingredient> key,
                Advancement.Builder advancement, ResourceLocation advancementId,
                boolean showNotification) {
            super(mapCategory(category));
            this.id = id;
            this.resultStack = resultStack;
            this.group = group;
            this.pattern = pattern;
            this.key = key;
            this.advancement = advancement;
            this.advancementId = advancementId;
            this.showNotification = showNotification;
        }

        private static CraftingBookCategory mapCategory(RecipeCategory category) {
            return switch (category) {
                case BUILDING_BLOCKS, DECORATIONS, TRANSPORTATION -> CraftingBookCategory.BUILDING;
                case REDSTONE -> CraftingBookCategory.REDSTONE;
                case TOOLS, COMBAT -> CraftingBookCategory.EQUIPMENT;
                case FOOD, BREWING, MISC -> CraftingBookCategory.MISC;
            };
        }

        @Override
        public void serializeRecipeData(@NotNull JsonObject json) {
            super.serializeRecipeData(json);

            if (!this.group.isEmpty()) {
                json.addProperty("group", this.group);
            }

            JsonArray patternArray = new JsonArray();
            for (String row : this.pattern) {
                patternArray.add(row);
            }
            json.add("pattern", patternArray);

            JsonObject keyObject = new JsonObject();
            for (Map.Entry<Character, Ingredient> entry : this.key.entrySet()) {
                keyObject.add(String.valueOf(entry.getKey()), entry.getValue().toJson());
            }
            json.add("key", keyObject);

            JsonObject resultObject = new JsonObject();
            resultObject.addProperty("item",
                    Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(this.resultStack.getItem())).toString());

            if (this.resultStack.getCount() > 1) {
                resultObject.addProperty("count", this.resultStack.getCount());
            }

            if (this.resultStack.hasTag()) {
                CompoundTag tag = this.resultStack.getTag();
                if (tag != null && !tag.isEmpty()) {
                    resultObject.addProperty("nbt", tag.toString());
                }
            }

            json.add("result", resultObject);

            json.addProperty("show_notification", this.showNotification);
        }

        @Override
        public @NotNull RecipeSerializer<?> getType() {
            return RecipeSerializer.SHAPED_RECIPE;
        }

        @Override
        public @NotNull ResourceLocation getId() {
            return this.id;
        }

        @Override
        @Nullable
        public JsonObject serializeAdvancement() {
            return this.advancement.serializeToJson();
        }

        @Override
        @Nullable
        public ResourceLocation getAdvancementId() {
            return this.advancementId;
        }
    }
}
