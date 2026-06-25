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

/**
 * <h1>NbtShapedRecipeBuilder — 支持 NBT 输出的有序合成配方 Builder</h1>
 *
 * <p>与标准 {@link net.minecraft.data.recipes.ShapedRecipeBuilder} 的核心区别在于：
 * 本 Builder 接收一个完整的 {@link ItemStack}（可能带有 {@link CompoundTag}）作为配方输出，
 * 并在生成的配方 JSON 的 {@code "result"} 对象中序列化 NBT 数据。</p>
 *
 * <h2>设计模式</h2>
 * <p>采用<strong>装饰器/包装模式</strong>的思想：由于 {@code ShapedRecipeBuilder.Result}
 * 是包级私有的静态内部类，
 * 无法直接继承，本类自行管理 pattern/key/advancement 等状态，并提供一个自定义的 {@link Result}
 * 实现来完成 NBT 序列化。</p>
 *
 * <h2>用法示例</h2>
 * <pre>{@code
 * ItemStack nbtSword = new ItemStack(Items.DIAMOND_SWORD);
 * CompoundTag tag = new CompoundTag();
 * tag.putInt("Damage", 0);
 * CompoundTag display = new CompoundTag();
 * display.putString("Name", "{\"text\":\"神剑\"}");
 * tag.put("display", display);
 * nbtSword.setTag(tag);
 *
 * NbtShapedRecipeBuilder.nbtShaped(RecipeCategory.COMBAT, nbtSword)
 * .pattern("XXX")
 * .pattern("X X")
 * .pattern("XXX")
 * .define('X', Items.IRON_INGOT)
 * .unlockedBy("has_iron", has(Items.IRON_INGOT))
 * .save(consumer, new ResourceLocation("mymod", "my_nbt_sword"));
 * }</pre>
 *
 * <h2>生成的 JSON 结构</h2>
 * <pre>{@code
 * {
 * "type": "minecraft:crafting_shaped",
 * "category": "misc",
 * "pattern": ["XXX", "X X", "XXX"],
 * "key": { "X": { "item": "minecraft:iron_ingot" } },
 * "result": {
 * "item": "minecraft:diamond_sword",
 * "nbt": "{Damage:0,display:{Name:'{\"text\":\"神剑\"}'}}"
 * }
 * }
 * }</pre>
 *
 * <p>注意：返回的配方类型为 {@code "minecraft:crafting_shaped"}（标准有序合成），
 * 因为本质上这仍然是原版有序合成配方，只是在输出物品上附加了 NBT 数据。
 * Forge 的配方系统会在解析时识别 {@code "nbt"} 字段并将其应用到输出物品上。</p>
 *
 * @author FantasyDesire Team
 * @see net.minecraft.data.recipes.ShapedRecipeBuilder
 * @see net.minecraft.world.item.ItemStack
 * @see net.minecraft.nbt.CompoundTag
 */
public class NbtShapedRecipeBuilder extends CraftingRecipeBuilder implements RecipeBuilder {

    /** 配方分类（misc、combat、food 等） */
    private final RecipeCategory category;

    /** 配方输出物品（保留原始 ItemStack 以携带 NBT 数据） */
    private final ItemStack resultStack;

    /** 输出物品的纯 Item 引用 */
    private final Item result;

    /** 输出物品数量 */
    private final int count;

    /** 合成格图案行列表 */
    private final List<String> rows = Lists.newArrayList();

    /** 符号到原料的映射 */
    private final Map<Character, Ingredient> key = Maps.newLinkedHashMap();

    /** 进度 Builder */
    private final Advancement.Builder advancement = Advancement.Builder.recipeAdvancement();

    /** 配方分组（可选） */
    @Nullable
    private String group;

    /** 是否显示解锁通知 */
    private boolean showNotification = true;

    /**
     * 私有构造器，通过静态工厂方法 {@link #nbtShaped(RecipeCategory, ItemStack)} 创建实例。
     *
     * @param category 配方分类（决定解锁进度的目录名）
     * @param result   配方输出的 ItemStack（可能携带 NBT 数据）
     */
    private NbtShapedRecipeBuilder(RecipeCategory category, ItemStack result) {
        this.category = category;
        this.resultStack = result.copy();
        this.result = result.getItem();
        this.count = result.getCount();
    }

    /**
     * <h2>静态工厂方法</h2>
     * 创建一个新的 NbtShapedRecipeBuilder 实例。
     * <p>
     * 传入的 {@link ItemStack} 将被复制（通过 {@link ItemStack#copy()}），
     * 因此后续对原始 ItemStack 的修改不会影响配方输出。
     * </p>
     *
     * @param category 配方分类，例如
     *                 {@link RecipeCategory#COMBAT}、{@link RecipeCategory#MISC}
     * @param result   配方输出的 ItemStack，可携带 {@link CompoundTag}
     * @return 新的 NbtShapedRecipeBuilder 实例（支持链式调用）
     * @throws NullPointerException 如果 result 为 null
     */
    public static NbtShapedRecipeBuilder nbtShaped(RecipeCategory category, ItemStack result) {
        Objects.requireNonNull(result, "result ItemStack must not be null");
        return new NbtShapedRecipeBuilder(category, result);
    }

    /**
     * <h2>定义合成格的一行图案</h2>
     * <p>
     * 每行字符串长度必须相同（即合成格是矩形的）。
     * 字符串中的每个字符对应一个已定义的符号键。
     * </p>
     *
     * @param pattern 一行图案字符串，例如 {@code "XXX"}、{@code "X X"}
     * @return 自身引用（链式调用）
     * @throws IllegalArgumentException 如果该行长度与之前定义的行不一致
     */
    public NbtShapedRecipeBuilder pattern(String pattern) {
        if (!this.rows.isEmpty() && pattern.length() != this.rows.get(0).length()) {
            throw new IllegalArgumentException("Pattern must be the same width on every line!");
        }
        this.rows.add(pattern);
        return this;
    }

    /**
     * <h2>通过 {@link ItemLike} 定义符号</h2>
     * <p>
     * 将合成格中的符号字符映射到指定的物品。
     * </p>
     *
     * @param symbol 符号字符（大小写敏感，空格 ' ' 为保留字符）
     * @param item   对应的物品
     * @return 自身引用（链式调用）
     * @throws IllegalArgumentException 如果该符号已被定义或为空格
     */
    public NbtShapedRecipeBuilder define(Character symbol, ItemLike item) {
        return this.define(symbol, Ingredient.of(item));
    }

    /**
     * <h2>通过 {@link TagKey} 定义符号</h2>
     * <p>
     * 将合成格中的符号字符映射到指定的物品标签。
     * </p>
     *
     * @param symbol 符号字符
     * @param tag    对应的物品标签（例如 {@code Tags.Items.INGOTS_IRON}）
     * @return 自身引用（链式调用）
     * @throws IllegalArgumentException 如果该符号已被定义或为空格
     */
    public NbtShapedRecipeBuilder define(Character symbol, TagKey<Item> tag) {
        return this.define(symbol, Ingredient.of(tag));
    }

    /**
     * <h2>通过 {@link Ingredient} 定义符号</h2>
     * <p>
     * 这是最通用的定义方法，支持任意原料类型。
     * </p>
     *
     * @param symbol     符号字符
     * @param ingredient 对应的原料（可以是物品、标签、或自定义原料类型的组合）
     * @return 自身引用（链式调用）
     * @throws IllegalArgumentException 如果该符号已被定义或为空格
     */
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

    /**
     * <h2>设置解锁条件</h2>
     * <p>
     * 定义玩家获得该配方的触发条件（通常为"拥有某个物品"）。
     * </p>
     *
     * @param criterionName 条件名称（推荐使用 {@link RecipeBuilder#getHasName(ItemLike)}）
     * @param criterion     触发条件实例
     * @return 自身引用（链式调用）
     */
    @Override
    public @NotNull NbtShapedRecipeBuilder unlockedBy(@NotNull String criterionName,
            @NotNull CriterionTriggerInstance criterion) {
        this.advancement.addCriterion(criterionName, criterion);
        return this;
    }

    /**
     * <h2>设置配方分组</h2>
     * <p>
     * 同一分组的配方在配方书中只会显示一个（类似多种木材类型合成木棍的设计）。
     * </p>
     *
     * @param group 分组名称，传 {@code null} 表示不分组
     * @return 自身引用（链式调用）
     */
    @Override
    public @NotNull NbtShapedRecipeBuilder group(@Nullable String group) {
        this.group = group;
        return this;
    }

    /**
     * <h2>设置是否显示解锁通知</h2>
     *
     * @param show {@code true} 表示解锁时弹出通知，{@code false} 表示静默解锁
     * @return 自身引用（链式调用）
     */
    public NbtShapedRecipeBuilder showNotification(boolean show) {
        this.showNotification = show;
        return this;
    }

    /**
     * <h2>获取配方输出物品</h2>
     *
     * @return 配方的输出物品（不含 NBT）
     */
    @Override
    public @NotNull Item getResult() {
        return this.result;
    }

    /**
     * <h2>保存配方</h2>
     * <p>
     * 使用默认的 ID（取结果物品的注册名）保存配方。
     * </p>
     *
     * @param consumer 配方消费者（由数据生成器提供，负责将 FinishedRecipe 写入文件）
     */
    @Override
    public void save(@NotNull Consumer<FinishedRecipe> consumer) {
        this.save(consumer, Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(this.getResult())));
    }

    /**
     * <h2>保存配方（指定 ID）</h2>
     * <p>
     * 验证配方有效性后，生成自定义的 {@link Result}（包含 NBT 序列化逻辑），
     * 并将其传递给消费者。
     * </p>
     *
     * @param consumer 配方消费者
     * @param id       配方的唯一标识符（ResourceLocation）
     * @throws IllegalStateException 如果配方定义不完整或无效
     */
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

    /**
     * <h2>验证配方定义的有效性</h2>
     * <p>
     * 检查以下条件：
     * </p>
     * <ul>
     * <li>是否定义了图案</li>
     * <li>图案中使用的所有符号是否都已定义</li>
     * <li>是否存在定义了但未在图案中使用的符号</li>
     * <li>配方是否过于简单（单个物品的无序合成应使用 shapeless 配方）</li>
     * <li>是否至少有一个解锁条件</li>
     * </ul>
     *
     * @param id 配方 ID（用于错误消息）
     * @throws IllegalStateException 如果任何验证条件不满足
     */
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

    /**
     * <h2>自定义 FinishedRecipe 实现</h2>
     * <p>继承自 {@link CraftingRecipeBuilder.CraftingResult}，
     * 在 {@link #serializeRecipeData(JsonObject)} 中额外处理 NBT 序列化。</p>
     *
     * <p>生成的 {@code "result"} JSON 结构：</p>
     * <pre>{@code
     * {
     * "item": "modid:item_name",
     * "count": 1, // 仅当 count > 1 时写入
     * "nbt": "{...}" // 仅当 ItemStack 携带 CompoundTag 时写入
     * }
     * }</pre>
     */
    public static class Result extends CraftingRecipeBuilder.CraftingResult {

        /** 配方唯一标识符 */
        private final ResourceLocation id;

        /** 配方输出物品栈（保留原始 NBT 数据） */
        private final ItemStack resultStack;

        /** 配方分组 */
        private final String group;

        /** 合成格图案 */
        private final List<String> pattern;

        /** 符号到原料的映射 */
        private final Map<Character, Ingredient> key;

        /** 进度 Builder */
        private final Advancement.Builder advancement;

        /** 进度文件 ID */
        private final ResourceLocation advancementId;

        /** 是否显示解锁通知 */
        private final boolean showNotification;

        /**
         * 构造一个 NBT 感知的 FinishedRecipe 结果。
         *
         * @param id               配方 ID
         * @param resultStack      携带 NBT 的输出 ItemStack
         * @param category         配方分类
         * @param group            分组名（空字符串表示无分组）
         * @param pattern          合成格图案行列表
         * @param key              符号到原料的映射
         * @param advancement      进度 Builder
         * @param advancementId    进度文件 ID
         * @param showNotification 是否显示解锁通知
         */
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

        /**
         * 将 {@link RecipeCategory} 安全映射到 {@link CraftingBookCategory}。
         * <p>
         * 两个枚举的常量名并非一一对应（例如 {@code RecipeCategory.COMBAT}
         * 应映射到 {@code CraftingBookCategory.EQUIPMENT}），因此不能直接用
         * {@code valueOf(name())} 转换。
         * </p>
         *
         * @param category 配方分类
         * @return 对应的配方书分类
         */
        private static CraftingBookCategory mapCategory(RecipeCategory category) {
            return switch (category) {
                case BUILDING_BLOCKS, DECORATIONS, TRANSPORTATION -> CraftingBookCategory.BUILDING;
                case REDSTONE -> CraftingBookCategory.REDSTONE;
                case TOOLS, COMBAT -> CraftingBookCategory.EQUIPMENT;
                case FOOD, BREWING, MISC -> CraftingBookCategory.MISC;
            };
        }

        /**
         * <h2>序列化配方数据到 JSON</h2>
         *
         * <p>
         * 覆盖父类方法，写入标准的 pattern、key、result 字段，
         * 并在 result 对象中附加 NBT 数据（如果存在）。
         * </p>
         *
         * <p>
         * NBT 序列化使用 {@link CompoundTag#toString()} 获取 SNBT 格式字符串，
         * 该格式可被 Forge 的配方解析器（{@code CraftingHelper} / {@code ShapedRecipe}）
         * 正确反序列化。
         * </p>
         *
         * @param json 待填充的配方 JSON 根对象
         */
        @Override
        public void serializeRecipeData(@NotNull JsonObject json) {
            super.serializeRecipeData(json);

            // 写入分组（仅当非空时）
            if (!this.group.isEmpty()) {
                json.addProperty("group", this.group);
            }

            // 写入 pattern
            JsonArray patternArray = new JsonArray();
            for (String row : this.pattern) {
                patternArray.add(row);
            }
            json.add("pattern", patternArray);

            // 写入 key
            JsonObject keyObject = new JsonObject();
            for (Map.Entry<Character, Ingredient> entry : this.key.entrySet()) {
                keyObject.add(String.valueOf(entry.getKey()), entry.getValue().toJson());
            }
            json.add("key", keyObject);

            // 写入 result（包含 NBT 数据）
            JsonObject resultObject = new JsonObject();
            resultObject.addProperty("item",
                    Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(this.resultStack.getItem())).toString());

            if (this.resultStack.getCount() > 1) {
                resultObject.addProperty("count", this.resultStack.getCount());
            }

            // 关键：如果 ItemStack 携带 NBT 数据，序列化到 "nbt" 字段
            if (this.resultStack.hasTag()) {
                CompoundTag tag = this.resultStack.getTag();
                if (tag != null && !tag.isEmpty()) {
                    // 使用 CompoundTag.toString() 获取 SNBT 格式字符串
                    // 例如: {Damage:0,display:{Name:'{"text":"神剑"}'}}
                    resultObject.addProperty("nbt", tag.toString());
                }
            }

            json.add("result", resultObject);

            // 写入 show_notification
            json.addProperty("show_notification", this.showNotification);
        }

        /**
         * <h2>获取配方序列化器类型</h2>
         * <p>
         * 返回 {@link RecipeSerializer#SHAPED_RECIPE}，因为本质上这仍然是
         * 标准的有序合成配方（{@code minecraft:crafting_shaped}），
         * NBT 只是附加在 result 输出上。
         * </p>
         *
         * @return 标准有序合成配方的序列化器
         */
        @Override
        public @NotNull RecipeSerializer<?> getType() {
            return RecipeSerializer.SHAPED_RECIPE;
        }

        /**
         * <h2>获取配方 ID</h2>
         *
         * @return 配方的唯一 ResourceLocation
         */
        @Override
        public @NotNull ResourceLocation getId() {
            return this.id;
        }

        /**
         * <h2>序列化进度数据</h2>
         *
         * @return 序列化后的进度 JSON，或 {@code null}
         */
        @Override
        @Nullable
        public JsonObject serializeAdvancement() {
            return this.advancement.serializeToJson();
        }

        /**
         * <h2>获取进度 ID</h2>
         *
         * @return 进度文件的 ResourceLocation
         */
        @Override
        @Nullable
        public ResourceLocation getAdvancementId() {
            return this.advancementId;
        }
    }
}
