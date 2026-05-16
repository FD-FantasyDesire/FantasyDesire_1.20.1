package tennouboshiuzume.mods.FantasyDesire.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.recipe.FantasySlashBladeShapedRecipe;
import tennouboshiuzume.mods.FantasyDesire.recipe.SpecialTransformRecipe;

public class FDRecipeSerializerRegistry {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZER = DeferredRegister
            .create(ForgeRegistries.RECIPE_SERIALIZERS, FantasyDesire.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPE = DeferredRegister
            .create(Registries.RECIPE_TYPE, FantasyDesire.MODID);

    public static final RegistryObject<RecipeSerializer<?>> FANTASY_SLASHBLADE_SHAPED = RECIPE_SERIALIZER
            .register("shaped_fantasy_blade", () -> FantasySlashBladeShapedRecipe.SERIALIZER);

    public static final RegistryObject<RecipeSerializer<?>> SPECIAL_TRANSFORM = RECIPE_SERIALIZER
            .register("special_transform", SpecialTransformRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<SpecialTransformRecipe>> SPECIAL_TRANSFORM_TYPE = RECIPE_TYPE
            .register("special_transform", () -> new RecipeType<SpecialTransformRecipe>() {
                @Override
                public String toString() {
                    return "special_transform";
                }
            });

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZER.register(eventBus);
        RECIPE_TYPE.register(eventBus);
    }
}