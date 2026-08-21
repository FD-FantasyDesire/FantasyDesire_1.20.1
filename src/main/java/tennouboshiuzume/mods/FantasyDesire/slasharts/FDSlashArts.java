package tennouboshiuzume.mods.FantasyDesire.slasharts;

import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Function;

public class FDSlashArts extends SlashArts {
    private final boolean hasAltName;

    public FDSlashArts(Function<LivingEntity, ResourceLocation> state) {
        this(state, false);
    }

    public FDSlashArts(Function<LivingEntity, ResourceLocation> state, boolean hasAltName) {
        super(state);
        this.hasAltName = hasAltName;
    }

    public boolean hasAltName() {
        return this.hasAltName;
    }

}
