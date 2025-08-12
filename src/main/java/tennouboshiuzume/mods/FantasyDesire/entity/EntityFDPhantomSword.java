package tennouboshiuzume.mods.FantasyDesire.entity;

import com.mojang.math.Axis;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public class EntityFDPhantomSword extends EntityAbstractSummonedSword {

//    发射延迟
    private static final EntityDataAccessor<Integer> DELAY_TICKS = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.INT);
//    大小缩放
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.FLOAT);
//    目标ID
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.INT);
//    待命行为模式：绑定于玩家/绑定于世界 PLAYER/WORLD
    private static final EntityDataAccessor<String> STANDBY_MODE = SynchedEntityData.defineId(EntityFDPhantomSword.class,EntityDataSerializers.STRING);
//    发射后行为模式：追踪/直射 NORMAL/TRACKING
    private static final EntityDataAccessor<String> MOVING_MODE = SynchedEntityData.defineId(EntityFDPhantomSword.class,EntityDataSerializers.STRING);
//    待命固定朝向 (无论是玩家还是世界)
    private static final EntityDataAccessor<Vector3f> STANDBY_VEC = SynchedEntityData.defineId(EntityFDPhantomSword.class,EntityDataSerializers.VECTOR3);
//    飞行粒子
    private static final EntityDataAccessor<ParticleOptions> PARTICLE_TYPES = SynchedEntityData.defineId(EntityFDPhantomSword.class,EntityDataSerializers.PARTICLE);
//    向量偏移量，仅用于绑定玩家时
    private static final EntityDataAccessor<Vector3f> OFFSET = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.VECTOR3);
//    是否已发射
    private static final EntityDataAccessor<Boolean> IT_FIRED = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.BOOLEAN);
//    发射速度
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.FLOAT);

    public EntityFDPhantomSword(EntityType<? extends Projectile> entityTypeIn, Level worldIn)
    {
        super(entityTypeIn, worldIn);
        this.setPierce((byte) 5);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DELAY_TICKS, 0);
        this.entityData.define(SCALE, 1.0f);
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(STANDBY_MODE,"WORLD");
        this.entityData.define(MOVING_MODE, "NORMAL");
        this.entityData.define(STANDBY_VEC, Vec3.ZERO.toVector3f());
        this.entityData.define(PARTICLE_TYPES, null);
        this.entityData.define(OFFSET, Vec3.ZERO.toVector3f());
        this.entityData.define(IT_FIRED , false);
        this.entityData.define(SPEED,3f);
    }

    @Override
    public void tick() {
//        TODO::行为模式
//        测试使用GunBladeEffects.java类测试
//        检定：如果绑定于玩家，则骑乘玩家，适用视角对应的位置修正，类似BlisteringSwords
//        如果绑定于世界，则适用默认方向修正
//        无论如何，当延迟结束，都向剑的指向方向发射
        super.tick();
    }

    @Nullable
    private LivingEntity getTargetEntity() {
        int id = this.entityData.get(TARGET_ID);
        if (id == -1) return null;
        if (this.level().getEntity(id) instanceof LivingEntity target) {
            return target;
        }
        return null;
    }


//  TODO::待补全数据持久化
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Delay", this.entityData.get(DELAY_TICKS));
        tag.putFloat("Scale", this.entityData.get(SCALE));
        tag.putInt("TargetId", this.entityData.get(TARGET_ID));

    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DELAY_TICKS, tag.getInt("Delay"));
        this.entityData.set(SCALE, tag.getFloat("Scale"));
        this.entityData.set(TARGET_ID, tag.getInt("TargetId"));
    }

    protected void faceEntityStandby()
    {
        Vec3 pos = this.getVehicle().position();
        Vec3 offset = this.getOffset();

        if (this.getVehicle() == null)
        {
            doFire();
            return;
        }

        offset = offset.xRot((float) Math.toRadians(-this.getVehicle().getXRot()));
        offset = offset.yRot((float) Math.toRadians(-this.getVehicle().getYRot()));

        pos = pos.add(offset);

        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();

        setPos(pos);
        setRot(-this.getVehicle().getYRot(), -this.getVehicle().getXRot());
    }

    public Vec3 getOffset() {
        return new Vec3(this.getEntityData().get(OFFSET));
    }

    public void doFire() {
        this.getEntityData().set(IT_FIRED, true);
    }

    public boolean itFired() {
        return this.getEntityData().get(IT_FIRED);
    }
}
