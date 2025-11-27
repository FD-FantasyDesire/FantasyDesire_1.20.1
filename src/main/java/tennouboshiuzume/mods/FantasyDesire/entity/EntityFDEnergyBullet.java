package tennouboshiuzume.mods.FantasyDesire.entity;

import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class EntityFDEnergyBullet extends EntityFDDriveEx{
    public EntityFDEnergyBullet(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }


    @Override
    protected void onHitBlock(BlockHitResult blockraytraceresult) {
        if (this.getExpRadius()==0){
            // 获取当前速度
            Vec3 motion = this.getDeltaMovement();

            // 获取碰撞面的方向（如 EAST、UP 等）
            Direction dir = blockraytraceresult.getDirection();

            // 创建弹射效果：沿碰撞方向的分量取反
            double bounceFactor = 0.8; // 衰减比例，想弹性更强就调大一点

            double vx = motion.x;
            double vy = motion.y;
            double vz = motion.z;

            switch (dir) {
                case EAST, WEST -> vx = -vx * bounceFactor;
                case UP, DOWN -> vy = -vy * bounceFactor;
                case NORTH, SOUTH -> vz = -vz * bounceFactor;
            }
            // 设置新的速度
            this.setDeltaMovement(new Vec3(vx, vy, vz));
        }

        if (this.getExpRadius() > 0) {
            this.level().explode(this.getShooter(), this.getX(), this.getY(), this.getZ(), this.getExpRadius(), Level.ExplosionInteraction.NONE);
            this.burst();
        }
    }
}
