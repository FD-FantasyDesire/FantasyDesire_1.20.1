package tennouboshiuzume.mods.FantasyDesire.init;

import cn.mmf.slashblade_addon.registry.SBAComboStateRegistry;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;

public class FDSpecialAttacks {
    public static final DeferredRegister<SlashArts> SLASH_ARTS = DeferredRegister.create(SlashArts.REGISTRY_KEY,
            FantasyDesire.MODID);

    public static final RegistryObject<SlashArts> WING_TO_THE_FUTURE = SLASH_ARTS.register("wing_to_the_future",
            () -> new SlashArts((e) -> FDCombo.WING_TO_THE_FUTURE.getId()));
}
