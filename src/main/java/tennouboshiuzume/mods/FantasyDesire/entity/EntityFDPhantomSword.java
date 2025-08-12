package tennouboshiuzume.mods.FantasyDesire.entity;

import com.mojang.math.Axis;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.entity.IShootable;
import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.RayTraceHelper;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

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
    private static final EntityDataAccessor<String> PARTICLE_TYPES = SynchedEntityData.defineId(EntityFDPhantomSword.class,EntityDataSerializers.STRING);
//    向量偏移量，仅用于绑定玩家时
    private static final EntityDataAccessor<Vector3f> OFFSET = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.VECTOR3);
//    是否已发射
    private static final EntityDataAccessor<Boolean> IT_FIRED = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.BOOLEAN);
//    发射速度
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.FLOAT);

    long fireTime = -1;
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
        this.entityData.define(PARTICLE_TYPES, "Null");
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

        if(STANDBY_MODE.equals("PLAYER")){
            if (!itFired() && getVehicle() == null)
            {
                startRiding(this.getOwner(), true);
            }
        }

        System.out.println(fireTime);
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

    @Override
    public void rideTick()
    {
        if (itFired() && fireTime <= tickCount)
        {
            faceEntityStandby();
            Entity vehicle = getVehicle();
            Vec3 dir = this.getViewVector(0);
            if (!(vehicle instanceof LivingEntity))
            {
                this.shoot(dir.x, dir.y, dir.z, getSpeed(), 1.0f);
                return;
            }

            LivingEntity sender = (LivingEntity) getVehicle();
            this.stopRiding();

            this.tickCount = 0;

            Level worldIn = sender.level();
            Entity lockTarget = null;
            if (sender instanceof LivingEntity)
            {
                lockTarget = sender.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                        .filter(state -> state.getTargetEntity(worldIn) != null)
                        .map(state -> state.getTargetEntity(worldIn)).orElse(null);
            }

            Optional<Entity> foundTarget = Stream
                    .of(Optional.ofNullable(lockTarget),
                            RayTraceHelper
                                    .rayTrace(sender.level(), sender, sender.getEyePosition(1.0f), sender.getLookAngle(), 12, 12, (e) -> true)
                                    .filter(r -> r.getType() == HitResult.Type.ENTITY).filter(r ->
                                    {
                                        EntityHitResult er = (EntityHitResult) r;
                                        Entity target = er.getEntity();

                                        boolean isMatch = true;
                                        if (target instanceof LivingEntity)
                                            isMatch = TargetSelector.test.test(sender, (LivingEntity) target);

                                        if (target instanceof IShootable)
                                            isMatch = ((IShootable) target).getShooter() != sender;

                                        return isMatch;
                                    }).map(r -> ((EntityHitResult) r).getEntity()))
                    .filter(Optional::isPresent).map(Optional::get).findFirst();

            Vec3 targetPos = foundTarget.map((e) -> new Vec3(e.getX(), e.getY() + e.getEyeHeight() * 0.5, e.getZ()))
                    .orElseGet(() ->
                    {
                        Vec3 start = sender.getEyePosition(1.0f);
                        Vec3 end = start.add(sender.getLookAngle().scale(40));
                        HitResult result = worldIn.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sender));
                        return result.getLocation();
                    });

            Vec3 pos = this.getPosition(0.0f);
            dir = targetPos.subtract(pos).normalize();

            this.shoot(dir.x, dir.y, dir.z, getSpeed(), 1.0f);
            if (sender instanceof ServerPlayer)
            {
                ((ServerPlayer) sender).playNotifySound(SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0F, 1.0F);
            }

            return;
        }

        this.setDeltaMovement(Vec3.ZERO);
        if (canUpdate()) this.baseTick();

        faceEntityStandby();

        // lifetime check
        if (!itFired() && getVehicle() instanceof LivingEntity)
        {
            if (tickCount >= getDelay())
            {
                fireTime = tickCount + getDelay();
                doFire();
            }
        }
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

    // 发射延迟
    public int getDelayTicks() {
        return this.entityData.get(DELAY_TICKS);
    }
    public void setDelayTicks(int delay) {
        this.entityData.set(DELAY_TICKS, delay);
    }

    // 大小缩放
    public float getScale() {
        return this.entityData.get(SCALE);
    }
    public void setScale(float scale) {
        this.entityData.set(SCALE, scale);
    }

    // 目标ID
    public int getTargetId() {
        return this.entityData.get(TARGET_ID);
    }
    public void setTargetId(int id) {
        this.entityData.set(TARGET_ID, id);
    }

    // 待命行为模式：PLAYER / WORLD
    public String getStandbyMode() {
        return this.entityData.get(STANDBY_MODE);
    }
    public void setStandbyMode(String mode) {
        this.entityData.set(STANDBY_MODE, mode);
    }

    // 发射后行为模式：NORMAL / TRACKING
    public String getMovingMode() {
        return this.entityData.get(MOVING_MODE);
    }
    public void setMovingMode(String mode) {
        this.entityData.set(MOVING_MODE, mode);
    }

    // 待命固定朝向 (Vector3f)
    public Vector3f getStandbyVec() {
        return this.entityData.get(STANDBY_VEC);
    }
    public void setStandbyVec(Vector3f vec) {
        this.entityData.set(STANDBY_VEC, vec);
    }

    // 飞行粒子
    public ParticleOptions getParticleTypes() {
        return this.entityData.get(PARTICLE_TYPES);
    }
    public void setParticleTypes(ParticleOptions particle) {
        this.entityData.set(PARTICLE_TYPES, particle);
    }

    public void setOffset(Vec3 offset) {
        this.entityData.set(OFFSET, offset.toVector3f());
    }

    // 是否已发射
    public boolean itFired() {
        return this.getEntityData().get(IT_FIRED);
    }
    public void doFire() {
        this.getEntityData().set(IT_FIRED, true);
    }

    // 发射速度
    public float getSpeed() {
        return this.entityData.get(SPEED);
    }
    public void setSpeed(float speed) {
        this.entityData.set(SPEED, speed);
    }
}
