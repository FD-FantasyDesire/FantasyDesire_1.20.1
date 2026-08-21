package tennouboshiuzume.mods.FantasyDesire.specialeffects;

import mods.flammpfeil.slashblade.registry.SpecialEffectsRegistry;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;

public class FDSpecialEffectBase extends SpecialEffect {
    private final boolean hasAltName;

    public FDSpecialEffectBase(int requestLevel, boolean isCopiable, boolean isRemovable) {
        this(requestLevel, isCopiable, isRemovable, false);
    }

    public FDSpecialEffectBase(int requestLevel, boolean isCopiable, boolean isRemovable, boolean hasAltName) {
        super(requestLevel, isCopiable, isRemovable);
        this.hasAltName = hasAltName;
    }

    public boolean hasAltName() {
        return this.hasAltName;
    }

    public static boolean hasAltName(ResourceLocation id) {
        return ((FDSpecialEffectBase) ((IForgeRegistry) SpecialEffectsRegistry.REGISTRY.get()).getValue(id)).hasAltName();
    }
}
