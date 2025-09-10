package tennouboshiuzume.mods.FantasyDesire.entity;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.entity.IShootable;
import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.RayTraceHelper;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.ForgeEventFactory;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Iterator;
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
    private static final EntityDataAccessor<String> STANDBY_MODE = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.STRING);
    //    发射后行为模式：追踪/直射 NORMAL/TRACKING
    private static final EntityDataAccessor<String> MOVING_MODE = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.STRING);
    //    待命固定朝向 (无论是玩家还是世界)
    private static final EntityDataAccessor<Float> STANDBY_YAW = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STANDBY_PITCH = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.FLOAT);
    //    飞行粒子
    private static final EntityDataAccessor<String> PARTICLE_TYPES = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.STRING);
    //    向量偏移量，仅用于绑定玩家时
    private static final EntityDataAccessor<Vector3f> OFFSET = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.VECTOR3);
    //    是否已发射
    private static final EntityDataAccessor<Boolean> IT_FIRED = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.BOOLEAN);
    //    发射速度
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.FLOAT);
    //    生命周期时长
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(EntityFDPhantomSword.class,EntityDataSerializers.INT);
    //    爆炸半径
    private static final EntityDataAccessor<Float> EXP_RADIUS = SynchedEntityData.defineId(EntityFDPhantomSword.class,EntityDataSerializers.FLOAT);
    //    伤害类型
    private static final EntityDataAccessor<String> DAMAGE_TYPE = SynchedEntityData.defineId(EntityFDPhantomSword.class,EntityDataSerializers.STRING);

    private int ticksInGround;
    private boolean inGround;
    private BlockState inBlockState;
    private int ticksInAir;


    public EntityFDPhantomSword(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    private boolean hasInitialized = false;

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DELAY_TICKS, 0);
        this.entityData.define(SCALE, 1.0f);
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(STANDBY_MODE, "WORLD");//“PLAYER” or "WORLD"
        this.entityData.define(MOVING_MODE, "NORMAL");//"NORMAL" or "TRACKING"
        this.entityData.define(STANDBY_YAW, 0f);
        this.entityData.define(STANDBY_PITCH, 0f);
        this.entityData.define(PARTICLE_TYPES, "Null");
        this.entityData.define(OFFSET, Vec3.ZERO.toVector3f());
        this.entityData.define(IT_FIRED, false);
        this.entityData.define(SPEED, 3f);
        this.entityData.define(LIFETIME,300);
        this.entityData.define(EXP_RADIUS,0f);
        this.entityData.define(DAMAGE_TYPE,"Null");
    }

    @Override
    public void tick() {
//        TODO::行为模式
//        测试使用GunBladeEffects.java类测试
//        检定：如果绑定于玩家，则骑乘玩家，适用视角对应的位置修正，类似BlisteringSwords
//        如果绑定于世界，则适用默认方向修正
//        无论如何，当延迟结束，都向剑的指向方向发射

        if (getStandbyMode().equals("PLAYER") && getOwner() != null) {
            if (!itFired() && getVehicle() == null) {
                startRiding(this.getOwner(), true);
            }
        } else if (getStandbyMode().equals("WORLD") && getOwner() != null) {
            if (!itFired()) {
                if (this.tickCount < this.getDelayTicks()) {
                    updateStandbyOrientation();
                }
                else {
                    fireInStandbyDirection();
                }
            }
        }
        if (itFired()){
            flyticking();
        }

    }

    public void flyticking(){
        if (this.getHitEntity() != null) {
            Entity hits = this.getHitEntity();
            if (!hits.isAlive()) {
                this.burst();
            } else {
                this.setPos(hits.getX(), hits.getY() + (double)(hits.getEyeHeight() * 0.5F), hits.getZ());
                int delay = this.getDelay();
                --delay;
                this.setDelay(delay);
                if (!this.level().isClientSide() && delay < 0) {
                    this.burst();
                }
            }

        } else {
            boolean disallowedHitBlock = this.isNoClip();
            BlockPos blockpos = this.getOnPos();
            BlockState blockstate = this.level().getBlockState(blockpos);
            if (!blockstate.isAir() && !disallowedHitBlock) {
                VoxelShape voxelshape = blockstate.getCollisionShape(this.level(), blockpos);
                if (!voxelshape.isEmpty()) {
                    Iterator var5 = voxelshape.toAabbs().iterator();

                    while(var5.hasNext()) {
                        AABB axisalignedbb = (AABB)var5.next();
                        if (axisalignedbb.move(blockpos).contains(new Vec3(this.getX(), this.getY(), this.getZ()))) {
                            this.inGround = true;
                            break;
                        }
                    }
                }
            }

            if (this.isInWaterOrRain()) {
                this.clearFire();
            }

            if (this.inGround && !disallowedHitBlock) {
                if (this.inBlockState != blockstate && this.level().noCollision(this.getBoundingBox().inflate(0.06))) {
                    this.burst();
                } else if (!this.level().isClientSide()) {
                    this.tryDespawn();
                }
            } else {
                Vec3 motionVec = this.getDeltaMovement();
                if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
                    float f = Mth.sqrt((float)motionVec.horizontalDistanceSqr());
                    this.setYRot((float)(Mth.atan2(motionVec.x, motionVec.z) * 57.2957763671875));
                    this.setXRot((float)(Mth.atan2(motionVec.y, (double)f) * 57.2957763671875));
                    this.yRotO = this.getYRot();
                    this.xRotO = this.getXRot();
                }

                ++this.ticksInAir;
                Vec3 positionVec = this.position();
                Vec3 movedVec = positionVec.add(motionVec);
                HitResult raytraceresult = this.level().clip(new ClipContext(positionVec, movedVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (((HitResult)raytraceresult).getType() != HitResult.Type.MISS) {
                    movedVec = ((HitResult)raytraceresult).getLocation();
                }

                while(this.isAlive()) {
                    EntityHitResult entityraytraceresult = this.getRayTrace(positionVec, movedVec);
                    if (entityraytraceresult != null) {
                        raytraceresult = entityraytraceresult;
                    }

                    if (raytraceresult != null && ((HitResult)raytraceresult).getType() == HitResult.Type.ENTITY) {
                        Entity entity = ((EntityHitResult)raytraceresult).getEntity();
                        Entity entity1 = this.getShooter();
                        if (entity instanceof LivingEntity && entity1 instanceof LivingEntity && !TargetSelector.test.test((LivingEntity)entity1, (LivingEntity)entity)) {
                            raytraceresult = null;
                            entityraytraceresult = null;
                        }
                    }

                    if (raytraceresult != null && (!disallowedHitBlock || ((HitResult)raytraceresult).getType() != HitResult.Type.BLOCK) && !ForgeEventFactory.onProjectileImpact(this, (HitResult)raytraceresult)) {
                        this.onHit((HitResult)raytraceresult);
                        this.hasImpulse = true;
                    }

                    if (entityraytraceresult == null || this.getPierce() <= 0) {
                        break;
                    }

                    raytraceresult = null;
                }

                motionVec = this.getDeltaMovement();
                double mx = motionVec.x;
                double my = motionVec.y;
                double mz = motionVec.z;
                if (this.getIsCritical()) {
                    for(int i = 0; i < 4; ++i) {
                        this.level().addParticle(ParticleTypes.CRIT, this.getX() + mx * (double)i / 4.0, this.getY() + my * (double)i / 4.0, this.getZ() + mz * (double)i / 4.0, -mx, -my + 0.2, -mz);
                    }
                }

                this.setPos(this.getX() + mx, this.getY() + my, this.getZ() + mz);
                float f4 = Mth.sqrt((float)motionVec.horizontalDistanceSqr());
                if (disallowedHitBlock) {
                    this.setYRot((float)(Mth.atan2(-mx, -mz) * 57.2957763671875));
                } else {
                    this.setYRot((float)(Mth.atan2(mx, mz) * 57.2957763671875));
                }

                this.setXRot((float)(Mth.atan2(my, (double)f4) * 57.2957763671875));

                while(this.getXRot() - this.xRotO < -180.0F) {
                    this.xRotO -= 360.0F;
                }

                while(this.getXRot() - this.xRotO >= 180.0F) {
                    this.xRotO += 360.0F;
                }

                while(this.getYRot() - this.yRotO < -180.0F) {
                    this.yRotO -= 360.0F;
                }

                while(this.getYRot() - this.yRotO >= 180.0F) {
                    this.yRotO += 360.0F;
                }

                this.setXRot(Mth.lerp(0.2F, this.xRotO, this.getXRot()));
                this.setYRot(Mth.lerp(0.2F, this.yRotO, this.getYRot()));
                float f1 = 0.99F;
                if (this.isInWater()) {
                    for(int j = 0; j < 4; ++j) {
                        this.level().addParticle(ParticleTypes.BUBBLE, this.getX() - mx * 0.25, this.getY() - my * 0.25, this.getZ() - mz * 0.25, mx, my, mz);
                    }
                }

                this.setDeltaMovement(motionVec.scale((double)f1));
                if (!this.isNoGravity() && !disallowedHitBlock) {
                    Vec3 vec3d3 = this.getDeltaMovement();
                    this.setDeltaMovement(vec3d3.x, vec3d3.y - 0.05000000074505806, vec3d3.z);
                }

                this.checkInsideBlocks();
            }

            if (!this.level().isClientSide() && this.ticksInGround <= 0 && 100 < this.tickCount) {
                this.remove(RemovalReason.DISCARDED);
            }

        }
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


//    DONE 数据持久化
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("Delay", this.entityData.get(DELAY_TICKS));
        tag.putFloat("Scale", this.entityData.get(SCALE));
        tag.putInt("TargetId", this.entityData.get(TARGET_ID));
        tag.putString("StandbyMode", this.entityData.get(STANDBY_MODE));
        tag.putString("MovingMode", this.entityData.get(MOVING_MODE));
        tag.putFloat("StandbyYaw", this.entityData.get(STANDBY_YAW));
        tag.putFloat("StandbyPitch", this.entityData.get(STANDBY_PITCH));
        tag.putString("ParticleTypes", this.entityData.get(PARTICLE_TYPES));

        Vector3f offset = this.entityData.get(OFFSET);
        CompoundTag offsetTag = new CompoundTag();
        offsetTag.putFloat("X", offset.x());
        offsetTag.putFloat("Y", offset.y());
        offsetTag.putFloat("Z", offset.z());
        tag.put("Offset", offsetTag);

        tag.putBoolean("ItFired", this.entityData.get(IT_FIRED));
        tag.putFloat("Speed", this.entityData.get(SPEED));
        tag.putInt("Lifetime", this.entityData.get(LIFETIME));
        tag.putFloat("ExpRadius", this.entityData.get(EXP_RADIUS));
        tag.putString("DamageType", this.entityData.get(DAMAGE_TYPE));
    }

    // 读取
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        this.entityData.set(DELAY_TICKS, tag.getInt("Delay"));
        this.entityData.set(SCALE, tag.getFloat("Scale"));
        this.entityData.set(TARGET_ID, tag.getInt("TargetId"));
        this.entityData.set(STANDBY_MODE, tag.getString("StandbyMode"));
        this.entityData.set(MOVING_MODE, tag.getString("MovingMode"));
        this.entityData.set(STANDBY_YAW, tag.getFloat("StandbyYaw"));
        this.entityData.set(STANDBY_PITCH, tag.getFloat("StandbyPitch"));
        this.entityData.set(PARTICLE_TYPES, tag.getString("ParticleTypes"));

        if (tag.contains("Offset", Tag.TAG_COMPOUND)) {
            CompoundTag offsetTag = tag.getCompound("Offset");
            Vector3f offset = new Vector3f(
                    offsetTag.getFloat("X"),
                    offsetTag.getFloat("Y"),
                    offsetTag.getFloat("Z")
            );
            this.entityData.set(OFFSET, offset);
        }

        this.entityData.set(IT_FIRED, tag.getBoolean("ItFired"));
        this.entityData.set(SPEED, tag.getFloat("Speed"));
        this.entityData.set(LIFETIME, tag.getInt("Lifetime"));
        this.entityData.set(EXP_RADIUS, tag.getFloat("ExpRadius"));
        this.entityData.set(DAMAGE_TYPE, tag.getString("DamageType"));
    }

    @Override
    public void rideTick() {
        if (itFired() && getDelayTicks() <= tickCount) {
            faceEntityStandby();
            Entity vehicle = getVehicle();
            Vec3 dir = this.getViewVector(0);
            if (!(vehicle instanceof LivingEntity)) {
                dir = dir.yRot((float) Math.toRadians(-getStandbyYawPitch()[0]));
                dir = dir.xRot((float) Math.toRadians(-getStandbyYawPitch()[1]));
                this.shoot(dir.x, dir.y, dir.z, getSpeed(), 1.0f);
                return;
            }

            LivingEntity sender = (LivingEntity) getVehicle();
            this.stopRiding();

            this.tickCount = 0;

            Level worldIn = sender.level();
            Entity lockTarget = null;
            if (sender instanceof LivingEntity) {
                lockTarget = sender.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).filter(state -> state.getTargetEntity(worldIn) != null).map(state -> state.getTargetEntity(worldIn)).orElse(null);
            }

            Optional<Entity> foundTarget = Stream.of(Optional.ofNullable(lockTarget), RayTraceHelper.rayTrace(sender.level(), sender, sender.getEyePosition(1.0f), sender.getLookAngle(), 12, 12, (e) -> true).filter(r -> r.getType() == HitResult.Type.ENTITY).filter(r -> {
                EntityHitResult er = (EntityHitResult) r;
                Entity target = er.getEntity();

                boolean isMatch = true;
                if (target instanceof LivingEntity) isMatch = TargetSelector.test.test(sender, (LivingEntity) target);

                if (target instanceof IShootable) isMatch = ((IShootable) target).getShooter() != sender;

                return isMatch;
            }).map(r -> ((EntityHitResult) r).getEntity())).filter(Optional::isPresent).map(Optional::get).findFirst();

            Vec3 targetPos = foundTarget.map((e) -> new Vec3(e.getX(), e.getY() + e.getEyeHeight() * 0.5, e.getZ())).orElseGet(() -> {
                Vec3 start = sender.getEyePosition(1.0f);
                Vec3 end = start.add(sender.getLookAngle().scale(40));
                HitResult result = worldIn.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sender));
                return result.getLocation();
            });

            Vec3 pos = this.getPosition(0.0f);
            dir = targetPos.subtract(pos).normalize();

            dir = dir.yRot((float) Math.toRadians(-getStandbyYawPitch()[0]));
            dir = dir.xRot((float) Math.toRadians(-getStandbyYawPitch()[1]));

            this.shoot(dir.x, dir.y, dir.z, getSpeed(), 1.0f);
            if (sender instanceof ServerPlayer) {
                ((ServerPlayer) sender).playNotifySound(SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0F, 1.0F);
            }

            return;
        }

        this.setDeltaMovement(Vec3.ZERO);
        if (canUpdate()) this.baseTick();

        faceEntityStandby();

        // lifetime check
        if (!itFired() && getVehicle() instanceof LivingEntity) {
            if (tickCount >= getDelayTicks()) {
                doFire();
            }
        }
    }

    protected void faceEntityStandby() {
        Vec3 pos = this.getVehicle().position();
        Vec3 offset = this.getOffset();

        if (this.getVehicle() == null) {
            doFire();
            return;
        }

        offset = offset.xRot((float) Math.toRadians(-this.getVehicle().getXRot()));
        offset = offset.yRot((float) Math.toRadians(-this.getVehicle().getYRot()));

        pos = pos.add(offset);

        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();

        setPos(pos);

        setRot(-this.getVehicle().getYRot()-getStandbyYawPitch()[0], -this.getVehicle().getXRot()-getStandbyYawPitch()[1]);
    }

    private void updateStandbyOrientation() {
        Entity target = getTargetEntity(); // 通过目标ID获取实体
        if (target != null && target.isAlive()) {
            // 计算朝向目标的 yaw/pitch
            Vec3 toTarget = target.position().add(0, target.getEyeHeight() * 0.5, 0)
                    .subtract(this.position());
            double dx = toTarget.x;
            double dy = toTarget.y;
            double dz = toTarget.z;

            float targetYaw = (float)(Mth.atan2(dx, dz) * (180F / Math.PI));
            float targetPitch = (float)(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180F / Math.PI));

            // 插值旋转，使得逐渐转向，而不是瞬间对准
            float newYaw = Mth.approachDegrees(this.getYRot(), targetYaw, 5f);   // 每tick最多转5°
            float newPitch = Mth.approachDegrees(this.getXRot(), targetPitch, 5f);

            this.setYRot(newYaw);
            this.setXRot(newPitch);

            // 更新 standbyYawPitch 以保证发射时方向一致
            this.getEntityData().set(STANDBY_YAW, newYaw);
            this.getEntityData().set(STANDBY_PITCH, newPitch);

        } else {
            // 没有目标：保持生成时的 standbyYawPitch，不做处理
            this.setYRot(this.getEntityData().get(STANDBY_YAW));
            this.setXRot(this.getEntityData().get(STANDBY_PITCH));
        }
    }

    private void fireInStandbyDirection() {
        float yaw = this.getEntityData().get(STANDBY_YAW);
        float pitch = this.getEntityData().get(STANDBY_PITCH);

        // 如果有目标，则重新计算指向目标
        Entity target = getTargetEntity();
        if (target != null && target.isAlive()) {
            Vec3 toTarget = target.position().add(0, target.getEyeHeight() * 0.5, 0)
                    .subtract(this.position()).normalize();
            this.shoot(toTarget.x, toTarget.y, toTarget.z, getSpeed(), 1.0F);
        } else {
            // 按照 yaw/pitch 生成方向向量
            double x = -Mth.sin(yaw * ((float)Math.PI / 180F)) * Mth.cos(pitch * ((float)Math.PI / 180F));
            double y = -Mth.sin(pitch * ((float)Math.PI / 180F));
            double z = Mth.cos(yaw * ((float)Math.PI / 180F)) * Mth.cos(pitch * ((float)Math.PI / 180F));

            this.shoot(x, y, z, getSpeed(), 1.0F);
        }

        this.doFire();
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

    // 待命固定朝向
    public float[] getStandbyYawPitch() {
        return new float[]{this.entityData.get(STANDBY_YAW), this.entityData.get(STANDBY_PITCH)};
    }

    public void setStandbyYawPitch(float yaw, float pitch) {
        this.entityData.set(STANDBY_YAW, yaw);
        this.entityData.set(STANDBY_PITCH, pitch);
    }

//    // 飞行粒子
//    public ParticleOptions getParticleTypes() {
//        return this.entityData.get(PARTICLE_TYPES);
//    }
//    public void setParticleTypes(ParticleOptions particle) {
//        this.entityData.set(PARTICLE_TYPES, particle);
//    }

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
