package tennouboshiuzume.mods.FantasyDesire.entity;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;

public class EntityFDSpearPhantomSword extends EntityFDPhantomSword {
    public EntityFDSpearPhantomSword(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    public ResourceLocation getModelLoc() {
        return new ResourceLocation("fantasydesire", "models/util/spear.obj");
    }

    @Override
    public void customEffectFired() {
        // Stabed Entity
        if (this.getHitEntity() != null && this.getOwner() != null
                && this.tickCount % 3 == 0 && getHitEntity() instanceof LivingEntity tarEntity) {
            Level level = this.level();
            if (!level.isClientSide()) {
                double theta = this.random.nextDouble() * 2 * Math.PI;
                double phi = Math.acos(2 * this.random.nextDouble() - 1);
                double radius = 24.0;
                double targetX = tarEntity.getX();
                double targetY = tarEntity.getY() + tarEntity.getBbHeight() / 2.0;
                double targetZ = tarEntity.getZ();
                double x = targetX + radius * Math.sin(phi) * Math.cos(theta);
                double y = targetY + radius * Math.cos(phi);
                double z = targetZ + radius * Math.sin(phi) * Math.sin(theta);
                Vec3 lookVec = tarEntity.position().add(0, tarEntity.getBbHeight() / 2, 0).subtract(new Vec3(x, y, z))
                        .normalize();
                // float lookYaw = (float) (Math.atan2(-lookVec.x, lookVec.z) * (180f /
                // Math.PI));
                // float lookPitch = (float) (Math.asin(-lookVec.y) * (180f / Math.PI));
                float yawRad = (float) Math.atan2(-lookVec.x, lookVec.z);
                float pitchRad = (float) Math.asin(-lookVec.y);
                Quaternionf qBase = new Quaternionf().rotationY(-yawRad).rotateX(pitchRad);
                Quaternionf qLocalY = new Quaternionf().rotationY((float) Math.PI / 2.0f);
                qBase.mul(qLocalY);
                Vector3f newForward = qBase.transform(new Vector3f(0.0f, 0.0f, 1.0f));
                float finalYaw = (float) Math.toDegrees(Math.atan2(-newForward.x, newForward.z));
                float finalPitch = (float) Math.toDegrees(Math.asin(-newForward.y));

                EntityFDSoulPhantomSword sword = new EntityFDSoulPhantomSword(
                        FDEntitys.FDPhantomSword.get(), level);
                sword.setOwner(this.getOwner());
                sword.setDamage(this.getDamage() / 20);
                sword.setSpeed(2f);
                sword.setPos(x, y, z);
                sword.setColor(this.getColor());
                sword.setRoll(this.getRoll());
                sword.setScale(0.2f);
                sword.setMovingMode(EntityFDPhantomSword.MovingMode.SEEK);
                sword.setStandbyMode(EntityFDPhantomSword.StandbyMode.WORLD);
                sword.setDelay(100);
                sword.setSeekAngle(6);
                sword.setHasTail(true);
                sword.setDelayTicks(0);
                sword.setTargetId(tarEntity.getId());
                sword.setNoClip(true);
                sword.setStandbyYawPitch(finalYaw, finalPitch);
                sword.tryInit();
                level.addFreshEntity(sword);
            }
        }
    }
}