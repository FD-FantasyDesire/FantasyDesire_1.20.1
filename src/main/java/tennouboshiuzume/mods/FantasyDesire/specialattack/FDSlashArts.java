package tennouboshiuzume.mods.FantasyDesire.specialattack;

import mods.flammpfeil.slashblade.registry.SlashArtsRegistry;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.IForgeRegistry;
import tennouboshiuzume.mods.FantasyDesire.specialeffect.FDSpecialEffectBase;

import java.util.function.Function;

public class FDSlashArts extends SlashArts {
    private final int descColumn;
    public FDSlashArts(Function<LivingEntity, ResourceLocation> state,int descolumn) {
        super(state);
        this.descColumn = descolumn;
    }
    public int getDescColumn(){
        return this.descColumn;
    }

    public static int getDescColumn(ResourceLocation id) {
        return ((FDSpecialEffectBase)((IForgeRegistry) SlashArtsRegistry.REGISTRY.get()).getValue(id)).getDescColumn();
    }
}
