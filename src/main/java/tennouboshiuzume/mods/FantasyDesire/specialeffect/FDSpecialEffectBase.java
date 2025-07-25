package tennouboshiuzume.mods.FantasyDesire.specialeffect;

import mods.flammpfeil.slashblade.registry.SpecialEffectsRegistry;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;

public class FDSpecialEffectBase extends SpecialEffect {
//    用于定义SE行数
    private final int descColumn;
    public FDSpecialEffectBase (int requestLevel, boolean isCopiable, boolean isRemovable,int descolumn){
        super(requestLevel,isCopiable,isRemovable);
        this.descColumn = descolumn;
    }

    public int getDescColumn(){
        return this.descColumn;
    }

    public static int getDescColumn(ResourceLocation id) {
        return ((FDSpecialEffectBase)((IForgeRegistry) SpecialEffectsRegistry.REGISTRY.get()).getValue(id)).getDescColumn();
    }

}
