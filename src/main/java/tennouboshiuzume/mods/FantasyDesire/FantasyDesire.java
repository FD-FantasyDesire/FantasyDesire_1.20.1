package tennouboshiuzume.mods.FantasyDesire;

import com.google.common.base.CaseFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import tennouboshiuzume.mods.FantasyDesire.client.FDShaderHandler;
import tennouboshiuzume.mods.FantasyDesire.config.FDConfig;
import tennouboshiuzume.mods.FantasyDesire.data.FantasySlashBladeDefinition;
import tennouboshiuzume.mods.FantasyDesire.init.*;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.CapabilityFantasySlashBlade;
import tennouboshiuzume.mods.FantasyDesire.network.FDNetwork;

@Mod(FantasyDesire.MODID)
@SuppressWarnings("removal")
public class FantasyDesire {
    public static final String MODID = "fantasydesire";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation prefix(String path) {
        return new ResourceLocation(MODID, path);
    }

    public FantasyDesire() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, FDConfig.SPEC);
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(this::commonSetup);
        FDEntitys.register(eventBus);
        FDParticles.PARTICLES.register(eventBus);
        FDCombo.FD_COMBO_STATES.register(eventBus);
        FDSlashArtRegistry.FD_SLASH_ARTS.register(eventBus);
        FDSpecialEffectsRegistry.SPECIAL_EFFECT.register(eventBus);
        FDPotionEffects.register(eventBus);
        FDAttributes.register(eventBus);
        FDNetwork.register();
        FDTab.register(eventBus);
        FDRecipeSerializerRegistry.register(eventBus);
        FDItemsRegistry.register(eventBus);
        DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> FDShaderHandler.register(eventBus));
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onRegisterCapability(final RegisterCapabilitiesEvent event) {
            CapabilityFantasySlashBlade.register(event);
            event.register(tennouboshiuzume.mods.FantasyDesire.capability.IEchoDamageCap.class);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    private static String classToString(Class<? extends Entity> entityClass) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entityClass.getSimpleName())
                .replace("entity_", "");
    }

    public static Registry<FantasySlashBladeDefinition> getFantasySlashBladeDefinitionRegistry(Level level) {
        if (level.isClientSide())
            return net.minecraftforge.fml.DistExecutor.unsafeCallWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> tennouboshiuzume.mods.FantasyDesire.client.FDClientRegistryAccess::getRegistry);
        return level.registryAccess().registryOrThrow(FantasySlashBladeDefinition.REGISTRY_KEY);
    }

    public static Registry<FantasySlashBladeDefinition> getClientSlashBladeRegistry() {
        return net.minecraftforge.fml.DistExecutor.unsafeCallWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> tennouboshiuzume.mods.FantasyDesire.client.FDClientRegistryAccess::getRegistry);
    }

    public static HolderLookup.RegistryLookup<FantasySlashBladeDefinition> getFantasySlashBladeDefinitionRegistry(
            HolderLookup.Provider access) {
        return access.lookupOrThrow(FantasySlashBladeDefinition.REGISTRY_KEY);
    }

    public static ItemStack getBladeAsRegistry(Level level, ResourceKey<FantasySlashBladeDefinition> key) {
        if (level.isClientSide()) {
            return getClientSlashBladeRegistry().get(key).getBlade();
        } else {
            return level.registryAccess().registryOrThrow(FantasySlashBladeDefinition.REGISTRY_KEY).get(key).getBlade();
        }
    }
}
