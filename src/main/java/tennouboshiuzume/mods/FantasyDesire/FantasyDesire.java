package tennouboshiuzume.mods.FantasyDesire;

import com.mojang.logging.LogUtils;
import mods.flammpfeil.slashblade.item.ItemTierSlashBlade;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;
import tennouboshiuzume.mods.FantasyDesire.data.FantasySlashBladeDefinition;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.CapabilityFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.registry.creativetab.FdTab;
import tennouboshiuzume.mods.FantasyDesire.specialeffect.idletest;

@Mod(FantasyDesire.MODID)
public class FantasyDesire {
    public static final String MODID = "fantasydesire";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation prefix(String path) {
        return new ResourceLocation(MODID,path);
    }

    public FantasyDesire() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
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
        MinecraftForge.EVENT_BUS.register(new idletest());
    }

//    @SubscribeEvent
//    public static void Baked(final ModelEvent.ModifyBakingResult event) {
//        bakeBlade(FDItems.fantasyslashblade, event);
//    }
//
//    public static void bakeBlade(Item blade, final ModelEvent.ModifyBakingResult event) {
//        ModelResourceLocation loc = new ModelResourceLocation(ForgeRegistries.ITEMS.getKey(blade), "inventory");
//        BladeModel model = new BladeModel(event.getModels().get(loc), event.getModelBakery());
//        event.getModels().put(loc, model);
//    }

    private void clientSetup(final FMLClientSetupEvent event) {
    }

    public static HolderLookup.RegistryLookup<FantasySlashBladeDefinition> getFantasySlashBladeDefinitionRegistry(HolderLookup.Provider access) {
        return access.lookupOrThrow(FantasySlashBladeDefinition.REGISTRY_KEY);
    }
}
