package tennouboshiuzume.mods.FantasyDesire.data;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.data.builtin.FantasySlashBladeBuiltInRegistry;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGen {
    @SubscribeEvent
    public static void dataGen(GatherDataEvent event) {
        System.out.println("==== DataGen 已触发 ====");
        System.out.println("==== DataGen includeServer: " + event.includeServer() + " ====");
        DataGenerator dataGenerator = event.getGenerator();
        CompletableFuture<Provider> lookupProvider = event.getLookupProvider();
        PackOutput packOutput = dataGenerator.getPackOutput();
        final RegistrySetBuilder fantasybladeBuilder = new RegistrySetBuilder().add(FantasySlashBladeDefinition.REGISTRY_KEY,
                FantasySlashBladeBuiltInRegistry::registerAll);
        dataGenerator.addProvider(event.includeServer(),
                new DatapackBuiltinEntriesProvider(packOutput, lookupProvider, fantasybladeBuilder, Set.of(FantasyDesire.MODID)));
        System.out.println("==== DataGen 已完成 ====");
    }

}