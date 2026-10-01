package tennouboshiuzume.mods.FantasyDesire.entity.phantomsword;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;

import javax.annotation.Nullable;

public class HomingFlight implements SwordFlightBehavior {
    private final boolean guideBySight;

    public HomingFlight(boolean guideBySight) {
        this.guideBySight = guideBySight;
    }

    @Nullable
    @Override
    public Vec3 steer(EntityFDPhantomSword sword) {
        LivingEntity target = sword.getTargetEntity();
        if (target != null && target.isAlive()) {
            return toward(sword, target.position().add(0, target.getEyeHeight() * 0.5, 0),
                    Math.max(sword.getSpeed(), target.getDeltaMovement().length()));
        }
        if (!sword.level().isClientSide() && !sword.hasPendingTarget())
            sword.setTargetId(-1);
        if (!guideBySight || !(sword.getShooter() instanceof LivingEntity shooter))
            return null;
        if (!sword.level().isClientSide() && sword.distanceTo(shooter) >= 16)
            sword.setNoClip(false);
        Vec3 start = shooter.getEyePosition(1.0F);
        Vec3 end = start.add(shooter.getViewVector(1.0F).scale(100));
        HitResult hit = sword.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, shooter));
        return toward(sword, hit.getLocation(), sword.getSpeed());
    }

    protected Vec3 toward(EntityFDPhantomSword sword, Vec3 target, double speed) {
        Vec3 displacement = target.subtract(sword.position());
        if (displacement.lengthSqr() < 1.0E-12)
            return sword.getDeltaMovement();
        double turn = Math.min(Math.toRadians(36),
                Math.toRadians(sword.getSeekAngle() * sword.getDeltaMovement().length()));
        return rotateTowards(sword.getDeltaMovement(), displacement, turn).scale(speed);
    }

    /** 严格限制弧度，尤其处理换目标后的反向向量，避免线性混合抵消为零。 */
    public static Vec3 rotateTowards(Vec3 from, Vec3 to, double maxRadians) {
        Vec3 destination = to.normalize();
        if (destination.lengthSqr() < 1.0E-12)
            return from.normalize();
        Vec3 direction = from.normalize();
        if (direction.lengthSqr() < 1.0E-12)
            return destination;
        if (!Double.isFinite(maxRadians) || maxRadians <= 0)
            return direction;
        double angle = Math.acos(Mth.clamp(direction.dot(destination), -1, 1));
        if (angle <= maxRadians)
            return destination;
        Vec3 axis = direction.cross(destination);
        if (axis.lengthSqr() < 1.0E-12) {
            axis = direction.cross(Math.abs(direction.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0));
        }
        axis = axis.normalize();
        return direction.scale(Math.cos(maxRadians)).add(axis.cross(direction).scale(Math.sin(maxRadians)))
                .add(axis.scale(axis.dot(direction) * (1 - Math.cos(maxRadians)))).normalize();
    }
}
