package tennouboshiuzume.mods.FantasyDesire.entity;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import mods.flammpfeil.slashblade.SlashBladeConfig;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraft.network.FriendlyByteBuf;
import org.joml.Vector3f;
import tennouboshiuzume.mods.FantasyDesire.entity.phantomsword.*;
import tennouboshiuzume.mods.FantasyDesire.utils.FDTargetSelector;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("removal")
public class EntityFDPhantomSword extends EntityAbstractSummonedSword implements IEntityAdditionalSpawnData {
    private static final int MAX_COLLISIONS_PER_TICK = 128;
    private static final int MAX_PARTICLE_STEPS = 64;
    private static final int MAX_TAIL_NODES = 256;

    public enum StandbyMode {
        NONE, PLAYER, WORLD
    }

    public enum MovingMode {
        NORMAL, SEEK, ADV_SEEK, PIERCING_SEEK
    }

    // 发射延迟
    private static final EntityDataAccessor<Integer> DELAY_TICKS = SynchedEntityData
            .defineId(EntityFDPhantomSword.class, EntityDataSerializers.INT);
    // 追踪行为延迟，大于发射延迟时控制飞行行为，小于发射延迟时控制待命行为
    private static final EntityDataAccessor<Integer> SEEK_DELAY = SynchedEntityData.defineId(EntityFDPhantomSword.class,
            EntityDataSerializers.INT);
    // 大小缩放
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(EntityFDPhantomSword.class,
            EntityDataSerializers.FLOAT);
    // 目标ID
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(EntityFDPhantomSword.class,
            EntityDataSerializers.INT);
    // 待命行为模式：绑定于玩家/绑定于世界 PLAYER/WORLD
    private static final EntityDataAccessor<Byte> STANDBY_MODE = SynchedEntityData
            .defineId(EntityFDPhantomSword.class, EntityDataSerializers.BYTE);
    // 发射后行为模式：追踪/直射 NORMAL/SEEK/ADV_SEEK
    private static final EntityDataAccessor<Byte> MOVING_MODE = SynchedEntityData.defineId(EntityFDPhantomSword.class,
            EntityDataSerializers.BYTE);
    // 待命固定朝向 (无论是玩家还是世界)
    private static final EntityDataAccessor<Float> STANDBY_YAW = SynchedEntityData.defineId(EntityFDPhantomSword.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STANDBY_PITCH = SynchedEntityData
            .defineId(EntityFDPhantomSword.class, EntityDataSerializers.FLOAT);
    // 飞行粒子
    private static final EntityDataAccessor<String> PARTICLE_TYPES = SynchedEntityData
            .defineId(EntityFDPhantomSword.class, EntityDataSerializers.STRING);
    // 向量偏移量，仅用于绑定玩家时
    private static final EntityDataAccessor<Vector3f> OFFSET = SynchedEntityData.defineId(EntityFDPhantomSword.class,
            EntityDataSerializers.VECTOR3);
    // 中心向量偏移加值
    private static final EntityDataAccessor<Vector3f> CENTER_OFFSET = SynchedEntityData
            .defineId(EntityFDPhantomSword.class, EntityDataSerializers.VECTOR3);
    // 是否已发射
    private static final EntityDataAccessor<Boolean> IT_FIRED = SynchedEntityData.defineId(EntityFDPhantomSword.class,
            EntityDataSerializers.BOOLEAN);
    // 发射速度
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(EntityFDPhantomSword.class,
            EntityDataSerializers.FLOAT);
    // 爆炸半径
    private static final EntityDataAccessor<Float> EXP_RADIUS = SynchedEntityData.defineId(EntityFDPhantomSword.class,
            EntityDataSerializers.FLOAT);
    // 伤害类型
    private static final EntityDataAccessor<String> DAMAGE_TYPE = SynchedEntityData.defineId(EntityFDPhantomSword.class,
            EntityDataSerializers.STRING);
    // 是否可多次命中
    private static final EntityDataAccessor<Boolean> MULTIPLE_HIT = SynchedEntityData
            .defineId(EntityFDPhantomSword.class, EntityDataSerializers.BOOLEAN);
    // 是否触发击中事件，防止某些效果造成循环
    private static final EntityDataAccessor<Boolean> NO_EVENT = SynchedEntityData.defineId(EntityFDPhantomSword.class,
            EntityDataSerializers.BOOLEAN);
    // 拖尾开关
    private static final EntityDataAccessor<Boolean> HAS_TAIL = SynchedEntityData.defineId(EntityFDPhantomSword.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FORCE_TAIL = SynchedEntityData.defineId(EntityFDPhantomSword.class,
            EntityDataSerializers.BOOLEAN);
    // 追踪转向角度
    private static final EntityDataAccessor<Float> SEEK_ANGLE = SynchedEntityData.defineId(EntityFDPhantomSword.class,
            EntityDataSerializers.FLOAT);
    // 拖尾节点数
    private static final EntityDataAccessor<Integer> TAIL_NODES = SynchedEntityData.defineId(EntityFDPhantomSword.class,
            EntityDataSerializers.INT);
    // 落地后存活时间
    private static final EntityDataAccessor<Integer> GROUND_LIFESPAN = SynchedEntityData.defineId(
            EntityFDPhantomSword.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> BINDING_TYPE = SynchedEntityData.defineId(
            EntityFDPhantomSword.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> FLIGHT_TYPE = SynchedEntityData.defineId(
            EntityFDPhantomSword.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> INACCURACY = SynchedEntityData.defineId(
            EntityFDPhantomSword.class, EntityDataSerializers.FLOAT);

    protected boolean inited = false;
    protected boolean isSeeking = false;
    protected SoundEvent fireSound = null;
    protected float fireSoundVolume = 1;
    protected float fireSoundRate = 1;

    // 本 tick 的查询去重与父类的整段飞行穿透记录分开，取消事件也必须推进查询。
    private final IntOpenHashSet tracedEntities = new IntOpenHashSet();
    private boolean bursting;
    // 客户端按同一年龄预测发射；不改写服务端同步的 IT_FIRED。
    private boolean predictedFired;
    @Nullable
    private UUID pendingTargetUUID;
    private ResourceLocation activeBindingType;
    private String loadedBindingId;
    private SwordStandbyBinding standbyBinding;
    private ResourceLocation activeFlightType;
    private String loadedFlightId;
    private SwordFlightBehavior flightBehavior;

    @OnlyIn(Dist.CLIENT)
    private final Deque<Vec3> trailPositions = new ArrayDeque<>();

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
        this.entityData.define(STANDBY_MODE, (byte) StandbyMode.WORLD.ordinal());// “PLAYER” or "WORLD"
        this.entityData.define(MOVING_MODE, (byte) MovingMode.NORMAL.ordinal());// "NORMAL" or "SEEK"
        this.entityData.define(STANDBY_YAW, 0f);
        this.entityData.define(STANDBY_PITCH, 0f);
        this.entityData.define(PARTICLE_TYPES, "Null");
        this.entityData.define(OFFSET, Vec3.ZERO.toVector3f());
        this.entityData.define(CENTER_OFFSET, Vec3.ZERO.toVector3f());
        this.entityData.define(IT_FIRED, false);
        this.entityData.define(MULTIPLE_HIT, false);
        this.entityData.define(SPEED, 3f);
        this.entityData.define(EXP_RADIUS, 0f);
        this.entityData.define(DAMAGE_TYPE, "");
        this.entityData.define(NO_EVENT, false);
        this.entityData.define(HAS_TAIL, false);
        this.entityData.define(FORCE_TAIL, false);
        this.entityData.define(SEEK_ANGLE, 18.0f);
        this.entityData.define(TAIL_NODES, 8);
        this.entityData.define(GROUND_LIFESPAN, 100);
        this.entityData.define(BINDING_TYPE, PhantomSwordBehaviors.WORLD.toString());
        this.entityData.define(FLIGHT_TYPE, PhantomSwordBehaviors.STRAIGHT.toString());
        this.entityData.define(INACCURACY, 0f);
    }

    @Override
    public void tick() {
        // 本类替换父类飞行流程，仅补齐 Entity 基础更新，避免 super.tick() 重复移动、碰撞。
        // tickCount 和旧坐标由 Level 更新；这里不能再递增年龄。
        this.baseTick();
        if (this.isRemoved())
            return;
        if (pendingTargetUUID != null && this.level() instanceof ServerLevel serverLevel) {
            Entity target = serverLevel.getEntity(pendingTargetUUID);
            if (target != null)
                setTargetId(target.getId());
        }
        if (!this.level().isClientSide() && (getShooter() == null || !getShooter().isAlive())) {
            if (tickCount > 20)
                remove(RemovalReason.DISCARDED);
            return;
        }
        // 0 tick初始化
        if (!inited) {
            tryInit();
            if (!inited)
                return;
        }
        boolean launchedThisTick = false;
        // 每种绑定都先更新当 tick 姿态，再检查是否发射，零延迟也走同一入口。
        if (!getFired()) {
            updateStandby(false);
            if (getStandbyBinding().autoLaunch() && getOwner() != null
                    && tickCount >= getDelayTicks()) {
                fire();
                launchedThisTick = true;
            }
        }
        if (getFired()) {
            customEffectFired();
            if (this.isRemoved())
                return;
            isSeeking = false;
            if (!launchedThisTick && tickCount > getSeekDelay() && !getInGround() && getHitEntity() == null) {
                Vec3 velocity = getFlightBehavior().steer(this);
                if (this.isRemoved())
                    return;
                if (velocity != null) {
                    setDeltaMovement(velocity);
                    setYRot((float) Math.toDegrees(Math.atan2(velocity.x, velocity.z)));
                    setXRot((float) Math.toDegrees(Math.atan2(velocity.y, velocity.horizontalDistance())));
                    isSeeking = true;
                }
            }
            flyticking();
            if (this.isRemoved())
                return;
        }
        // 客户端渲染尾迹记录
        if (this.level().isClientSide() && getHitEntity() == null && getHasTail() && (getFired() || getForceTail())) {
            Vec3 pos = this.position();
            trailPositions.addFirst(pos);
            while (trailPositions.size() > getTailNodes()) {
                trailPositions.removeLast();
            }
        } else if (this.level().isClientSide()) {
            trailPositions.clear();
        }
        // 常驻播放粒子
        if (!getInGround() && (getPierce() > 0 || getHitEntity() == null))
            playparticle();

        // 落地后倒计时消失
        if (!this.level().isClientSide() && this.getInGround()) {
            int currentLifespan = this.getGroundLifespan() - 1;
            this.setGroundLifespan(currentLifespan);
            if (currentLifespan <= 0) {
                this.discard();
            }
        }
    }

    public void customEffectFired() {
        // 占位用，后续子类可重写并且插入Fired段
    }

    @Override
    public void burst() {
        // 命中、爆炸与子类回调可能在同一 tick 连续请求碎裂。
        if (!this.isRemoved() && !bursting) {
            bursting = true;
            super.burst();
        }
    }

    protected void playparticle() {
        if (this.level().isClientSide())
            return;
        ParticleOptions particle = getParticleType();
        if (particle == null)
            return;

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
        int steps = Mth.clamp((int) getSpeed(), 1, MAX_PARTICLE_STEPS);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double x = Mth.lerp(t, px, cx);
            double y = Mth.lerp(t, py, cy);
            double z = Mth.lerp(t, pz, cz);

            sl.sendParticles(
                    particle,
                    x, y, z,
                    1,
                    0.05 * getScale(),
                    0.05 * getScale(),
                    0.05 * getScale(),
                    0.05 * getScale());
        }
    }

    @Override
    public void setHitEntity(Entity hit) {
        if (hit == null || hit == this || this.isRemoved())
            return;
        super.setHitEntity(hit);
        // 插入后只随目标平移；保留命中朝向，不再把残余飞行速度传给客户端。
        this.setDeltaMovement(Vec3.ZERO);
        this.isSeeking = false;
        this.hasImpulse = true;
    }

    // 刺入实体
    protected void stabInEntity(Entity hit) {
        if (!hit.isAlive()) {
            this.burst();
            return;
        }
        this.setPos(
                hit.getX(),
                hit.getY() + hit.getEyeHeight() * 0.5F,
                hit.getZ());
        this.setDeltaMovement(Vec3.ZERO);
        if (!this.level().isClientSide()) {
            int delay = this.getDelay() - 1;
            this.setDelay(delay);
            if (delay < 0)
                this.burst();
        }
    }

    private void flyticking() {
        if (this.getHitEntity() != null) {
            stabInEntity(this.getHitEntity());
        } else {
            boolean disallowedHitBlock = this.isNoClip();
            BlockPos blockpos = this.blockPosition();
            BlockState blockstate = this.level().getBlockState(blockpos);
            if (!blockstate.isAir() && !disallowedHitBlock) {
                VoxelShape voxelshape = blockstate.getCollisionShape(this.level(), blockpos);
                if (!voxelshape.isEmpty()) {
                    for (AABB axisalignedbb : voxelshape.toAabbs()) {
                        if (axisalignedbb.move(blockpos).contains(new Vec3(this.getX(), this.getY(), this.getZ()))) {
                            this.setInGround(true);
                            break;
                        }
                    }
                }
            }
            if (this.isInWaterOrRain()) {
                this.clearFire();
            }

            if (getInGround() && !disallowedHitBlock) {
                if (getInBlockState() != blockstate && this.level().noCollision(this.getBoundingBox().inflate(0.06D))) {
                    // 插入的方块被移除。
                    this.burst();
                } else if (!this.level().isClientSide()) {
                    // 落地剩余寿命统一由 tick() 管理，不再叠加父类固定 100 tick 的回收。
                    this.setTicksInGround(getTicksInGround() + 1);
                }
            } else {
                // process pose
                Vec3 motionVec = this.getDeltaMovement();
                if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
                    float f = Mth.sqrt((float) motionVec.horizontalDistanceSqr());
                    this.setYRot((float) (Mth.atan2(motionVec.x, motionVec.z) * (double) (180F / (float) Math.PI)));
                    this.setXRot((float) (Mth.atan2(motionVec.y, f) * (double) (180F / (float) Math.PI)));
                    this.yRotO = this.getYRot();
                    this.xRotO = this.getXRot();
                }

                // process inAir
                this.setTicksInAir(getTicksInAir() + 1);
                Vec3 positionVec = this.position();
                Vec3 movedVec = positionVec.add(motionVec);
                BlockHitResult blockHit = this.level().clip(
                        new ClipContext(positionVec, movedVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                                this));
                if (!disallowedHitBlock && blockHit.getType() != HitResult.Type.MISS) {
                    movedVec = blockHit.getLocation();
                }

                tracedEntities.clear();
                for (int attempts = 0; this.isAlive() && attempts < MAX_COLLISIONS_PER_TICK; attempts++) {
                    EntityHitResult entityraytraceresult = this.getRayTrace(positionVec, movedVec);
                    if (entityraytraceresult == null)
                        break;
                    if (!tracedEntities.add(entityraytraceresult.getEntity().getId())) {
                        // 子类可能重写射线谓词而不维护父类命中集合，不能反复处理同一目标。
                        break;
                    }
                    Entity entity = entityraytraceresult.getEntity();
                    if (!getFlightBehavior().impacts().canHit(this, entity)) {
                        continue;
                    }
                    if (!processImpact(entityraytraceresult))
                        continue;
                    if (this.isRemoved() || this.getHitEntity() != null)
                        return;
                    // 换目标后留在接触点，下 tick 再沿新轨迹飞行，不能继续扫描旧线段。
                    if (getFlightBehavior().impacts().stopAtContact())
                        return;
                    if (!getFlightBehavior().impacts().continueSweep(this)) {
                        break;
                    }
                }
                // 方块结果独立保留，即使实体查询去重退出或耗尽预算，也不能直接穿过墙壁。
                if (!disallowedHitBlock && blockHit.getType() == HitResult.Type.BLOCK)
                    processImpact(blockHit);
                if (this.isRemoved())
                    return;

                motionVec = this.getDeltaMovement();
                double mx = motionVec.x;
                double my = motionVec.y;
                double mz = motionVec.z;
                if (this.getIsCritical()) {
                    for (int i = 0; i < 4; ++i) {
                        this.level().addParticle(ParticleTypes.CRIT, this.getX() + mx * (double) i / 4.0D,
                                this.getY() + my * (double) i / 4.0D, this.getZ() + mz * (double) i / 4.0D, -mx,
                                -my + 0.2D,
                                -mz);
                    }
                }

                // 插墙回调调整了起点和位移；穿透命中后仍只推进原始飞行距离。
                this.setPos((getInGround() ? this.position() : positionVec).add(motionVec));
                float f4 = Mth.sqrt((float) motionVec.horizontalDistanceSqr());
                // if (disallowedHitBlock) {
                // this.setYRot((float) (Mth.atan2(-mx, -mz) * (double) (180F / (float)
                // Math.PI)));
                // } else {
                this.setYRot((float) (Mth.atan2(mx, mz) * (double) (180F / (float) Math.PI)));
                // }

                for (this.setXRot((float) (Mth.atan2(my, f4) * (double) (180F / (float) Math.PI))); this.getXRot()
                        - this.xRotO < -180.0F; this.xRotO -= 360.0F) {
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
                        this.level().addParticle(ParticleTypes.BUBBLE, this.getX() - mx * 0.25D,
                                this.getY() - my * 0.25D,
                                this.getZ() - mz * 0.25D, mx, my, mz);
                    }
                }

                this.setDeltaMovement(motionVec.scale(f1));
                if (!this.isNoGravity() && !disallowedHitBlock) {
                    Vec3 vec3d3 = this.getDeltaMovement();
                    this.setDeltaMovement(vec3d3.x, vec3d3.y - (double) 0.05F, vec3d3.z);
                }

                // this.setPosition(this.getPosX(), this.getPosY(), this.getPosZ());
                this.checkInsideBlocks();
            }
            if (!this.level().isClientSide() && !getInGround() && getDelay() < this.tickCount) {
                this.remove(RemovalReason.DISCARDED);
            }
        }
    }

    private boolean processImpact(HitResult hit) {
        if (this.isRemoved() || net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hit))
            return false;
        if (this.isRemoved())
            return false;
        if (!this.level().isClientSide()) {
            if (hit instanceof EntityHitResult)
                this.setPos(hit.getLocation());
            this.onHit(hit);
        } else if (hit instanceof BlockHitResult blockHit) {
            // 客户端只预测插墙姿态；伤害、命中事件及子类技能回调由服务端执行。
            super.onHitBlock(blockHit);
        } else if (getFlightBehavior().impacts().stopAtContact()) {
            this.setPos(hit.getLocation());
        }
        this.hasImpulse = true;
        return true;
    }

    @Nullable
    @Override
    protected EntityHitResult getRayTrace(Vec3 start, Vec3 end) {
        IntOpenHashSet alreadyHits = getAlreadyHits();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(this.level(), this, start, end,
                this.getBoundingBox().move(start.subtract(this.position())).expandTowards(end.subtract(start))
                        .inflate(1.0D),
                entity -> entity.canBeHitByProjectile() && !entity.isSpectator()
                        && (entity != this.getShooter() || getTicksInAir() >= 5)
                        && !tracedEntities.contains(entity.getId())
                        && (alreadyHits == null || !alreadyHits.contains(entity.getId()))
                        && getFlightBehavior().impacts().canHit(this, entity));
        if (hit == null)
            return null;
        // 此版 ProjectileUtil 只返回目标脚底位置，需用相同的 0.3 膨胀量恢复真实接触点。
        Vec3 contact = hit.getEntity().getBoundingBox().inflate(0.3F).clip(start, end).orElse(hit.getLocation());
        return new EntityHitResult(hit.getEntity(), contact);
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        if (this.level().isClientSide() || this.isRemoved())
            return;
        Entity targetEntity = entityHitResult.getEntity();
        SwordImpactPolicy impactPolicy = getFlightBehavior().impacts();
        if (!impactPolicy.beginHit(this, targetEntity))
            return;
        if (!this.getEntityData().get(NO_EVENT)) {
            SlashBladeEvent.SummonedSwordOnHitEntityEvent event = new SlashBladeEvent.SummonedSwordOnHitEntityEvent(
                    this, targetEntity);
            MinecraftForge.EVENT_BUS.post(event);
            if (this.isRemoved())
                return;
        }
        int i = Mth.ceil(this.getDamage());
        if (this.getIsCritical()) {
            i += this.random.nextInt(i / 2 + 2);
        }

        Entity shooter = this.getShooter();
        DamageSource damagesource;
        String typeStr = this.getDamageType();
        Entity causingEntity = shooter != null ? shooter : this;
        damagesource = this.damageSources().indirectMagic(this, causingEntity);
        ResourceLocation damageTypeId = ResourceLocation.tryParse(typeStr);
        if (damageTypeId != null) {
            damagesource = this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, damageTypeId))
                    .map(holder -> new DamageSource(holder, this, causingEntity)).orElse(damagesource);
        }

        if (shooter instanceof LivingEntity) {
            Entity hits = targetEntity;
            if (targetEntity instanceof PartEntity) {
                hits = ((PartEntity) targetEntity).getParent();
            }

            ((LivingEntity) shooter).setLastHurtMob(hits);
        }

        int fireTime = targetEntity.getRemainingFireTicks();
        if (this.isOnFire() && !(targetEntity instanceof EnderMan)) {
            targetEntity.setSecondsOnFire(5);
        }

        targetEntity.invulnerableTime = 0;
        float scale = 1.0F;
        if (shooter instanceof LivingEntity living) {
            scale = (float) ((double) AttackManager.getSlashBladeDamageScale(living)
                    * (Double) SlashBladeConfig.SLASHBLADE_DAMAGE_MULTIPLIER.get());
        }

        float damageValue = (float) i * scale;
        applyDamage(targetEntity, damagesource, damageValue, shooter, fireTime);
        if (this.entityData.get(EXP_RADIUS) > 0) {
            targetEntity.invulnerableTime = 0;
            doExplosive();
            this.burst();
            targetEntity.invulnerableTime = 0;
        }
        if (!this.isRemoved())
            impactPolicy.afterHit(this, targetEntity);
    }

    protected void doExplosive() {
        if (!this.level().isClientSide()) {
            this.level().explode(this.getShooter(), this.getX(), this.getY(), this.getZ(),
                    this.entityData.get(EXP_RADIUS), Level.ExplosionInteraction.NONE);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult blockraytraceresult) {
        super.onHitBlock(blockraytraceresult);
        if (!this.level().isClientSide() && this.entityData.get(EXP_RADIUS) > 0) {
            this.setPos(blockraytraceresult.getLocation());
            doExplosive();
            this.burst();
        }
    }

    @Nullable
    public LivingEntity getTargetEntity() {
        int id = this.entityData.get(TARGET_ID);
        if (id == -1)
            return null;
        if (this.level().getEntity(id) instanceof LivingEntity target) {
            return target;
        }
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FDAge", this.tickCount);
        tag.putInt("DelayTicks", this.entityData.get(DELAY_TICKS));
        tag.putInt("SeekDelay", this.entityData.get(SEEK_DELAY));
        tag.putFloat("Scale", this.entityData.get(SCALE));
        // 网络实体 ID 只在当前世界会话有效，存档使用 UUID。
        Entity target = this.level().getEntity(getTargetId());
        if (target != null)
            tag.putUUID("TargetUUID", target.getUUID());
        else if (pendingTargetUUID != null)
            tag.putUUID("TargetUUID", pendingTargetUUID);
        tag.putByte("StandbyMode", this.entityData.get(STANDBY_MODE));
        tag.putByte("MovingMode", this.entityData.get(MOVING_MODE));
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
        tag.putFloat("ExpRadius", this.entityData.get(EXP_RADIUS));
        tag.putString("DamageType", this.entityData.get(DAMAGE_TYPE));
        tag.putBoolean("NoEvent", this.entityData.get(NO_EVENT));
        tag.putBoolean("HasTail", this.entityData.get(HAS_TAIL));
        tag.putBoolean("ForceTail", this.entityData.get(FORCE_TAIL));
        tag.putFloat("SeekAngle", this.entityData.get(SEEK_ANGLE));
        tag.putInt("TailNodes", this.entityData.get(TAIL_NODES));
        tag.putInt("GroundLifespan", this.entityData.get(GROUND_LIFESPAN));
        tag.putString("BindingType", getBindingType().toString());
        tag.putString("FlightType", getFlightType().toString());
        tag.put("FlightState", getFlightBehavior().save());
        tag.putFloat("Inaccuracy", getInaccuracy());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("FDAge", Tag.TAG_ANY_NUMERIC))
            this.tickCount = Math.max(0, tag.getInt("FDAge"));
        if (tag.contains("DelayTicks", Tag.TAG_ANY_NUMERIC))
            setDelayTicks(tag.getInt("DelayTicks"));
        if (tag.contains("SeekDelay", Tag.TAG_ANY_NUMERIC))
            setSeekDelay(tag.getInt("SeekDelay"));
        if (tag.contains("Scale", Tag.TAG_ANY_NUMERIC))
            setScale(tag.getFloat("Scale"));
        setTargetId(-1);
        pendingTargetUUID = tag.hasUUID("TargetUUID") ? tag.getUUID("TargetUUID") : null;
        if (tag.contains("StandbyMode", 99)) {
            this.entityData.set(STANDBY_MODE, tag.getByte("StandbyMode"));
        } else if (tag.contains("StandbyMode", 8)) {
            try {
                this.entityData.set(STANDBY_MODE, (byte) StandbyMode.valueOf(tag.getString("StandbyMode")).ordinal());
            } catch (Exception e) {
                this.entityData.set(STANDBY_MODE, (byte) StandbyMode.WORLD.ordinal());
            }
        }
        if (tag.contains("MovingMode", 99)) {
            this.entityData.set(MOVING_MODE, tag.getByte("MovingMode"));
        } else if (tag.contains("MovingMode", 8)) {
            try {
                this.entityData.set(MOVING_MODE, (byte) MovingMode.valueOf(tag.getString("MovingMode")).ordinal());
            } catch (Exception e) {
                this.entityData.set(MOVING_MODE, (byte) MovingMode.NORMAL.ordinal());
            }
        }
        setStandbyYawPitch(tag.contains("StandbyYaw", Tag.TAG_ANY_NUMERIC) ? tag.getFloat("StandbyYaw")
                : this.entityData.get(STANDBY_YAW),
                tag.contains("StandbyPitch", Tag.TAG_ANY_NUMERIC) ? tag.getFloat("StandbyPitch")
                        : this.entityData.get(STANDBY_PITCH));
        if (tag.contains("ParticleTypes", Tag.TAG_STRING))
            this.entityData.set(PARTICLE_TYPES, tag.getString("ParticleTypes"));
        if (tag.contains("Offset", Tag.TAG_COMPOUND)) {
            CompoundTag offsetTag = tag.getCompound("Offset");
            Vector3f offset = new Vector3f(
                    offsetTag.getFloat("X"),
                    offsetTag.getFloat("Y"),
                    offsetTag.getFloat("Z"));
            setOffset(new Vec3(offset));
        }
        if (tag.contains("CenterOffset", Tag.TAG_COMPOUND)) {
            CompoundTag offsetTag = tag.getCompound("CenterOffset");
            Vector3f CenterOffset = new Vector3f(
                    offsetTag.getFloat("X"),
                    offsetTag.getFloat("Y"),
                    offsetTag.getFloat("Z"));
            setCenterOffset(new Vec3(CenterOffset));
        }
        if (tag.contains("ItFired", Tag.TAG_ANY_NUMERIC))
            this.entityData.set(IT_FIRED, tag.getBoolean("ItFired"));
        if (tag.contains("MultipleHit", Tag.TAG_ANY_NUMERIC))
            setMultipleHit(tag.getBoolean("MultipleHit"));
        if (tag.contains("Speed", Tag.TAG_ANY_NUMERIC))
            setSpeed(tag.getFloat("Speed"));
        if (tag.contains("ExpRadius", Tag.TAG_ANY_NUMERIC))
            setExpRadius(tag.getFloat("ExpRadius"));
        if (tag.contains("DamageType", Tag.TAG_STRING))
            setDamageType(tag.getString("DamageType"));
        if (tag.contains("NoEvent", Tag.TAG_ANY_NUMERIC))
            setNoEvent(tag.getBoolean("NoEvent"));
        if (tag.contains("HasTail", Tag.TAG_ANY_NUMERIC))
            setHasTail(tag.getBoolean("HasTail"));
        if (tag.contains("ForceTail", Tag.TAG_ANY_NUMERIC))
            ForceTail(tag.getBoolean("ForceTail"));
        if (tag.contains("SeekAngle", Tag.TAG_ANY_NUMERIC))
            setSeekAngle(tag.getFloat("SeekAngle"));
        if (tag.contains("TailNodes", Tag.TAG_ANY_NUMERIC))
            setTailNodes(tag.getInt("TailNodes"));
        if (tag.contains("GroundLifespan", Tag.TAG_ANY_NUMERIC))
            setGroundLifespan(tag.getInt("GroundLifespan"));
        // 新存档优先使用稳定 ID；旧枚举只在兼容入口转换一次。
        if (tag.contains("BindingType", Tag.TAG_STRING))
            setBindingType(
                    PhantomSwordBehaviors.BINDINGS.resolve(tag.getString("BindingType"), PhantomSwordBehaviors.WORLD));
        else
            setStandbyMode(getStandbyMode());
        if (tag.contains("FlightType", Tag.TAG_STRING)) {
            ResourceLocation id = PhantomSwordBehaviors.FLIGHTS.resolve(tag.getString("FlightType"),
                    PhantomSwordBehaviors.STRAIGHT);
            setFlightBehavior(id, tag.getCompound("FlightState"));
        } else {
            setMovingMode(getMovingMode());
        }
        if (tag.contains("Inaccuracy", Tag.TAG_ANY_NUMERIC))
            setInaccuracy(tag.getFloat("Inaccuracy"));
        // 已加载的世界位置和旋转由 Entity.load 恢复，不再重新套用出生朝向。
        inited = getFired() || (getStandbyMode() != StandbyMode.PLAYER && tag.contains("Rotation", Tag.TAG_LIST));
    }

    private void updateStandby(boolean initialize) {
        SwordStandbyBinding.Pose pose = getStandbyBinding().resolve(this);
        if (pose == null)
            return;
        setPos(pose.position());
        setDeltaMovement(pose.motion());
        if (initialize || pose.followsRotation())
            setRot(pose.yaw(), pose.pitch());
        LivingEntity target = getTargetEntity();
        if (!initialize && target != null && target.isAlive() && tickCount > getSeekDelay()) {
            Vec3 direction = target.getBoundingBox().getCenter().subtract(position());
            float yaw = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
            float pitch = (float) Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance()));
            // 主人绑定保留既有渐进瞄准手感；世界绑定从上一 tick 的朝向继续转动。
            float step = pose.followsRotation() ? 5f * tickCount : 5f;
            setRot(Mth.approachDegrees(getYRot(), yaw, step), Mth.approachDegrees(getXRot(), pitch, step));
        }
        if (initialize) {
            yRotO = getYRot();
            xRotO = getXRot();
        }
    }

    private void fire() {
        Vec3 dir = Vec3.directionFromRotation(-this.getXRot(), -this.getYRot());
        Vec3 velocity = SwordLaunch.velocity(getUUID(), dir, getSpeed(), getInaccuracy());
        // 与父类 shoot() 一样初始化朝向和落地计时，散布由两端共享的种子计算。
        this.setDeltaMovement(velocity);
        float horizontal = Mth.sqrt((float) velocity.horizontalDistanceSqr());
        this.setYRot((float) (Mth.atan2(velocity.x, velocity.z) * (double) (180F / (float) Math.PI)));
        this.setXRot((float) (Mth.atan2(velocity.y, horizontal) * (double) (180F / (float) Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.setTicksInGround(0);
        this.hasImpulse = true;
        if (!this.level().isClientSide())
            playFireSound(this.fireSoundVolume, this.fireSoundRate);
        this.setFired();
    }

    private void playFireSound(float volume, float pitch) {
        SoundEvent sound = fireSound;
        if (sound != null) {
            this.playSound(sound, volume, pitch);
        }
    }

    public void tryInit() {
        if (getFired() || !getStandbyBinding().autoLaunch()) {
            inited = true;
        } else if (getStandbyBinding().resolve(this) != null) {
            updateStandby(true);
            inited = true;
        }
    }

    public void tryUpdateTarget() {
        if (!this.level().isClientSide() && getShooter() instanceof LivingEntity shooter) {
            setTargetId(FDTargetSelector.getLockTarget(shooter)
                    .filter(entity -> entity instanceof LivingEntity && entity.isAlive() && entity != shooter)
                    .map(Entity::getId).orElse(-1));
        }
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
        this.entityData.set(DELAY_TICKS, Math.max(0, delay));
    }

    public void setExpRadius(float value) {
        this.entityData.set(EXP_RADIUS, nonNegativeFinite(value, 0f));
    }

    public float getExpRadius() {
        return this.entityData.get(EXP_RADIUS);
    }

    // 大小缩放
    public float getScale() {
        return this.entityData.get(SCALE);
    }

    public void setScale(float scale) {
        this.entityData.set(SCALE, nonNegativeFinite(scale, 1f));
    }

    // 目标ID
    public int getTargetId() {
        return this.entityData.get(TARGET_ID);
    }

    public void setTargetId(int id) {
        this.entityData.set(TARGET_ID, id);
        pendingTargetUUID = null;
    }

    // 待命行为模式：PLAYER / WORLD
    public StandbyMode getStandbyMode() {
        byte ordinal = this.entityData.get(STANDBY_MODE);
        if (ordinal >= 0 && ordinal < StandbyMode.values().length) {
            return StandbyMode.values()[ordinal];
        }
        return StandbyMode.WORLD;
    }

    public void setStandbyMode(StandbyMode mode) {
        setBindingType(switch (mode != null ? mode : StandbyMode.WORLD) {
            case NONE -> PhantomSwordBehaviors.NONE;
            case PLAYER -> PhantomSwordBehaviors.OWNER;
            case WORLD -> PhantomSwordBehaviors.WORLD;
        });
    }

    public void setStandbyMode(String modeStr) {
        try {
            setStandbyMode(StandbyMode.valueOf(modeStr));
        } catch (IllegalArgumentException | NullPointerException e) {
            setStandbyMode(StandbyMode.WORLD);
        }
    }

    // 发射后行为模式：NORMAL / TRACKING
    public MovingMode getMovingMode() {
        byte ordinal = this.entityData.get(MOVING_MODE);
        if (ordinal >= 0 && ordinal < MovingMode.values().length) {
            return MovingMode.values()[ordinal];
        }
        return MovingMode.NORMAL;
    }

    public void setMovingMode(MovingMode mode) {
        setFlightBehavior(switch (mode != null ? mode : MovingMode.NORMAL) {
            case NORMAL -> PhantomSwordBehaviors.STRAIGHT;
            case SEEK -> PhantomSwordBehaviors.HOMING;
            case ADV_SEEK -> PhantomSwordBehaviors.GUIDED;
            case PIERCING_SEEK -> PhantomSwordBehaviors.PIERCING_HOMING;
        }, new CompoundTag());
    }

    public void setMovingMode(String modeStr) {
        try {
            setMovingMode(MovingMode.valueOf(modeStr));
        } catch (IllegalArgumentException | NullPointerException e) {
            setMovingMode(MovingMode.NORMAL);
        }
    }

    public ResourceLocation getBindingType() {
        getStandbyBinding();
        return activeBindingType;
    }

    public void setBindingType(ResourceLocation type) {
        SwordStandbyBinding binding = PhantomSwordBehaviors.BINDINGS.create(type);
        entityData.set(BINDING_TYPE, type.toString());
        StandbyMode legacy = type.equals(PhantomSwordBehaviors.NONE) ? StandbyMode.NONE
                : type.equals(PhantomSwordBehaviors.OWNER) ? StandbyMode.PLAYER : StandbyMode.WORLD;
        entityData.set(STANDBY_MODE, (byte) legacy.ordinal());
        activeBindingType = type;
        loadedBindingId = type.toString();
        standbyBinding = binding;
        inited = false;
    }

    private SwordStandbyBinding getStandbyBinding() {
        String id = entityData.get(BINDING_TYPE);
        if (!id.equals(loadedBindingId)) {
            ResourceLocation type = PhantomSwordBehaviors.BINDINGS.resolve(id, PhantomSwordBehaviors.WORLD);
            standbyBinding = PhantomSwordBehaviors.BINDINGS.create(type);
            activeBindingType = type;
            loadedBindingId = id;
        }
        return standbyBinding;
    }

    public ResourceLocation getFlightType() {
        getFlightBehavior();
        return activeFlightType;
    }

    /** 配置与恢复共用类型工厂，但 load 不触发发射、命中或阶段进入效果。 */
    public void setFlightBehavior(ResourceLocation type, CompoundTag settings) {
        SwordFlightBehavior behavior = PhantomSwordBehaviors.FLIGHTS.create(type);
        behavior.load(settings.copy());
        entityData.set(FLIGHT_TYPE, type.toString());
        MovingMode legacy = type.equals(PhantomSwordBehaviors.HOMING) ? MovingMode.SEEK
                : type.equals(PhantomSwordBehaviors.GUIDED) ? MovingMode.ADV_SEEK
                        : (type.equals(PhantomSwordBehaviors.PIERCING_HOMING)
                                || type.equals(PhantomSwordBehaviors.PIERCING_CHAIN)) ? MovingMode.PIERCING_SEEK
                                : MovingMode.NORMAL;
        entityData.set(MOVING_MODE, (byte) legacy.ordinal());
        activeFlightType = type;
        loadedFlightId = type.toString();
        flightBehavior = behavior;
    }

    public SwordFlightBehavior getFlightBehavior() {
        String id = entityData.get(FLIGHT_TYPE);
        if (!id.equals(loadedFlightId)) {
            ResourceLocation type = PhantomSwordBehaviors.FLIGHTS.resolve(id, PhantomSwordBehaviors.STRAIGHT);
            flightBehavior = PhantomSwordBehaviors.FLIGHTS.create(type);
            activeFlightType = type;
            loadedFlightId = id;
        }
        return flightBehavior;
    }

    /** 总次数包含初始目标；此配置独立于父类 setPierce()。 */
    public void setPiercingHoming(int totalHits, float searchRadius) {
        CompoundTag settings = new CompoundTag();
        settings.putInt("RemainingHits", totalHits);
        settings.putFloat("SearchRadius", searchRadius);
        setFlightBehavior(PhantomSwordBehaviors.PIERCING_HOMING, settings);
    }

    public boolean hasPendingTarget() {
        return pendingTargetUUID != null;
    }

    public float getInaccuracy() {
        return entityData.get(INACCURACY);
    }

    /** 与父类 shoot() 同单位，0 表示无散布，不是角度。 */
    public void setInaccuracy(float inaccuracy) {
        entityData.set(INACCURACY, Mth.clamp(nonNegativeFinite(inaccuracy, 0), 0, 100));
    }

    // 待命固定朝向

    public float[] getStandbyYawPitch() {
        return new float[] {
                this.entityData.get(STANDBY_YAW),
                this.entityData.get(STANDBY_PITCH)
        };
    }

    public void setStandbyYawPitch(float yaw, float pitch) {
        this.entityData.set(STANDBY_YAW, Float.isFinite(yaw) ? Mth.wrapDegrees(yaw) : 0f);
        this.entityData.set(STANDBY_PITCH, Float.isFinite(pitch) ? Mth.wrapDegrees(pitch) : 0f);
    }

    // 发射后追踪延迟
    public int getSeekDelay() {
        return this.entityData.get(SEEK_DELAY);
    }

    public void setSeekDelay(int delay) {
        this.entityData.set(SEEK_DELAY, Math.max(0, delay));
    }

    // 飞行粒子
    public ParticleOptions getParticleType() {
        String id = this.entityData.get(PARTICLE_TYPES);
        if (id.equals("Null") || id.isBlank())
            return null;
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null)
            return null;
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.getOptional(location).orElse(null);
        if (type instanceof SimpleParticleType) {
            return (SimpleParticleType) type;
        }
        return null;
    }

    // 设置粒子效果
    public void setParticleType(ParticleType<?> type) {
        if (type == null) {
            this.entityData.set(PARTICLE_TYPES, "Null");
            return;
        }
        ResourceLocation id = BuiltInRegistries.PARTICLE_TYPE.getKey(type);
        if (id != null) {
            this.entityData.set(PARTICLE_TYPES, id.toString());
        }
    }

    // 设置发射音效
    public void setFireSound(SoundEvent sound, float volume, float rate) {
        this.fireSound = sound;
        this.fireSoundRate = rate;
        this.fireSoundVolume = volume;
    }

    // 设置偏移量
    public void setOffset(Vec3 offset) {
        this.entityData.set(OFFSET, finiteOffset(offset));
    }

    // 设置偏移中心
    public void setCenterOffset(Vec3 offset) {
        this.entityData.set(CENTER_OFFSET, finiteOffset(offset));
    }

    // 是否已发射
    public boolean getFired() {
        return this.getEntityData().get(IT_FIRED) || (this.level().isClientSide() && predictedFired);
    }

    public void setFired() {
        if (this.level().isClientSide()) {
            predictedFired = true;
            return;
        }
        if (!getFired())
            this.gameEvent(GameEvent.PROJECTILE_SHOOT, this.getOwner());
        this.getEntityData().set(IT_FIRED, true);
    }

    // 发射速度
    public float getSpeed() {
        return this.entityData.get(SPEED);
    }

    public void setSpeed(float speed) {
        this.entityData.set(SPEED, nonNegativeFinite(speed, 3f));
    }

    public void setMultipleHit(boolean value) {
        this.entityData.set(MULTIPLE_HIT, value);
    }

    public boolean getMultipleHit() {
        return this.entityData.get(MULTIPLE_HIT);
    }

    public void setNoEvent(boolean value) {
        this.entityData.set(NO_EVENT, value);
    }

    public void setHasTail(boolean value) {
        this.entityData.set(HAS_TAIL, value);
    }

    public boolean getHasTail() {
        return this.entityData.get(HAS_TAIL);
    }

    public void ForceTail(boolean value) {
        this.entityData.set(FORCE_TAIL, value);
    }

    public boolean getForceTail() {
        return this.entityData.get(FORCE_TAIL);
    }

    public boolean getNoEvent() {
        return this.entityData.get(NO_EVENT);
    }

    public void setSeekAngle(float angle) {
        this.entityData.set(SEEK_ANGLE, Mth.clamp(nonNegativeFinite(angle, 18f), 0f, 180f));
    }

    public float getSeekAngle() {
        return this.entityData.get(SEEK_ANGLE);
    }

    public void setTailNodes(int nodes) {
        this.entityData.set(TAIL_NODES, Mth.clamp(nodes, 0, MAX_TAIL_NODES));
    }

    public int getTailNodes() {
        return this.entityData.get(TAIL_NODES);
    }

    public void setGroundLifespan(int lifespan) {
        this.entityData.set(GROUND_LIFESPAN, Math.max(0, lifespan));
    }

    public int getGroundLifespan() {
        return this.entityData.get(GROUND_LIFESPAN);
    }

    public void setDamageType(String type) {
        ResourceLocation id = type == null || type.isBlank() ? null : ResourceLocation.tryParse(type);
        this.entityData.set(DAMAGE_TYPE, id != null ? id.toString() : "");
    }

    private static float nonNegativeFinite(float value, float fallback) {
        return Float.isFinite(value) ? Math.max(0f, value) : fallback;
    }

    private static Vector3f finiteOffset(@Nullable Vec3 offset) {
        if (offset == null)
            return new Vector3f();
        Vector3f value = offset.toVector3f();
        return value.isFinite() ? value : new Vector3f();
    }

    public String getDamageType() {
        return this.entityData.get(DAMAGE_TYPE);
    }

    protected void applyDamage(Entity targetEntity, DamageSource damagesource, float damageValue, Entity shooter,
            int fireTime) {
        if (doHurt(targetEntity, damagesource, damageValue)) {
            Entity hits = targetEntity;
            if (targetEntity instanceof PartEntity) {
                hits = ((PartEntity) targetEntity).getParent();
            }

            if (hits instanceof LivingEntity targetLivingEntity) {
                StunManager.setStun(targetLivingEntity);
                if (!this.level().isClientSide() && getFlightBehavior().impacts().canEmbed(this)) {
                    this.setHitEntity(hits);
                }

                if (!this.level().isClientSide() && shooter instanceof LivingEntity) {
                    EnchantmentHelper.doPostHurtEffects(targetLivingEntity, shooter);
                    EnchantmentHelper.doPostDamageEffects((LivingEntity) shooter, targetLivingEntity);
                }

                this.affectEntity(targetLivingEntity, this.getPotionEffects(), 1.0);
                if (shooter != null && targetLivingEntity != shooter && targetLivingEntity instanceof Player
                        && shooter instanceof ServerPlayer serverPlayer) {
                    serverPlayer.playNotifySound(this.getHitEntityPlayerSound(), SoundSource.PLAYERS, 0.18F,
                            0.45F);
                }
            }

            this.playSound(this.getHitEntitySound(), 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
            if (getFlightBehavior().impacts().canEmbed(this)
                    && (this.getHitEntity() == null || !this.getHitEntity().isAlive())) {
                this.burst();
            }
        } else {
            targetEntity.setRemainingFireTicks(fireTime);
            setTicksInAir(0);
            if (!this.level().isClientSide())
                getFlightBehavior().impacts().damageRejected(this);
        }
    }

    protected boolean doHurt(Entity target, DamageSource damagesource, float damageValue) {
        return target.hurt(damagesource, damageValue);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        Entity owner = this.getOwner();
        buffer.writeInt(owner != null ? owner.getId() : -1);
        buffer.writeInt(this.tickCount);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        int ownerId = additionalData.readInt();
        if (ownerId != -1) {
            Entity owner = this.level().getEntity(ownerId);
            if (owner != null) {
                this.setOwner(owner);
            }
        }
        this.tickCount = additionalData.readInt();
    }

    //////////////////////////
    // 我讨厌反射 //
    //////////////////////////
    private static final Field IN_GROUND_FIELD;
    private static final Field IN_BLOCK_STATE_FIELD;
    private static final Field TICKS_IN_GROUND_FIELD;
    private static final Field ALREADY_HITS_FIELD;
    private static final Field TICKS_IN_AIR_FIELD;

    static {
        try {
            IN_GROUND_FIELD = EntityAbstractSummonedSword.class.getDeclaredField("inGround");
            IN_GROUND_FIELD.setAccessible(true);
            IN_BLOCK_STATE_FIELD = EntityAbstractSummonedSword.class.getDeclaredField("inBlockState");
            IN_BLOCK_STATE_FIELD.setAccessible(true);
            TICKS_IN_GROUND_FIELD = EntityAbstractSummonedSword.class.getDeclaredField("ticksInGround");
            TICKS_IN_GROUND_FIELD.setAccessible(true);
            ALREADY_HITS_FIELD = EntityAbstractSummonedSword.class.getDeclaredField("alreadyHits");
            ALREADY_HITS_FIELD.setAccessible(true);
            TICKS_IN_AIR_FIELD = EntityAbstractSummonedSword.class.getDeclaredField("ticksInAir");
            TICKS_IN_AIR_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    // 读取父类字段
    public boolean getInGround() {
        try {
            return IN_GROUND_FIELD.getBoolean(this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }
    }

    public BlockState getInBlockState() {
        try {
            return (BlockState) IN_BLOCK_STATE_FIELD.get(this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getTicksInGround() {
        try {
            return TICKS_IN_GROUND_FIELD.getInt(this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public IntOpenHashSet getAlreadyHits() {
        try {
            return (IntOpenHashSet) ALREADY_HITS_FIELD.get(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getTicksInAir() {
        try {
            return (int) TICKS_IN_AIR_FIELD.get(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 修改父类字段
    public void setInGround(boolean value) {
        try {
            IN_GROUND_FIELD.setBoolean(this, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void setInBlockState(BlockState state) {
        try {
            IN_BLOCK_STATE_FIELD.set(this, state);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void setTicksInGround(int ticks) {
        try {
            TICKS_IN_GROUND_FIELD.setInt(this, ticks);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void setAlreadyHits(IntOpenHashSet value) {
        try {
            ALREADY_HITS_FIELD.set(this, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setTicksInAir(int value) {
        try {
            TICKS_IN_AIR_FIELD.set(this, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public List<Vec3> getTrailPositions() {
        return new ArrayList<>(this.trailPositions);
    }
}
