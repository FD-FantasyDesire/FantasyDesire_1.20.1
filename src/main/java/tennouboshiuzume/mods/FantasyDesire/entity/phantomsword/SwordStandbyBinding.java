package tennouboshiuzume.mods.FantasyDesire.entity.phantomsword;

import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;

import javax.annotation.Nullable;

public interface SwordStandbyBinding {
    /** 只计算待命姿态，移动、瞄准和发射仍由实体统一调度。 */
    @Nullable
    Pose resolve(EntityFDPhantomSword sword);

    default boolean autoLaunch() {
        return true;
    }

    record Pose(Vec3 position, Vec3 motion, float yaw, float pitch, boolean followsRotation) {}
}
