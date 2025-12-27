package tennouboshiuzume.mods.FantasyDesire.init;

import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.specialattack.FDSlashArts;

public class FDSpecialAttacks {
        public static final DeferredRegister<SlashArts> FD_SLASH_ARTS = DeferredRegister.create(SlashArts.REGISTRY_KEY,
                        FantasyDesire.MODID);

        public static final RegistryObject<SlashArts> WING_TO_THE_FUTURE = FD_SLASH_ARTS.register("wing_to_the_future",
                        () -> new FDSlashArts((e) -> FDCombo.WING_TO_THE_FUTURE.getId(), 4, true));
        public static final RegistryObject<SlashArts> RAINBOW_STAR = FD_SLASH_ARTS.register("rainbow_star",
                        () -> new FDSlashArts((e) -> FDCombo.RAINBOW_STAR.getId(), 1, true));
        public static final RegistryObject<SlashArts> CRIMSON_STRIKE = FD_SLASH_ARTS.register("crimson_strike",
                        () -> new FDSlashArts((e) -> FDCombo.WING_TO_THE_FUTURE.getId(), 1));
        public static final RegistryObject<SlashArts> TWIN_SYSTEM_L = FD_SLASH_ARTS.register("twin_system_l",
                        () -> new FDSlashArts((e) -> FDCombo.MOOD_SLASH.getId(), 4));
        public static final RegistryObject<SlashArts> TWIN_SYSTEM_R = FD_SLASH_ARTS.register("twin_system_r",
                        () -> new FDSlashArts((e) -> FDCombo.DOOM_SLASH.getId(), 4));
        public static final RegistryObject<SlashArts> CHARGE_SHOT = FD_SLASH_ARTS.register("charge_shot",
                        () -> new FDSlashArts((e) -> FDCombo.WING_TO_THE_FUTURE.getId(), 2));
        public static final RegistryObject<SlashArts> OVER_CHARGE = FD_SLASH_ARTS.register("over_charge",
                        () -> new FDSlashArts((e) -> FDCombo.WING_TO_THE_FUTURE.getId(), 2));
        public static final RegistryObject<SlashArts> ECHOING_VOID = FD_SLASH_ARTS.register("echoing_void",
                        () -> new FDSlashArts((e) -> FDCombo.ECHOING_VOID.getId(), 1));
        public static final RegistryObject<SlashArts> FREEZE_ZERO = FD_SLASH_ARTS.register("freeze_zero",
                        () -> new FDSlashArts((e) -> FDCombo.WING_TO_THE_FUTURE.getId(), 4));
        public static final RegistryObject<SlashArts> CHROMITE_COMET = FD_SLASH_ARTS.register("chromite_comet",
                        () -> new FDSlashArts((e) -> FDCombo.WING_TO_THE_FUTURE.getId(), 1));
}
