package tennouboshiuzume.mods.FantasyDesire.init;

import cn.mmf.slashblade_addon.registry.SBAComboStateRegistry;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;

public class FDSpecialAttacks {
    public static final DeferredRegister<SlashArts> SLASH_ARTS = DeferredRegister.create(SlashArts.REGISTRY_KEY,
            FantasyDesire.MODID);

    public static final RegistryObject<SlashArts> WING_TO_THE_FUTURE = SLASH_ARTS.register("wing_to_the_future",
            () -> new SlashArts((e) -> FDCombo.WING_TO_THE_FUTURE.getId()));
    public static final RegistryObject<SlashArts> RAINBOW_STAR = SLASH_ARTS.register("rainbow_star",
            () -> new SlashArts((e) -> FDCombo.RAINBOW_STAR.getId()));
    public static final RegistryObject<SlashArts> CRIMSON_STRIKE = SLASH_ARTS.register("crimson_strike",
            () -> new SlashArts((e) -> FDCombo.WING_TO_THE_FUTURE.getId()));
    public static final RegistryObject<SlashArts> TWIN_SYSTEM_L = SLASH_ARTS.register("twin_system_l",
            () -> new SlashArts((e) -> FDCombo.WING_TO_THE_FUTURE.getId()));
    public static final RegistryObject<SlashArts> TWIN_SYSTEM_R = SLASH_ARTS.register("twin_system_r",
            () -> new SlashArts((e) -> FDCombo.WING_TO_THE_FUTURE.getId()));
    public static final RegistryObject<SlashArts> CHARGE_SHOT = SLASH_ARTS.register("charge_shot",
            () -> new SlashArts((e) -> FDCombo.WING_TO_THE_FUTURE.getId()));
    public static final RegistryObject<SlashArts> OVER_CHARGE = SLASH_ARTS.register("over_charge",
            () -> new SlashArts((e) -> FDCombo.WING_TO_THE_FUTURE.getId()));
    public static final RegistryObject<SlashArts> ECHOING_VOID = SLASH_ARTS.register("echoing_void",
            () -> new SlashArts((e) -> FDCombo.WING_TO_THE_FUTURE.getId()));
    public static final RegistryObject<SlashArts> FREEZE_ZERO = SLASH_ARTS.register("freeze_zero",
            () -> new SlashArts((e) -> FDCombo.WING_TO_THE_FUTURE.getId()));
    public static final RegistryObject<SlashArts> CHROMITE_COMET = SLASH_ARTS.register("chromite_comet",
            () -> new SlashArts((e) -> FDCombo.WING_TO_THE_FUTURE.getId()));
}
