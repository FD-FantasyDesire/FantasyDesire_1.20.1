package tennouboshiuzume.mods.FantasyDesire.entity.phantomsword;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;

import javax.annotation.Nullable;

public interface SwordFlightBehavior {
    /** 返回期望速度；null 表示保持现有弹道。只在自由飞行且制导延迟结束后调用。 */
    @Nullable
    Vec3 steer(EntityFDPhantomSword sword);

    default SwordImpactPolicy impacts() {
        return SwordImpactPolicy.LEGACY;
    }

    default CompoundTag save() {
        return new CompoundTag();
    }

    default void load(CompoundTag tag) {}
}
