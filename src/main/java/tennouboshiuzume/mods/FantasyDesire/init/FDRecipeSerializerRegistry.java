package tennouboshiuzume.mods.FantasyDesire.init;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.recipe.FantasySlashBladeShapedRecipe;

public class FDRecipeSerializerRegistry {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZER = DeferredRegister
            .create(ForgeRegistries.RECIPE_SERIALIZERS, FantasyDesire.MODID);

    public static final RegistryObject<RecipeSerializer<?>> FANTASY_SLASHBLADE_SHAPED = RECIPE_SERIALIZER
            .register("shaped_fantasy_blade", () -> FantasySlashBladeShapedRecipe.SERIALIZER);

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZER.register(eventBus);
    }
}