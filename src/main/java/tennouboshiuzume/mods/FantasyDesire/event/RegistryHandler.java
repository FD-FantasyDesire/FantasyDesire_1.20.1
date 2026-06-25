package tennouboshiuzume.mods.FantasyDesire.event;

import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DataPackRegistryEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.data.FantasySlashBladeDefinition;
import tennouboshiuzume.mods.FantasyDesire.recipe.FantasySlashBladeIngredient;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RegistryHandler {
    @SubscribeEvent
    public static void onDatapackRegister(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(
                FantasySlashBladeDefinition.REGISTRY_KEY,
                FantasySlashBladeDefinition.CODEC,
                FantasySlashBladeDefinition.CODEC);
    }

    @SubscribeEvent
    public static void registerSerializers(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.RECIPE_SERIALIZERS, helper -> CraftingHelper
                .register(FantasyDesire.prefix("fantasy_blade"),
                        FantasySlashBladeIngredient.Serializer.INSTANCE));
    }
}
