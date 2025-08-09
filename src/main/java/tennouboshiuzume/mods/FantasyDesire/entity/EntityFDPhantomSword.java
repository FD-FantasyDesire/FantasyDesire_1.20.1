package tennouboshiuzume.mods.FantasyDesire.entity;

import com.mojang.math.Axis;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.init.FDEntitys;

import javax.annotation.Nullable;

public class EntityFDPhantomSword extends EntityAbstractSummonedSword {

    private static final EntityDataAccessor<Integer> DELAY_TICKS = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(EntityFDPhantomSword.class, EntityDataSerializers.INT);

    private Vec3 initialDirection = Vec3.ZERO;
    private boolean fired = false;

    public EntityFDPhantomSword(EntityType<? extends EntityFDPhantomSword> type, Level level) {
        super(type, level);
    }

    public EntityFDPhantomSword(Level level, LivingEntity owner, Vec3 initialDirection, int delayTicks, @Nullable LivingEntity target, float scale) {
        this(FDEntitys.FDPhantomSword.get(), level);
        this.setOwner(owner);
        this.initialDirection = initialDirection.normalize();
        this.entityData.set(DELAY_TICKS, delayTicks);
        this.entityData.set(SCALE, scale);
        if (target != null) {
            this.entityData.set(TARGET_ID, target.getId());
        } else {
            this.entityData.set(TARGET_ID, -1);
        }
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DELAY_TICKS, 0);
        this.entityData.define(SCALE, 1.0f);
        this.entityData.define(TARGET_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();

        int delay = this.entityData.get(DELAY_TICKS);
        if (!fired) {
            if (this.tickCount < delay) {
                // 延迟期间转向目标
                LivingEntity target = getTargetEntity();
                if (target != null && target.isAlive()) {
                    Vec3 toTarget = target.position().add(0, target.getBbHeight() / 2, 0).subtract(this.position()).normalize();
                    this.setYRot((float)(Mth.atan2(toTarget.x, toTarget.z) * Mth.RAD_TO_DEG));
                    this.setXRot((float)(Mth.atan2(toTarget.y, toTarget.horizontalDistance()) * Mth.RAD_TO_DEG));
                } else {
                    // 保持初始朝向
                    this.setYRot((float)(Mth.atan2(initialDirection.x, initialDirection.z) * Mth.RAD_TO_DEG));
                    this.setXRot((float)(Mth.atan2(initialDirection.y, initialDirection.horizontalDistance()) * Mth.RAD_TO_DEG));
                }
                // 静止
                this.setDeltaMovement(Vec3.ZERO);
            } else if (this.tickCount == delay) {
                // 发射
                Vec3 dir = Vec3.directionFromRotation(-this.getXRot(), -this.getYRot());
                this.shoot(dir.x, dir.y, dir.z, 1.5f, 0.0f);
                fired = true;
            }
        } else {
            // 发射后粒子效果
            this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
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

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Delay", this.entityData.get(DELAY_TICKS));
        tag.putFloat("Scale", this.entityData.get(SCALE));
        tag.putInt("TargetId", this.entityData.get(TARGET_ID));
        tag.put("InitialDir", this.newDoubleList(
                this.initialDirection.x,
                this.initialDirection.y,
                this.initialDirection.z
        ));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DELAY_TICKS, tag.getInt("Delay"));
        this.entityData.set(SCALE, tag.getFloat("Scale"));
        this.entityData.set(TARGET_ID, tag.getInt("TargetId"));
        if (tag.contains("InitialDir", 9)) { // 9 = ListTag
            ListTag list = tag.getList("InitialDir", 6); // 6 = DoubleTag
            if (list.size() == 3) {
                this.initialDirection = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
            }
        }
    }

    public float getScaleValue() {
        return this.entityData.get(SCALE);
    }
}
