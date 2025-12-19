package tennouboshiuzume.mods.FantasyDesire.specialeffect;

import mods.flammpfeil.slashblade.registry.SpecialEffectsRegistry;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;

public class FDSpecialEffectBase extends SpecialEffect {
    //    用于定义SE行数
    private final int descColumn;
    private final boolean hasAltName;

    public FDSpecialEffectBase(int requestLevel, boolean isCopiable, boolean isRemovable, int descColumn) {
        this(requestLevel, isCopiable, isRemovable, descColumn, false);
    }

    public FDSpecialEffectBase(int requestLevel, boolean isCopiable, boolean isRemovable, int descColumn, boolean hasAltName) {
        super(requestLevel, isCopiable, isRemovable);
        this.descColumn = descColumn;
        this.hasAltName = hasAltName;
    }

    public int getDescColumn() {
        return this.descColumn;
    }

    public boolean hasAltName() {
        return this.hasAltName;
    }

    public static boolean hasAltName(ResourceLocation id) {
        return ((FDSpecialEffectBase) ((IForgeRegistry) SpecialEffectsRegistry.REGISTRY.get()).getValue(id)).hasAltName();
    }

    public static int getDescColumn(ResourceLocation id) {
        return ((FDSpecialEffectBase) ((IForgeRegistry) SpecialEffectsRegistry.REGISTRY.get()).getValue(id)).getDescColumn();
    }

}
