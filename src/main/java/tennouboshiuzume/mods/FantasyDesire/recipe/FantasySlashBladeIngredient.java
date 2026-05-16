package tennouboshiuzume.mods.FantasyDesire.recipe;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import tennouboshiuzume.mods.FantasyDesire.init.FDItemsRegistry;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FantasySlashBladeIngredient extends Ingredient {
    private final Set<Item> items;
    private final FDRequestDefinition request;

    private ItemStack[] itemStacks;

    protected FantasySlashBladeIngredient(Set<Item> items, FDRequestDefinition request) {
        super(items.stream().map(item -> {
            ItemStack stack = new ItemStack(item);
            request.initItemStack(stack);
            return new Ingredient.ItemValue(stack);
        }));
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Cannot create a FantasySlashBladeIngredient with no items");
        }
        this.items = Collections.unmodifiableSet(items);
        this.request = request;
    }

    @Override
    public ItemStack[] getItems() {
        if (this.itemStacks == null) {
            ItemStack[] superItems = super.getItems();
            this.itemStacks = new ItemStack[superItems.length];
            for (int i = 0; i < superItems.length; i++) {
                ItemStack stack = superItems[i].copy();
                ResourceLocation name = this.request.name();
                if (!name.equals(mods.flammpfeil.slashblade.SlashBlade.prefix("none"))) {
                    tennouboshiuzume.mods.FantasyDesire.data.FantasySlashBladeDefinition def = null;
                    try {
                        if (net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() != null) {
                            def = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().registryAccess()
                                    .registryOrThrow(
                                            tennouboshiuzume.mods.FantasyDesire.data.FantasySlashBladeDefinition.REGISTRY_KEY)
                                    .get(name);
                        } else if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
                            def = net.minecraft.client.Minecraft.getInstance().getConnection().registryAccess()
                                    .registryOrThrow(
                                            tennouboshiuzume.mods.FantasyDesire.data.FantasySlashBladeDefinition.REGISTRY_KEY)
                                    .get(name);
                        }
                    } catch (Exception e) {
                    }

                    if (def != null) {
                        stack = def.getBlade().copy();
                        ItemStack finalStack = stack;
                        // Only apply counts/enchants/types from request, do not overwrite translation
                        // key and fdState
                        var state = finalStack.getCapability(mods.flammpfeil.slashblade.item.ItemSlashBlade.BLADESTATE)
                                .orElse(null);
                        if (state != null) {
                            if (this.request.proudSoulCount() > 0)
                                state.setProudSoulCount(this.request.proudSoulCount());
                            if (this.request.killCount() > 0)
                                state.setKillCount(this.request.killCount());
                            if (this.request.refineCount() > 0)
                                state.setRefine(this.request.refineCount());
                            this.request.enchantments().forEach(enchantment -> {
                                var ench = net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS
                                        .getValue(enchantment.getEnchantmentID());
                                if (ench != null) {
                                    finalStack.enchant(ench, enchantment.getEnchantmentLevel());
                                }
                            });
                            this.request.defaultType().forEach(type -> {
                                switch (type) {
                                    case BEWITCHED -> state.setDefaultBewitched(true);
                                    case BROKEN -> {
                                        finalStack.setDamageValue(finalStack.getMaxDamage() - 1);
                                        state.setBroken(true);
                                    }
                                    case SEALED -> state.setSealed(true);
                                    default -> {
                                    }
                                }
                            });
                            finalStack.getOrCreateTag().put("bladeState", state.serializeNBT());
                        }
                    } else {
                        // Fallback to standard init if definition is not found
                        this.request.initItemStack(stack);
                    }
                }
                this.itemStacks[i] = stack;
            }
        }
        return this.itemStacks;
    }

    public static FantasySlashBladeIngredient of(ItemLike item, FDRequestDefinition request) {
        return new FantasySlashBladeIngredient(Set.of(item.asItem()), request);
    }

    public static FantasySlashBladeIngredient of(FDRequestDefinition request) {
        return new FantasySlashBladeIngredient(Set.of(FDItemsRegistry.FANTASY_SLASHBLADE.get()), request);
    }

    public static FantasySlashBladeIngredient of(ItemLike item, ResourceLocation request) {
        return new FantasySlashBladeIngredient(Set.of(item.asItem()),
                FDRequestDefinition.Builder.newInstance().name(request).build());
    }

    public static FantasySlashBladeIngredient of(ResourceLocation request) {
        return new FantasySlashBladeIngredient(Set.of(FDItemsRegistry.FANTASY_SLASHBLADE.get()),
                FDRequestDefinition.Builder.newInstance().name(request).build());
    }

    public static FantasySlashBladeIngredient of(Consumer<FDRequestDefinition.Builder> configurer) {
        FDRequestDefinition.Builder builder = FDRequestDefinition.Builder.newInstance();
        configurer.accept(builder);
        return new FantasySlashBladeIngredient(Set.of(FDItemsRegistry.FANTASY_SLASHBLADE.get()), builder.build());
    }

    public static FantasySlashBladeIngredient blankNameless() {
        return of(FDRequestDefinition.Builder.newInstance().build());
    }

    public static FantasySlashBladeIngredient blank() {
        return blankNameless();
    }

    @Override
    public boolean test(ItemStack input) {
        if (input == null) {
            return false;
        }
        return items.contains(input.getItem()) && this.request.test(input);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public @NotNull IIngredientSerializer<? extends Ingredient> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public @NotNull JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", Objects.requireNonNull(CraftingHelper.getID(Serializer.INSTANCE)).toString());
        if (items.size() == 1) {
            json.addProperty("item",
                    Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(items.iterator().next())).toString());
        } else {
            JsonArray itemsArray = new JsonArray();
            this.items.stream().map(ForgeRegistries.ITEMS::getKey).sorted()
                    .forEach(name -> itemsArray.add(name.toString()));
            json.add("items", itemsArray);
        }
        json.add("request", this.request.toJson());
        return json;
    }

    public static class Serializer implements IIngredientSerializer<FantasySlashBladeIngredient> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public @NotNull FantasySlashBladeIngredient parse(FriendlyByteBuf buffer) {
            Set<Item> items = Stream.generate(() -> buffer.readRegistryIdUnsafe(ForgeRegistries.ITEMS))
                    .limit(buffer.readVarInt()).collect(Collectors.toSet());
            FDRequestDefinition request = FDRequestDefinition.fromNetwork(buffer);
            return new FantasySlashBladeIngredient(items, request);
        }

        @Override
        public @NotNull FantasySlashBladeIngredient parse(JsonObject json) {
            Set<Item> items;
            if (json.has("item")) {
                items = Set.of(CraftingHelper.getItem(GsonHelper.getAsString(json, "item"), true));
            } else if (json.has("items")) {
                ImmutableSet.Builder<Item> builder = ImmutableSet.builder();
                JsonArray itemArray = GsonHelper.getAsJsonArray(json, "items");
                for (int i = 0; i < itemArray.size(); i++) {
                    builder.add(CraftingHelper.getItem(GsonHelper.convertToString(itemArray.get(i), "items[" + i + ']'),
                            true));
                }
                items = builder.build();
            } else {
                throw new JsonSyntaxException("Must set either 'item' or 'items'");
            }
            var request = FDRequestDefinition.fromJSON(json.getAsJsonObject("request"));
            return new FantasySlashBladeIngredient(items, request);
        }

        @Override
        public void write(FriendlyByteBuf buffer, FantasySlashBladeIngredient ingredient) {
            buffer.writeVarInt(ingredient.items.size());
            for (Item item : ingredient.items) {
                buffer.writeRegistryIdUnsafe(ForgeRegistries.ITEMS, item);
            }
            ingredient.request.toNetwork(buffer);
        }
    }
}
