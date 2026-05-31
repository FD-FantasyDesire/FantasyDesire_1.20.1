package tennouboshiuzume.mods.FantasyDesire.slasharts;

import net.minecraft.world.entity.LivingEntity;
import tennouboshiuzume.mods.FantasyDesire.utils.CapabilityUtils;

public class CrimsonStrike {
    public static boolean AntiNTR(LivingEntity entity) {
        return CapabilityUtils.SEConditionMatcher.of(entity)
                .requireTranslation("item.fantasydesire.crimson_scythe")
                .match() != null;
    }
}
