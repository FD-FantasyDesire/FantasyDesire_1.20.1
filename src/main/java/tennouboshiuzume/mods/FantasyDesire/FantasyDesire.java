package tennouboshiuzume.mods.FantasyDesire;

import com.google.common.base.CaseFormat;
import com.mojang.logging.LogUtils;
import mods.flammpfeil.slashblade.item.ItemTierSlashBlade;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;
import tennouboshiuzume.mods.FantasyDesire.data.FantasySlashBladeDefinition;
import tennouboshiuzume.mods.FantasyDesire.init.*;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.CapabilityFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;
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
        FDEntitys.register(eventBus);
        FDCombo.COMBO_STATES.register(eventBus);
        FDSpecialAttacks.SLASH_ARTS.register(eventBus);
        FDSpecialEffects.SPECIAL_EFFECT.register(eventBus);
        FDPotionEffects.register(eventBus);
        FDTab.register(eventBus);
    }
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
//        public static final ResourceLocation FDSummonSwordBaseLoc = new ResourceLocation(FantasyDesire.MODID,
//                classToString(EntityFDSummonSwordBase.class));
//        public static EntityType<EntityFDSummonSwordBase>  FDSummonSwordBase;
        @SubscribeEvent
        public static void register(RegisterEvent event) {

            event.register(ForgeRegistries.Keys.ITEMS, helper -> {
                helper.register(new ResourceLocation(MODID, "fantasyslashblade"), new ItemFantasySlashBlade(new ItemTierSlashBlade(40, 4F), 4, -2.4F, (new Item.Properties())));
            });
        }

        @SubscribeEvent
        public static void onRegisterCapability(final RegisterCapabilitiesEvent event) {
            CapabilityFantasySlashBlade.register(event);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
//            event.registerEntityRenderer(RegistryEvents.FDSummonSwordBase, FDSummonSwordRenderer::new);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
//        MinecraftForge.EVENT_BUS.register(new CapabilityAttachHandler());
        MinecraftForge.EVENT_BUS.register(new idletest());
    }


    private static String classToString(Class<? extends Entity> entityClass) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entityClass.getSimpleName())
                .replace("entity_", "");
    }


    public static Registry<FantasySlashBladeDefinition> getFantasySlashBladeDefinitionRegistry(Level level) {
        if (level.isClientSide())
            return getClientSlashBladeRegistry();
        return level.registryAccess().registryOrThrow(FantasySlashBladeDefinition.REGISTRY_KEY);
    }
    public static Registry<FantasySlashBladeDefinition> getClientSlashBladeRegistry() {
        return Minecraft.getInstance().getConnection().registryAccess().registryOrThrow(FantasySlashBladeDefinition.REGISTRY_KEY);
    }

    public static HolderLookup.RegistryLookup<FantasySlashBladeDefinition> getFantasySlashBladeDefinitionRegistry(HolderLookup.Provider access) {
        return access.lookupOrThrow(FantasySlashBladeDefinition.REGISTRY_KEY);
    }
    public static ItemStack getBladeAsRegistry(Level level,  ResourceKey<FantasySlashBladeDefinition> key){
        if (level.isClientSide()) {
            return getClientSlashBladeRegistry().get(key).getBlade();
        }else {
            return level.registryAccess().registryOrThrow(FantasySlashBladeDefinition.REGISTRY_KEY).get(key).getBlade();
        }
    }
}
