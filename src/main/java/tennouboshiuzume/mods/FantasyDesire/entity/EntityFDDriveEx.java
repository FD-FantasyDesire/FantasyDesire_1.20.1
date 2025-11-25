package tennouboshiuzume.mods.FantasyDesire.entity;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank;
import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.EnumSetConverter;
import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.NBTHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

import javax.annotation.Nullable;
import java.util.List;

import static mods.flammpfeil.slashblade.SlashBladeConfig.REFINE_DAMAGE_MULTIPLIER;
import static mods.flammpfeil.slashblade.SlashBladeConfig.SLASHBLADE_DAMAGE_MULTIPLIER;

public class EntityFDDriveEx extends EntityFDPhantomSword {
    private static final EntityDataAccessor<Integer> FLAGS = SynchedEntityData.<Integer>defineId(EntityFDDriveEx.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RANK = SynchedEntityData.<Float>defineId(EntityFDDriveEx.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BASESIZE = SynchedEntityData.<Float>defineId(EntityFDDriveEx.class,
            EntityDataSerializers.FLOAT);

    private KnockBacks action = KnockBacks.cancel;

    private double damage = 7.0D;

    public KnockBacks getKnockBack() {
        return action;
    }

    public void setKnockBack(KnockBacks action) {
        this.action = action;
    }

    public void setKnockBackOrdinal(int ordinal) {
        if (0 <= ordinal && ordinal < KnockBacks.values().length)
            this.action = KnockBacks.values()[ordinal];
        else
            this.action = KnockBacks.cancel;
    }

    public EntityFDDriveEx(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
        this.setNoGravity(true);
    }

    public static EntityFDDriveEx createInstance(PlayMessages.SpawnEntity packet, Level worldIn) {
        return new EntityFDDriveEx(SlashBlade.RegistryEvents.Drive, worldIn);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FLAGS, 0);
        this.entityData.define(RANK, 0.0f);
        this.entityData.define(BASESIZE, 1.0f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);

        NBTHelper.getNBTCoupler(compound).put("BaseSize", this.getBaseSize())
                .put("Speed", this.getSpeed()).put("Color", this.getColor()).put("Rank", this.getRank())
                .put("damage", this.damage).put("crit", this.getIsCritical()).put("clip", this.isNoClip())
               .put("Knockback", this.getKnockBack().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);

        NBTHelper.getNBTCoupler(compound).get("BaseSize", this::setBaseSize)
                .get("Speed", this::setSpeed).get("Color", this::setColor).get("Rank", this::setRank)
                .get("damage", ((Double v) -> this.damage = v), this.damage).get("crit", this::setIsCritical)
                .get("clip", this::setNoClip)
                .get("Knockback", this::setKnockBackOrdinal);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d0 = this.getBoundingBox().getSize() * 10.0D;
        if (Double.isNaN(d0)) {
            d0 = 1.0D;
        }

        d0 = d0 * 64.0D * getViewScale();
        return distance < d0 * d0;
    }

    @Override
    public void tick() {
        super.tick();
        tryDespawn();
    }

    protected void tryDespawn() {
        if (!this.level().isClientSide()) {
            if (getDelay() < this.tickCount)
                this.remove(RemovalReason.DISCARDED);
        }
    }
    private void refreshFlags() {
        int newValue;
        if (this.level().isClientSide()) {
            newValue = (Integer)this.entityData.get(FLAGS);
            if (this.intFlags != newValue) {
                this.intFlags = newValue;

            }
        } else {
            newValue = EnumSetConverter.convertToInt(this.flags);
            if (this.intFlags != newValue) {
                this.entityData.set(FLAGS, newValue);
                this.intFlags = newValue;
            }
        }

    }

    public float getRank() {
        return this.getEntityData().get(RANK);
    }

    public void setRank(float value) {
        this.getEntityData().set(RANK, value);
    }

    public IConcentrationRank.ConcentrationRanks getRankCode() {
        return IConcentrationRank.ConcentrationRanks.getRankFromLevel(getRank());
    }

    public float getBaseSize() {
        return this.getEntityData().get(BASESIZE);
    }

    public void setBaseSize(float value) {
        this.getEntityData().set(BASESIZE, value);
    }

    @Nullable
    @Override
    public Entity getShooter() {
        return this.getOwner();
    }

    @Override
    public void setShooter(Entity shooter) {
        setOwner(shooter);
    }

    public List<MobEffectInstance> getPotionEffects() {
        List<MobEffectInstance> effects = PotionUtils.getAllEffects(this.getPersistentData());

        if (effects.isEmpty())
            effects.add(new MobEffectInstance(MobEffects.POISON, 1, 1));

        return effects;
    }

    public void setDamage(double damageIn) {
        this.damage = damageIn;
    }

    @Override
    public double getDamage() {
        return this.damage;
    }

    protected void onHitEntity(EntityHitResult entityHitResult) {
        Entity targetEntity = entityHitResult.getEntity();
        float damageValue = (float) this.getDamage();

        Entity shooter = this.getShooter();
        DamageSource damagesource;
        if (shooter == null) {
            damagesource = this.damageSources().indirectMagic(this, this);
        } else {
            damagesource = this.damageSources().indirectMagic(this, shooter);
            if (shooter instanceof LivingEntity) {
                Entity hits = targetEntity;
                if (targetEntity instanceof PartEntity) {
                    hits = ((PartEntity<?>) targetEntity).getParent();
                }
                ((LivingEntity) shooter).setLastHurtMob(hits);
            }
        }

        int fireTime = targetEntity.getRemainingFireTicks();
        if (this.isOnFire() && !(targetEntity instanceof EnderMan)) {
            targetEntity.setSecondsOnFire(5);
        }

        targetEntity.invulnerableTime = 0;
        if(this.getOwner() instanceof LivingEntity living) {
            damageValue *= living.getAttributeValue(Attributes.ATTACK_DAMAGE);
            //评分等级加成
            if (living instanceof Player player){
                IConcentrationRank.ConcentrationRanks rankBonus = player
                        .getCapability(ConcentrationRankCapabilityProvider.RANK_POINT)
                        .map(rp -> rp.getRank(player.getCommandSenderWorld().getGameTime()))
                        .orElse(IConcentrationRank.ConcentrationRanks.NONE);
                float rankDamageBonus = rankBonus.level / 2.0f;
                if (IConcentrationRank.ConcentrationRanks.S.level <= rankBonus.level) {
                    int refine = player.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).map(rp -> rp.getRefine()).orElse(0);
                    int level = player.experienceLevel;
                    rankDamageBonus = (float) Math.max(rankDamageBonus, Math.min(level, refine) * REFINE_DAMAGE_MULTIPLIER.get());
                }
                damageValue += rankDamageBonus;
            }
            damageValue *= AttackManager.getSlashBladeDamageScale(living) * SLASHBLADE_DAMAGE_MULTIPLIER.get();
            if (this.getIsCritical()) {
                damageValue += this.random.nextInt((Mth.ceil(damageValue) / 2 + 2));
            }
        }

        if (targetEntity.hurt(damagesource, damageValue)) {
            Entity hits = targetEntity;
            if (targetEntity instanceof PartEntity) {
                hits = ((PartEntity<?>) targetEntity).getParent();
            }

            if (hits instanceof LivingEntity) {
                LivingEntity targetLivingEntity = (LivingEntity) hits;

                StunManager.setStun(targetLivingEntity);
                if (!this.level().isClientSide() && shooter instanceof LivingEntity) {
                    EnchantmentHelper.doPostHurtEffects(targetLivingEntity, shooter);
                    EnchantmentHelper.doPostDamageEffects((LivingEntity) shooter, targetLivingEntity);
                }

                affectEntity(targetLivingEntity, getPotionEffects(), 1.0f);

                if (shooter != null && targetLivingEntity != shooter && targetLivingEntity instanceof Player
                        && shooter instanceof ServerPlayer) {
                    ((ServerPlayer) shooter).playNotifySound(this.getHitEntityPlayerSound(), SoundSource.PLAYERS, 0.18F,
                            0.45F);
                }
            }

            this.playSound(this.getHitEntitySound(), 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
        } else {
            targetEntity.setRemainingFireTicks(fireTime);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult blockraytraceresult) {
        this.setRemoved(RemovalReason.DISCARDED);
    }

    @Nullable
    public EntityHitResult getRayTrace(Vec3 p_213866_1_, Vec3 p_213866_2_) {
        return ProjectileUtil.getEntityHitResult(this.level(), this, p_213866_1_, p_213866_2_,
                this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0D), (entity) -> {
                    return !entity.isSpectator() && entity.isAlive() && entity.isPickable()
                            && (entity != this.getShooter());
                });
    }
}