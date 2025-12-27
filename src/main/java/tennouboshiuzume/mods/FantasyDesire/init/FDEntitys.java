package tennouboshiuzume.mods.FantasyDesire.init;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.entity.*;

public class FDEntitys {
        public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister
                        .create(ForgeRegistries.ENTITY_TYPES, FantasyDesire.MODID);

        public static final RegistryObject<EntityType<EntityFDPhantomSword>> FDPhantomSword = ENTITY_TYPES.register(
                        "fd_phantom_sword",
                        () -> EntityType.Builder.<EntityFDPhantomSword>of(EntityFDPhantomSword::new, MobCategory.MISC)
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(8)
                                        .updateInterval(1)
                                        .build("fd_phantom_sword"));
        public static final RegistryObject<EntityType<EntityFDDriveEx>> FDDriveEx = ENTITY_TYPES.register("fd_drive_ex",
                        () -> EntityType.Builder.<EntityFDDriveEx>of(EntityFDDriveEx::new, MobCategory.MISC)
                                        .sized(3.0F, 3.0F)
                                        .clientTrackingRange(8)
                                        .updateInterval(1)
                                        .build("fd_drive_ex"));
        public static final RegistryObject<EntityType<EntityFDRainbowPhantomSword>> FDRainbowPhantomSword = ENTITY_TYPES
                        .register("fd_rainbow_phantom_sword",
                                        () -> EntityType.Builder
                                                        .<EntityFDRainbowPhantomSword>of(
                                                                        EntityFDRainbowPhantomSword::new,
                                                                        MobCategory.MISC)
                                                        .sized(0.5F, 0.5F)
                                                        .clientTrackingRange(8)
                                                        .updateInterval(1)
                                                        .build("fd_rainbow_phantom_sword"));
        public static final RegistryObject<EntityType<EntityFDEnergyBullet>> FDEnergyBullet = ENTITY_TYPES.register(
                        "fd_energy_bullet",
                        () -> EntityType.Builder.<EntityFDEnergyBullet>of(EntityFDEnergyBullet::new, MobCategory.MISC)
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(8)
                                        .updateInterval(1)
                                        .build("fd_energy_bullet"));
        public static final RegistryObject<EntityType<EntityFDBFG>> FDBFG = ENTITY_TYPES.register("fd_bfg",
                        () -> EntityType.Builder.<EntityFDBFG>of(EntityFDBFG::new, MobCategory.MISC)
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(8)
                                        .updateInterval(1)
                                        .build("fd_bfg"));
        public static final RegistryObject<EntityType<EntityRefinedMissile>> RefinedMissile = ENTITY_TYPES.register(
                        "refined_missile",
                        () -> EntityType.Builder.<EntityRefinedMissile>of(EntityRefinedMissile::new, MobCategory.MISC)
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(8)
                                        .updateInterval(1)
                                        .build("refined_missile"));
        public static final RegistryObject<EntityType<EntityFDHuntSword>> FDHuntSword = ENTITY_TYPES.register(
                        "fd_hunt_sword",
                        () -> EntityType.Builder.<EntityFDHuntSword>of(EntityFDHuntSword::new, MobCategory.MISC)
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(8)
                                        .updateInterval(1)
                                        .build("fd_hunt_sword"));

        public static final RegistryObject<EntityType<EntityFDSlashEffect>> FDSlashEffect = ENTITY_TYPES.register(
                        "fd_slash_effect",
                        () -> EntityType.Builder.<EntityFDSlashEffect>of(EntityFDSlashEffect::new, MobCategory.MISC)
                                        .sized(3.0F, 3.0F)
                                        .clientTrackingRange(4)
                                        .updateInterval(20)
                                        .build("fd_slash_effect"));
        public static final RegistryObject<EntityType<EntityEnderSlashEffect>> EnderSlashEffect = ENTITY_TYPES.register(
                        "ender_slash_effect",
                        () -> EntityType.Builder
                                        .<EntityEnderSlashEffect>of(EntityEnderSlashEffect::new, MobCategory.MISC)
                                        .sized(3.0F, 3.0F)
                                        .clientTrackingRange(4)
                                        .updateInterval(20)
                                        .build("ender_slash_effect"));

        public static void register(IEventBus eventBus) {
                ENTITY_TYPES.register(eventBus);
        }
}
