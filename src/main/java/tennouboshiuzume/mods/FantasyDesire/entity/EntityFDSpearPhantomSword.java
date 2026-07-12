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

    // ========== 十字星型闪光特效 ==========
    /** 闪光剩余 tick（客户端） */
    private int flashTicks = 0;
    /** 标记是否已在发射时触发过闪光 */
    private boolean flashOnFireTriggered = false;

    @Override
    public void tick() {
        super.tick();
        // 客户端闪光计时
        if (this.level().isClientSide()) {
            if (this.flashTicks > 0) {
                this.flashTicks--;
            }
        }
    }

    /**
     * 获取当前闪光剩余 tick
     */
    public int getFlashTicks() {
        return flashTicks;
    }

    @Override
    public ResourceLocation getModelLoc() {
        return new ResourceLocation("fantasydesire", "models/util/spear.obj");
    }

    @Override
    public void customEffectFired() {
        // ===== 客户端闪光效果 =====
        if (this.level().isClientSide()) {
            // 击中目标时触发闪光（一次性）
            if (this.getHitEntity() != null && this.flashTicks <= 0) {
                this.flashTicks = 15;
            }
            // 发射时触发闪光（一次性）
            if (this.getFired() && !flashOnFireTriggered) {
                this.flashTicks = 20;
                this.flashOnFireTriggered = true;
            }
            return;
        }
        if (this.getHitEntity() != null && this.getOwner() != null
                && getHitEntity() instanceof LivingEntity tarEntity) {
            Level level = this.level();
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
            float yawRad = (float) Math.atan2(-lookVec.x, lookVec.z);
            float pitchRad = (float) Math.asin(-lookVec.y);
            Quaternionf qBase = new Quaternionf().rotationY(-yawRad).rotateX(pitchRad);
            Quaternionf qLocalY = new Quaternionf().rotationY((float) Math.PI / 2.0f);
            qBase.mul(qLocalY);
            Vector3f newForward = qBase.transform(new Vector3f(0.0f, 0.0f, 1.0f));
            float finalYaw = (float) Math.toDegrees(Math.atan2(-newForward.x, newForward.z));
            float finalPitch = (float) Math.toDegrees(Math.asin(-newForward.y));
            EntityFDSoulPhantomSword sword = new EntityFDSoulPhantomSword(
                    FDEntitys.FDSoulPhantomSword.get(), level);
            sword.setOwner(this.getOwner());
            sword.setDamage(this.getDamage() / this.getDelay());
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