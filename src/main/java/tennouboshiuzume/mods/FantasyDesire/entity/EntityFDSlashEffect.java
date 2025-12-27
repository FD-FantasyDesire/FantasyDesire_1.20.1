package tennouboshiuzume.mods.FantasyDesire.entity;

import com.mojang.math.Axis;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.event.handler.FallHandler;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import tennouboshiuzume.mods.FantasyDesire.utils.FDAttackManager;

import java.lang.reflect.Field;
import java.util.List;

public class EntityFDSlashEffect extends EntitySlashEffect {
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(EntityFDSlashEffect.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(EntityFDSlashEffect.class,
            EntityDataSerializers.INT);

    public EntityFDSlashEffect(EntityType<? extends EntitySlashEffect> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SCALE, 1.0F);
        this.entityData.define(LIFETIME, 10);
    }

    public void setScale(float scale) {
        this.entityData.set(SCALE, scale);
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    public void setLifetime(int lifetime) {
        this.entityData.set(LIFETIME, lifetime);
    }

    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Scale", this.getScale());
        tag.putInt("Lifetime", this.getLifetime());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Scale")) {
            this.setScale(tag.getFloat("Scale"));
        }
        if (tag.contains("Lifetime")) {
            this.setLifetime(tag.getInt("Lifetime"));
        }
    }

    @Override
    public void tick() {
        if (tickCount == 2) {
            handleSoundEffects();
        }

        if (tickCount % 2 == 0 || tickCount < 5) {
            handleParticlesAndCollision();
        }

        if (this.getShooter() != null) {
            handleAttack();
        }

        if (this.tickCount > getLifetime()) {
            this.discard();
        }
    }

    protected void handleSoundEffects() {
        if (!getMute()) {
            this.playSound(this.getSlashSound(), 0.80F, 0.625F + 0.1f * this.random.nextFloat());
        } else {
            this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.5F, 0.4F / (this.random.nextFloat() * 0.4F + 0.8F));
        }

        if (getIsCritical()) {
            this.playSound(getHitEntitySound(), 0.2F, 0.4F + 0.25f * this.random.nextFloat());
        }
    }

    protected void handleParticlesAndCollision() {
        Vec3 start = this.position();
        Vector4f normal = new Vector4f(1, 0, 0, 1);
        Vector4f dir = new Vector4f(0, 0, 1, 1);

        float progress = this.tickCount / (float) getLifetime();

        transformVectors(normal, dir, progress);

        Vec3 normal3d = new Vec3(normal.x(), normal.y(), normal.z());

        handleBlockCollision(start, normal3d);
        spawnCritParticles(start, normal, dir, normal3d);
    }

    protected void transformVectors(Vector4f normal, Vector4f dir, float progress) {
        Axis.YP.rotationDegrees(60 + this.getRotationOffset() - 200.0F * progress).transform(normal);
        Axis.ZP.rotationDegrees(this.getRotationRoll()).transform(normal);
        Axis.XP.rotationDegrees(this.getXRot()).transform(normal);
        Axis.YP.rotationDegrees(-this.getYRot()).transform(normal);

        Axis.YP.rotationDegrees(60 + this.getRotationOffset() - 200.0F * progress).transform(dir);
        Axis.ZP.rotationDegrees(this.getRotationRoll()).transform(dir);
        Axis.XP.rotationDegrees(this.getXRot()).transform(dir);
        Axis.YP.rotationDegrees(-this.getYRot()).transform(dir);
    }

    protected void handleBlockCollision(Vec3 start, Vec3 normal3d) {
        BlockHitResult rayResult = this.getCommandSenderWorld().clip(new ClipContext(start.add(normal3d.scale(1.5)),
                start.add(normal3d.scale(3)), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, null));

        if (getShooter() != null && !getShooter().isInWaterOrRain()
                && rayResult.getType() == HitResult.Type.BLOCK) {
            FallHandler.spawnLandingParticle(this, rayResult.getLocation(), normal3d, 3);
        }
    }

    protected void spawnCritParticles(Vec3 start, Vector4f normal, Vector4f dir, Vec3 normal3d) {
        if (IConcentrationRank.ConcentrationRanks.S.level < getRankCode().level) {
            Vec3 vec3 = start.add(normal3d.scale(this.getBaseSize() * 2.5));
            this.level().addParticle(ParticleTypes.CRIT, vec3.x(), vec3.y(), vec3.z(), dir.x() + normal.x(),
                    dir.y() + normal.y(), dir.z() + normal.z());
            float randScale = random.nextFloat() + 0.5f;
            vec3 = vec3.add(dir.x() * randScale, dir.y() * randScale, dir.z() * randScale);
            this.level().addParticle(ParticleTypes.CRIT, vec3.x(), vec3.y(), vec3.z(), dir.x() + normal.x(),
                    dir.y() + normal.y(), dir.z() + normal.z());
        }
    }

    protected void handleAttack() {
        if (this.tickCount % 2 == 0) {
            boolean forceHit = true;
            List<Entity> hits;
            if (!getIndirect() && getShooter() instanceof LivingEntity shooter) {
                float ratio = (float) getDamage() * (getIsCritical() ? 1.1f : 1.0f);
                hits = FDAttackManager.areaAttack(shooter, this.getAction().action, this.position(),
                        4.0 * this.getScale(), ratio,
                        forceHit, false, true,
                        getAlreadyHits(), null);
            } else {
                hits = AttackManager.areaAttack(this, this.getAction().action, 4.0 * this.getScale(), forceHit,
                        false,
                        getAlreadyHits());
            }

            if (!this.doCycleHit()) {
                getAlreadyHits().addAll(hits);
            }
        }
    }

    private static final Field ALREADY_HITS_FIELD;
    private static final Field ACTION_FIELD;
    private static final Field DAMAGE_FIELD;

    static {
        try {
            ALREADY_HITS_FIELD = EntitySlashEffect.class.getDeclaredField("alreadyHits");
            ALREADY_HITS_FIELD.setAccessible(true);

            ACTION_FIELD = EntitySlashEffect.class.getDeclaredField("action");
            ACTION_FIELD.setAccessible(true);

            DAMAGE_FIELD = EntitySlashEffect.class.getDeclaredField("damage");
            DAMAGE_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Entity> getAlreadyHits() {
        try {
            return (List<Entity>) ALREADY_HITS_FIELD.get(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setAlreadyHits(List<Entity> value) {
        try {
            ALREADY_HITS_FIELD.set(this, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public KnockBacks getAction() {
        try {
            return (KnockBacks) ACTION_FIELD.get(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public double getDamage() {
        try {
            return DAMAGE_FIELD.getDouble(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
