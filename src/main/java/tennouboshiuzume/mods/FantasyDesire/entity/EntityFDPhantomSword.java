package tennouboshiuzume.mods.FantasyDesire.entity;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import mods.flammpfeil.slashblade.SlashBladeConfig;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;
import tennouboshiuzume.mods.FantasyDesire.utils.TargetUtils;
import tennouboshiuzume.mods.FantasyDesire.utils.VecMathUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Set;


public class EntityFDPhantomSword extends EntityAbstractSummonedSword {

    //    发射延迟
    private static final EntityDataAccessor<Integer> DELAY_TICKS = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.INT);
    //    追踪行为延迟，大于发射延迟时控制飞行行为，小于发射延迟时控制待命行为
    private static final EntityDataAccessor<Integer> SEEK_DELAY = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.INT);
    //    大小缩放
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.FLOAT);
    //    目标ID
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.INT);
    //    待命行为模式：绑定于玩家/绑定于世界 PLAYER/WORLD
    private static final EntityDataAccessor<String> STANDBY_MODE = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.STRING);
    //    发射后行为模式：追踪/直射 NORMAL/SEEK
    private static final EntityDataAccessor<String> MOVING_MODE = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.STRING);
    //    待命固定朝向 (无论是玩家还是世界)
    private static final EntityDataAccessor<Float> STANDBY_YAW = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STANDBY_PITCH = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.FLOAT);
    //    飞行粒子
    private static final EntityDataAccessor<String> PARTICLE_TYPES = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.STRING);
    //    向量偏移量，仅用于绑定玩家时
    private static final EntityDataAccessor<Vector3f> OFFSET = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.VECTOR3);
    //    中心向量偏移加值
    private static final EntityDataAccessor<Vector3f> CENTER_OFFSET = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.VECTOR3);
    //    是否已发射
    private static final EntityDataAccessor<Boolean> IT_FIRED = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.BOOLEAN);
    //    发射速度
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.FLOAT);
    //    生命周期时长
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.INT);
    //    爆炸半径
    private static final EntityDataAccessor<Float> EXP_RADIUS = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.FLOAT);
    //    伤害类型
    private static final EntityDataAccessor<String> DAMAGE_TYPE = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Boolean> MULTIPLE_HIT = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.BOOLEAN);

    protected boolean inited = false;
    protected boolean isSeeking = false;
    protected SoundEvent fireSound = null;
    protected float fireSoundVolume = 1;
    protected float fireSoundRate = 1;
    protected final Int2IntOpenHashMap hitCooldowns = new Int2IntOpenHashMap();

    public EntityFDPhantomSword(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DELAY_TICKS, 0);
        this.entityData.define(SEEK_DELAY, 0);
        this.entityData.define(SCALE, 1.0f);
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(STANDBY_MODE, "WORLD");//“PLAYER” or "WORLD"
        this.entityData.define(MOVING_MODE, "NORMAL");//"NORMAL" or "SEEK"
        this.entityData.define(STANDBY_YAW, 0f);
        this.entityData.define(STANDBY_PITCH, 0f);
        this.entityData.define(PARTICLE_TYPES, "Null");
        this.entityData.define(OFFSET, Vec3.ZERO.toVector3f());
        this.entityData.define(CENTER_OFFSET, Vec3.ZERO.toVector3f());
        this.entityData.define(IT_FIRED, false);
        this.entityData.define(MULTIPLE_HIT, false);
        this.entityData.define(SPEED, 3f);
        this.entityData.define(LIFETIME, 300);
        this.entityData.define(EXP_RADIUS, 0f);
        this.entityData.define(DAMAGE_TYPE, "Null");
    }

    @Override
    public void tick() {
//        TODO::行为模式
//        测试使用GunBladeEffects.java类测试
//        检定：如果绑定于玩家，则根据玩家位置，适用视角对应的位置修正，类似BlisteringSwords
//        如果绑定于世界，则适用默认方向修正 （STANDBY_YAW 、STANDBY_PITCH）
//        如果待命期间有目标且跟踪延迟结束，以每5deg/tick转向，如果跟踪延迟未结束，保持当前方向并且继续适用以上修正
//        发射延迟结束时，向指向方向发射
//        发射后，当跟踪延迟结束时，以15deg*(目标当前速度/自身基础速度)/tick向目标转向，并使飞行速度基于基础速度加上目标的移动速度
//        Example：
//          有目标，发射延迟<追踪延迟：以初始方向发射后再追踪敌人
//          绑定于世界，有目标，无追踪延迟，有发射延迟：以基础方向生成，并且立即开始转向目标，延迟结束时按朝向发射
//
        if (getShooter() == null) {
            if (tickCount > 100) remove(RemovalReason.DISCARDED);
            return;
        }

        if (!inited) {
            tryInit();
            if (!inited) return;
        }
        if (getStandbyMode().equals("PLAYER") && getOwner() != null) {
            if (!getFired()) {
                if (this.tickCount < this.getDelayTicks()) {
                    updateStandbyOrientationByShooter();
                } else {
                    fire();
                }
            }
        } else if (getStandbyMode().equals("WORLD") && getOwner() != null) {
            if (!getFired()) {
                if (this.tickCount < this.getDelayTicks()) {
                    updateStandbyOrientation();
                } else {
                    fire();
                }
            }
        }

        if (getFired()) {
            if (tickCount > getSeekDelay()
                    && this.getMovingMode().equals("SEEK")
                    && getTargetId() != -1
                    && !getInGround()) {  // 只有在未撞墙/未落地时才追踪
                Entity e = this.level().getEntity(this.getTargetId());
                if (e instanceof LivingEntity living && living.isAlive()) {
                    seeking(living);
                    this.isSeeking = true;
                } else {
                    this.isSeeking = false;
                    setTargetId(-1);
                }
            }else {
                this.isSeeking = false;
            }
            flyticking();
        }
        if (!getInGround() && (getPierce() > 0 || getHitEntity() == null)) playparticle();
    }

    private void playparticle() {
        if (this.level().isClientSide()) return;
        if (getParticleType() == null) return;

        ServerLevel sl = (ServerLevel) this.level();

        // 上一 tick 位置
        double px = this.xo;
        double py = this.yo;
        double pz = this.zo;

        // 当前 tick 位置
        double cx = this.getX();
        double cy = this.getY();
        double cz = this.getZ();

        // 插值生成粒子点
        int steps = (int) getSpeed(); // 轨迹分段数量，越大越密
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double x = Mth.lerp(t, px, cx);
            double y = Mth.lerp(t, py, cy);
            double z = Mth.lerp(t, pz, cz);

            sl.sendParticles(
                    getParticleType(),
                    x, y, z,
                    1,
                    0.25 * getScale(),
                    0.25 * getScale(),
                    0.25 * getScale(),
                    0.05 * getScale()
            );
        }
    }

    private void flyticking() {
        if (this.getHitEntity() != null) {
            Entity hits = this.getHitEntity();
            if (!hits.isAlive()) {
                this.burst();
            } else {
                this.setPos(hits.getX(), hits.getY() + (double) (hits.getEyeHeight() * 0.5F), hits.getZ());
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
                    // 用飞剑膨胀后的 AABB 来检测
                    AABB swordBox = this.getBoundingBox().inflate(getScale());
                    for (AABB axisalignedbb : voxelshape.toAabbs()) {
                        if (axisalignedbb.move(blockpos).intersects(swordBox)) {
                            setInGround(true);
                            break;
                        }
                    }
                }
            }

            if (this.isInWaterOrRain()) {
                this.clearFire();
            }

            if (getInGround() && !disallowedHitBlock) {
                if (getInBlockState() != blockstate && this.level().noCollision(this.getBoundingBox().inflate(0.06))) {
                    this.burst();
                } else if (!this.level().isClientSide()) {
                    this.tryDespawn();
                }
            } else {
                Vec3 motionVec = this.getDeltaMovement();
                if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
                    float f = Mth.sqrt((float) motionVec.horizontalDistanceSqr());
                    this.setYRot((float) (Mth.atan2(motionVec.x, motionVec.z) * 57.2957763671875));
                    this.setXRot((float) (Mth.atan2(motionVec.y, (double) f) * 57.2957763671875));
                    this.yRotO = this.getYRot();
                    this.xRotO = this.getXRot();
                }

                Vec3 positionVec = this.position();
                Vec3 movedVec = positionVec.add(motionVec);
                HitResult raytraceresult = this.level().clip(new ClipContext(positionVec, movedVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (((HitResult) raytraceresult).getType() != HitResult.Type.MISS) {
                    movedVec = ((HitResult) raytraceresult).getLocation();
                }

                while (this.isAlive()) {
                    EntityHitResult entityraytraceresult = this.getRayTrace(positionVec, movedVec.scale(getScale()));
                    if (entityraytraceresult != null) {
                        raytraceresult = entityraytraceresult;
                    }

                    if (raytraceresult != null && ((HitResult) raytraceresult).getType() == HitResult.Type.ENTITY) {
                        Entity entity = ((EntityHitResult) raytraceresult).getEntity();
                        Entity entity1 = this.getShooter();
                        if (entity instanceof LivingEntity && entity1 instanceof LivingEntity && !TargetSelector.test.test((LivingEntity) entity1, (LivingEntity) entity)) {
                            raytraceresult = null;
                            entityraytraceresult = null;
                        }
                    }

                    if (raytraceresult != null && (!disallowedHitBlock || ((HitResult) raytraceresult).getType() != HitResult.Type.BLOCK) && !ForgeEventFactory.onProjectileImpact(this, (HitResult) raytraceresult)) {
                        this.onHit((HitResult) raytraceresult);
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
                    for (int i = 0; i < 4; ++i) {
                        this.level().addParticle(ParticleTypes.CRIT, this.getX() + mx * (double) i / 4.0, this.getY() + my * (double) i / 4.0, this.getZ() + mz * (double) i / 4.0, -mx, -my + 0.2, -mz);
                    }
                }

                this.setPos(this.getX() + mx, this.getY() + my, this.getZ() + mz);
                float f4 = Mth.sqrt((float) motionVec.horizontalDistanceSqr());
//                if (disallowedHitBlock) {
//                    this.setYRot((float) (Mth.atan2(-mx, -mz) * 57.2957763671875));
//                } else {
//                    this.setYRot((float) (Mth.atan2(mx, mz) * 57.2957763671875));
//                }

                this.setXRot((float) (Mth.atan2(my, (double) f4) * 57.2957763671875));

                while (this.getXRot() - this.xRotO < -180.0F) {
                    this.xRotO -= 360.0F;
                }

                while (this.getXRot() - this.xRotO >= 180.0F) {
                    this.xRotO += 360.0F;
                }

                while (this.getYRot() - this.yRotO < -180.0F) {
                    this.yRotO -= 360.0F;
                }

                while (this.getYRot() - this.yRotO >= 180.0F) {
                    this.yRotO += 360.0F;
                }
                if (!this.isSeeking) {
                    this.setXRot(Mth.lerp(0.2F, this.xRotO, this.getXRot()));
                    this.setYRot(Mth.lerp(0.2F, this.yRotO, this.getYRot()));
                }
                float f1 = 0.99F;
                if (this.isInWater()) {
                    for (int j = 0; j < 4; ++j) {
                        this.level().addParticle(ParticleTypes.BUBBLE, this.getX() - mx * 0.25, this.getY() - my * 0.25, this.getZ() - mz * 0.25, mx, my, mz);
                    }
                }

                this.setDeltaMovement(motionVec.scale((double) f1));
                if (!this.isNoGravity() && !disallowedHitBlock) {
                    Vec3 vec3d3 = this.getDeltaMovement();
                    this.setDeltaMovement(vec3d3.x, vec3d3.y - 0.05000000074505806, vec3d3.z);
                }

                this.checkInsideBlocks();
            }

            if (!this.level().isClientSide() && getTicksInGround() <= 0 && 100 < this.tickCount) {
                this.remove(RemovalReason.DISCARDED);
            }

        }
    }

    private void seeking(LivingEntity target) {
        if (target == null || !target.isAlive()) return;

        Vec3 swordPos = this.position();
        Vec3 targetPos = target.position().add(0, target.getEyeHeight() * 0.5, 0);
        Vec3 toTarget = targetPos.subtract(swordPos).normalize();

        Vec3 currentDir = this.getDeltaMovement();
        if (currentDir.lengthSqr() < 1e-7) {
            currentDir = toTarget; // 初始指向目标
        } else {
            currentDir = currentDir.normalize();
        }

        double maxTurn = Math.max(Math.toRadians(10), Math.toRadians(30 * this.getDeltaMovement().length()));
        Vec3 finalDir = VecMathUtils.rotateTowards(currentDir, toTarget, (float) maxTurn);

        double baseSpeed = this.getSpeed();
        double targetSpeed = target.getDeltaMovement().length();
        double desiredSpeed = Math.max(baseSpeed, targetSpeed);

        this.setDeltaMovement(finalDir.scale(desiredSpeed));

        float yaw = (float) (Math.atan2(finalDir.x, finalDir.z) * 180.0 / Math.PI);
        float pitch = (float) (Math.atan2(finalDir.y, Math.sqrt(finalDir.x * finalDir.x + finalDir.z * finalDir.z)) * 180.0 / Math.PI);
        this.setRot(yaw, pitch);
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        Entity targetEntity = entityHitResult.getEntity();
        SlashBladeEvent.SummonedSwordOnHitEntityEvent event = new SlashBladeEvent.SummonedSwordOnHitEntityEvent(this);
        MinecraftForge.EVENT_BUS.post(event);
        int i = Mth.ceil(this.getDamage());
        if (this.getPierce() > 0) {
            if (getAlreadyHits() == null) {
                setAlreadyHits(new IntOpenHashSet(5));
            }

            if (getAlreadyHits().size() >= this.getPierce() + 1) {
                this.burst();
                return;
            }

            getAlreadyHits().add(targetEntity.getId());
        }

        if (this.getIsCritical()) {
            i += this.random.nextInt(i / 2 + 2);
        }

        Entity shooter = this.getShooter();
        DamageSource damagesource;
        if (shooter == null) {
            damagesource = this.damageSources().indirectMagic(this, this);
        } else {
            damagesource = this.damageSources().indirectMagic(this, shooter);
            if (shooter instanceof LivingEntity) {
                Entity hits = targetEntity;
                if (targetEntity instanceof PartEntity) {
                    hits = ((PartEntity) targetEntity).getParent();
                }

                ((LivingEntity) shooter).setLastHurtMob(hits);
            }
        }

        int fireTime = targetEntity.getRemainingFireTicks();
        if (this.isOnFire() && !(targetEntity instanceof EnderMan)) {
            targetEntity.setSecondsOnFire(5);
        }

        targetEntity.invulnerableTime = 0;
        float scale = 1.0F;
        if (shooter instanceof LivingEntity living) {
            scale = (float) ((double) AttackManager.getSlashBladeDamageScale(living) * (Double) SlashBladeConfig.SLASHBLADE_DAMAGE_MULTIPLIER.get());
        }

        float damageValue = (float) i * scale;
        if (targetEntity.hurt(damagesource, damageValue)) {
            Entity hits = targetEntity;
            if (targetEntity instanceof PartEntity) {
                hits = ((PartEntity) targetEntity).getParent();
            }

            if (hits instanceof LivingEntity) {
                LivingEntity targetLivingEntity = (LivingEntity) hits;
                StunManager.setStun(targetLivingEntity);
                if (!this.level().isClientSide() && this.getPierce() <= 0) {
                    this.setHitEntity(hits);
                }

                if (!this.level().isClientSide() && shooter instanceof LivingEntity) {
                    EnchantmentHelper.doPostHurtEffects(targetLivingEntity, shooter);
                    EnchantmentHelper.doPostDamageEffects((LivingEntity) shooter, targetLivingEntity);
                }

                this.affectEntity(targetLivingEntity, this.getPotionEffects(), 1.0);
                if (shooter != null && targetLivingEntity != shooter && targetLivingEntity instanceof Player && shooter instanceof ServerPlayer) {
                    ((ServerPlayer) shooter).playNotifySound(this.getHitEntityPlayerSound(), SoundSource.PLAYERS, 0.18F, 0.45F);
                }
            }

            this.playSound(this.getHitEntitySound(), 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
            if (this.getPierce() <= 0 && (this.getHitEntity() == null || !this.getHitEntity().isAlive())) {
                this.burst();
            }
        } else {
            targetEntity.setRemainingFireTicks(fireTime);
            setTicksInAir(0);
            if (!this.level().isClientSide() && this.getDeltaMovement().lengthSqr() < 1.0E-7) {
                if (this.getPierce() <= 1) {
                    this.burst();
                } else {
                    this.setPierce((byte) (this.getPierce() - 1));
                }
            }
        }
        if (this.entityData.get(EXP_RADIUS)>0){
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), this.entityData.get(EXP_RADIUS), Level.ExplosionInteraction.NONE);
            this.burst();
        }
    }
    @Override
    protected void onHitBlock(BlockHitResult blockraytraceresult) {
        super.onHitBlock(blockraytraceresult);
        if (this.entityData.get(EXP_RADIUS)>0){
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), this.entityData.get(EXP_RADIUS), Level.ExplosionInteraction.NONE);
            this.burst();
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
        tag.putInt("DelayTicks", this.entityData.get(DELAY_TICKS));
        tag.putInt("SeekDelay", this.entityData.get(SEEK_DELAY));
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
        Vector3f centeroffset = this.entityData.get(CENTER_OFFSET);
        CompoundTag centeroffsetTag = new CompoundTag();
        centeroffsetTag.putFloat("X", centeroffset.x());
        centeroffsetTag.putFloat("Y", centeroffset.y());
        centeroffsetTag.putFloat("Z", centeroffset.z());
        tag.put("CenterOffset", centeroffsetTag);
        tag.putBoolean("ItFired", this.entityData.get(IT_FIRED));
        tag.putBoolean("MultipleHit", this.entityData.get(MULTIPLE_HIT));
        tag.putFloat("Speed", this.entityData.get(SPEED));
        tag.putInt("Lifetime", this.entityData.get(LIFETIME));
        tag.putFloat("ExpRadius", this.entityData.get(EXP_RADIUS));
        tag.putString("DamageType", this.entityData.get(DAMAGE_TYPE));
    }

    // 读取
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DELAY_TICKS, tag.getInt("DelayTicks"));
        this.entityData.set(SEEK_DELAY, tag.getInt("SeekDelay"));
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
        if (tag.contains("CenterOffset", Tag.TAG_COMPOUND)) {
            CompoundTag offsetTag = tag.getCompound("CenterOffset");
            Vector3f CenterOffset = new Vector3f(
                    offsetTag.getFloat("X"),
                    offsetTag.getFloat("Y"),
                    offsetTag.getFloat("Z")
            );
            this.entityData.set(CENTER_OFFSET, CenterOffset);
        }
        this.entityData.set(IT_FIRED, tag.getBoolean("ItFired"));
        this.entityData.set(MULTIPLE_HIT, tag.getBoolean("MultipleHit"));
        this.entityData.set(SPEED, tag.getFloat("Speed"));
        this.entityData.set(LIFETIME, tag.getInt("Lifetime"));
        this.entityData.set(EXP_RADIUS, tag.getFloat("ExpRadius"));
        this.entityData.set(DAMAGE_TYPE, tag.getString("DamageType"));
    }

    private void updateStandbyOrientationByShooter() {
        Vec3 pos = this.getShooter().position().add(this.getCenterOffset());
        Vec3 offset = this.getOffset();
        offset = offset.xRot((float) Math.toRadians(-this.getShooter().getXRot()));
        offset = offset.yRot((float) Math.toRadians(-this.getShooter().getYRot()));
        pos = pos.add(offset);
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
        setPos(pos);
        Entity target = getTargetEntity(); // 通过目标ID获取实体
        if (target != null && target.isAlive() && tickCount > getSeekDelay()) {
            // 计算朝向目标的 yaw/pitch
            Vec3 toTarget = target.position().add(0, target.getEyeHeight() * 0.5, 0)
                    .subtract(this.position());
            double dx = toTarget.x;
            double dy = toTarget.y;
            double dz = toTarget.z;

            float targetYaw = (float) (Mth.atan2(dx, dz) * (180F / Math.PI));
            float targetPitch = (float) (Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180F / Math.PI));

            float newYaw = Mth.approachDegrees(-this.getShooter().getYRot() - getStandbyYawPitch()[0], targetYaw, 5f * tickCount);   // 每tick最多转5°
            float newPitch = Mth.approachDegrees(-this.getShooter().getXRot() - getStandbyYawPitch()[1], targetPitch, 5f * tickCount);

            this.setYRot(newYaw);
            this.setXRot(newPitch);
        } else {
            float newYaw = -this.getShooter().getYRot() - getStandbyYawPitch()[0];
            float newPitch = -this.getShooter().getXRot() - getStandbyYawPitch()[1];

            this.setRot(newYaw, newPitch);
        }
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

            float targetYaw = (float) (Mth.atan2(dx, dz) * (180F / Math.PI));
            float targetPitch = (float) (Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180F / Math.PI));

            // 插值旋转，使得逐渐转向，而不是瞬间对准
            float newYaw = Mth.approachDegrees(this.getYRot(), targetYaw, 5f);   // 每tick最多转5°
            float newPitch = Mth.approachDegrees(this.getXRot(), targetPitch, 5f);

            this.setYRot(newYaw);
            this.setXRot(newPitch);
        }

    }

    private void fire() {
        float yaw = -this.getYRot();
        float pitch = -this.getXRot();
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);
        this.shoot(dir.x, dir.y, dir.z, getSpeed(), getSpeed());
        playFireSound(this.fireSoundVolume, this.fireSoundRate);
        this.setFired();
    }

    private void playFireSound(float volume, float pitch) {
        SoundEvent sound = fireSound;
        if (sound != null) {
            this.playSound(sound, volume, pitch);
        }
    }

    private void tryInit() {
        if (this.getStandbyMode().equals("WORLD")) {
            this.yRotO = getStandbyYawPitch()[0];
            this.xRotO = getStandbyYawPitch()[1];
            this.setYRot(this.yRotO);
            this.setXRot(this.xRotO);
            inited = true;
        } else if (getShooter() != null) {
            this.yRotO = -getShooter().getYRot() - getStandbyYawPitch()[0];
            this.xRotO = -getShooter().getXRot() - getStandbyYawPitch()[1];
            this.setYRot(this.yRotO);
            this.setXRot(this.xRotO);
            inited = true;
        }
    }

    public void tryUpdateTarget() {
        TargetUtils.getLockTarget((LivingEntity) getShooter());
    }

    public Vec3 getOffset() {
        return new Vec3(this.getEntityData().get(OFFSET));
    }

    public Vec3 getCenterOffset() {
        return new Vec3(this.getEntityData().get(CENTER_OFFSET));
    }

    // 发射延迟
    public int getDelayTicks() {
        return this.entityData.get(DELAY_TICKS);
    }

    public void setDelayTicks(int delay) {
        this.entityData.set(DELAY_TICKS, delay);
    }

    public void setExpRadius(float value){
        this.entityData.set(EXP_RADIUS,value);
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

    public int getSeekDelay() {
        return this.entityData.get(SEEK_DELAY);
    }

    public void setSeekDelay(int delay) {
        this.entityData.set(SEEK_DELAY, delay);
    }

    // 飞行粒子
    public ParticleOptions getParticleType() {
        String id = this.entityData.get(PARTICLE_TYPES);
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(new ResourceLocation(id));
        if (type instanceof SimpleParticleType) {
            return (SimpleParticleType) type;
        }
        return null;
    }

    public void setParticleType(ParticleType<?> type) {
        ResourceLocation id = BuiltInRegistries.PARTICLE_TYPE.getKey(type);
        if (id != null) {
            this.entityData.set(PARTICLE_TYPES, id.toString());
        }
    }

    public void setFireSound(SoundEvent sound, float volume, float rate) {
        this.fireSound = sound;
        this.fireSoundRate = rate;
        this.fireSoundVolume = volume;
    }


    public void setOffset(Vec3 offset) {
        this.entityData.set(OFFSET, offset.toVector3f());
    }

    public void setCenterOffset(Vec3 offset) {
        this.entityData.set(CENTER_OFFSET, offset.toVector3f());
    }

    // 是否已发射
    public boolean getFired() {
        return this.getEntityData().get(IT_FIRED);
    }

    public void setFired() {
        this.getEntityData().set(IT_FIRED, true);
    }

    // 发射速度
    public float getSpeed() {
        return this.entityData.get(SPEED);
    }

    public void setSpeed(float speed) {
        this.entityData.set(SPEED, speed);
    }

    public void setMultipleHit(boolean value) {
        this.entityData.set(MULTIPLE_HIT, value);
    }

    public boolean getMultipleHit() {
        return this.entityData.get(MULTIPLE_HIT);
    }

    //////////////////////////
    //       我讨厌反射       //
    //////////////////////////
    // 读取父类字段
    public boolean getInGround() {
        try {
            Field field = EntityAbstractSummonedSword.class.getDeclaredField("inGround");
            field.setAccessible(true);
            return field.getBoolean(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }
    }

    public BlockState getInBlockState() {
        try {
            Field field = EntityAbstractSummonedSword.class.getDeclaredField("inBlockState");
            field.setAccessible(true);
            return (BlockState) field.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getTicksInGround() {
        try {
            Field field = EntityAbstractSummonedSword.class.getDeclaredField("ticksInGround");
            field.setAccessible(true);
            return field.getInt(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public IntOpenHashSet getAlreadyHits() {
        try {
            Field f = EntityAbstractSummonedSword.class.getDeclaredField("alreadyHits");
            f.setAccessible(true);
            return (IntOpenHashSet) f.get(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getTicksInAir() {
        try {
            Field f = EntityAbstractSummonedSword.class.getDeclaredField("ticksInAir");
            f.setAccessible(true);
            return (int) f.get(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 修改父类字段
    public void setInGround(boolean value) {
        try {
            Field field = EntityAbstractSummonedSword.class.getDeclaredField("inGround");
            field.setAccessible(true);
            field.setBoolean(this, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void setInBlockState(BlockState state) {
        try {
            Field field = EntityAbstractSummonedSword.class.getDeclaredField("inBlockState");
            field.setAccessible(true);
            field.set(this, state);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void setTicksInGround(int ticks) {
        try {
            Field field = EntityAbstractSummonedSword.class.getDeclaredField("ticksInGround");
            field.setAccessible(true);
            field.setInt(this, ticks);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void setAlreadyHits(IntOpenHashSet value) {
        try {
            Field f = EntityAbstractSummonedSword.class.getDeclaredField("alreadyHits");
            f.setAccessible(true);
            f.set(this, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setTicksInAir(int value) {
        try {
            Field f = EntityAbstractSummonedSword.class.getDeclaredField("ticksInAir");
            f.setAccessible(true);
            f.set(this, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
