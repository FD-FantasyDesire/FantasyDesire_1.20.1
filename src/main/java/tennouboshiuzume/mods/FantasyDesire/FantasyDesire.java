package tennouboshiuzume.mods.FantasyDesire;

import com.google.common.io.Closer;
import com.mojang.logging.LogUtils;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.item.ItemTierSlashBlade;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;
import tennouboshiuzume.mods.FantasyDesire.event.CapabilityAttachHandler;
import tennouboshiuzume.mods.FantasyDesire.items.CapabilityFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.items.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.registry.ModItems;
import tennouboshiuzume.mods.FantasyDesire.registry.creativetab.FdTab;

import static net.minecraftforge.versions.forge.ForgeVersion.MOD_ID;

@Mod(FantasyDesire.MODID)
public class FantasyDesire {
    public static final String MODID = "fantasydesire";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation prefix(String path) {
        return new ResourceLocation(MOD_ID,path);
    }

    public FantasyDesire() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        // 注册物品
        ModItems.register(eventBus);

        // 注册创造模式物品栏
        FdTab.register(eventBus);
    }
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void register(RegisterEvent event) {
            event.register(ForgeRegistries.Keys.ITEMS, helper -> {
                helper.register(new ResourceLocation(MODID, "fantasyslashblade"), new ItemFantasySlashBlade(new ItemTierSlashBlade(40, 4F), 4, -2.4F, (new Item.Properties())));
            });
        }
    }

    @SubscribeEvent
    public static void onRegisterCapability(final RegisterCapabilitiesEvent event) {
        CapabilityFantasySlashBlade.register(event);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
//        MinecraftForge.EVENT_BUS.register(new CapabilityAttachHandler());
    }

    private void clientSetup(final FMLClientSetupEvent event) {
    }
}
