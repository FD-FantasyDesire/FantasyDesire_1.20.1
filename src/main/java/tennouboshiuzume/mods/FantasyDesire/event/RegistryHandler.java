package tennouboshiuzume.mods.FantasyDesire.event;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DataPackRegistryEvent;
import tennouboshiuzume.mods.FantasyDesire.data.FantasySlashBladeDefinition;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RegistryHandler {
    @SubscribeEvent
    public static void onDatapackRegister(DataPackRegistryEvent.NewRegistry event) {
//        System.out.println("注册表键: " + FantasySlashBladeDefinition.REGISTRY_KEY);
        event.dataPackRegistry(
                FantasySlashBladeDefinition.REGISTRY_KEY,
                FantasySlashBladeDefinition.CODEC,
                FantasySlashBladeDefinition.CODEC
        );
    }
}
