package tennouboshiuzume.mods.FantasyDesire.init;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;

public class FDEntitys {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FantasyDesire.MODID);

    public static final RegistryObject<EntityType<EntityFDPhantomSword>> FDPhantomSword = ENTITY_TYPES.register("fd_phantom_sword",
            () -> EntityType.Builder.<EntityFDPhantomSword>of(EntityFDPhantomSword::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("fd_phantom_sword")
    );

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
