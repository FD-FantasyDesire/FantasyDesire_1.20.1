package tennouboshiuzume.mods.FantasyDesire.specialattack;

import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Function;

public class FDSlashArts extends SlashArts {
    private final int descColumn;
    private final boolean hasAltName;

    public FDSlashArts(Function<LivingEntity, ResourceLocation> state, int desColumn) {
        this(state, desColumn, false);
    }

    public FDSlashArts(Function<LivingEntity, ResourceLocation> state, int desColumn, boolean hasAltName) {
        super(state);
        this.descColumn = desColumn;
        this.hasAltName = hasAltName;
    }

    public int getDescColumn() {
        return this.descColumn;
    }

    public boolean hasAltName() {
        return this.hasAltName;
    }

}
