package tennouboshiuzume.mods.FantasyDesire.data;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.damagesource.FDDamageSource;
import tennouboshiuzume.mods.FantasyDesire.damagesource.ModDamageTypeTagsProvider;
import tennouboshiuzume.mods.FantasyDesire.data.builtin.FantasySlashBladeBuiltInRegistry;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGen {
        @SubscribeEvent
        public static void dataGen(final GatherDataEvent event) {
                System.out.println("==== DataGen 已触发 ====");
                DataGenerator dataGenerator = event.getGenerator();
                CompletableFuture<Provider> lookupProvider = event.getLookupProvider();
                PackOutput packOutput = dataGenerator.getPackOutput();
                ExistingFileHelper helper = event.getExistingFileHelper();
                // 注册拔刀剑和伤害
                final RegistrySetBuilder fantasybladeBuilder = new RegistrySetBuilder()
                                .add(FantasySlashBladeDefinition.REGISTRY_KEY,
                                                FantasySlashBladeBuiltInRegistry::registerAll)
                                .add(Registries.DAMAGE_TYPE, FDDamageSource::bootstrap);
                DatapackBuiltinEntriesProvider entriesProvider = new DatapackBuiltinEntriesProvider(packOutput,
                                lookupProvider, fantasybladeBuilder, Set.of(FantasyDesire.MODID));
                dataGenerator.addProvider(event.includeServer(), entriesProvider);
                dataGenerator.addProvider(event.includeServer(),
                                new ModDamageTypeTagsProvider(packOutput, entriesProvider.getRegistryProvider(),
                                                helper));
                dataGenerator.addProvider(event.includeServer(), new FantasySlashBladeRecipeProvider(packOutput));
                System.out.println("==== DataGen 已完成 ====");
        }

}