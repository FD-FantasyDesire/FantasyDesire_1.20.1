package tennouboshiuzume.mods.FantasyDesire.entity.phantomsword;

import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import tennouboshiuzume.mods.FantasyDesire.entity.EntityFDPhantomSword;

import java.util.Comparator;

public final class PiercingHomingFlight extends HomingFlight {
    private static final double CHAIN_SPEED_MULTIPLIER = 1.5;
    private static final double CHAIN_TURN_MULTIPLIER = 3;
    private static final double CHAIN_MAX_TURN = Math.toRadians(90);
    private final boolean chaining;

    private static final class TargetRules {
        // 世界查询时才加载依赖 Forge 访问转换的类型，行为工厂与存档本身不依赖世界启动。
        // 敌我判断使用射手，但视线另从剑弹出发，避免射手身后的墙影响转火。
        private static final TargetingConditions ATTACKABLE = new TargetSelector.SlashBladeTargetingConditions()
                .ignoreLineOfSight().ignoreInvisibilityTesting().selector(new TargetSelector.AttackablePredicate());
    }

    private PiercingHitBudget budget = new PiercingHitBudget(3);
    private float searchRadius = 16f;
    private int pendingTargetTicks;

    private final SwordImpactPolicy impacts = new SwordImpactPolicy() {
        @Override
        public boolean canHit(EntityFDPhantomSword sword, Entity target) {
            Entity root = rootTarget(target);
            // 此模式只碰撞当前锁定目标，旁路目标不会偷走次数或改变连锁顺序。
            return root.getId() == sword.getTargetId() && !budget.hasHit(root.getUUID())
                    && isValidTarget(sword, root);
        }

        @Override
        public boolean beginHit(EntityFDPhantomSword sword, Entity target) {
            // Forge 取消的碰撞不会进入此处；已接受的命中即消耗一次，包括 hurt() 被拒绝的情况。
            return canHit(sword, target) && budget.accept(rootTarget(target).getUUID());
        }

        @Override
        public void afterHit(EntityFDPhantomSword sword, Entity target) {
            LivingEntity next = budget.remaining() > 0 ? findNextTarget(sword) : null;
            if (next != null) {
                sword.setTargetId(next.getId());
                // 首段发射等待只执行一次，命中后下一 tick 即可制导。
                sword.setSeekDelay(0);
                if (!chaining)
                    sword.setFlightBehavior(PhantomSwordBehaviors.PIERCING_CHAIN, save());
            } else {
                // 无候选或次数耗尽都转入普通插入；死亡目标无法承载幻影剑。
                Entity hit = rootTarget(target);
                if (hit.isAlive())
                    sword.setHitEntity(hit);
                else
                    sword.burst();
            }
        }

        @Override
        public boolean canEmbed(EntityFDPhantomSword sword) {
            return false;
        }

        @Override
        public boolean stopAtContact() {
            return true;
        }

        @Override
        public void damageRejected(EntityFDPhantomSword sword) {
            // 命中后的统一结算负责推进次数和换目标，不再修改父类 Pierce。
        }
    };

    public PiercingHomingFlight() {
        this(false);
    }

    public PiercingHomingFlight(boolean chaining) {
        super(false);
        this.chaining = chaining;
    }

    @Override
    public SwordImpactPolicy impacts() {
        return impacts;
    }

    @Override
    public Vec3 steer(EntityFDPhantomSword sword) {
        LivingEntity target = sword.getTargetEntity();
        if (!sword.level().isClientSide()) {
            if (sword.hasPendingTarget() && ++pendingTargetTicks <= 20)
                return null;
            if (budget.remaining() == 0 || target == null || !isValidTarget(sword, target)
                    || budget.hasHit(target.getUUID())) {
                sword.burst();
                return null;
            }
        }
        if (chaining && target != null && target.isAlive()) {
            Vec3 displacement = target.position().add(0, target.getEyeHeight() * 0.5, 0).subtract(sword.position());
            return chainVelocity(sword.getDeltaMovement(), displacement, target.getDeltaMovement().length(),
                    sword.getSpeed(), sword.getSeekAngle());
        }
        return super.steer(sword);
    }

    /** 连锁段提高转向与追赶速度，大角度转弯时减速，接近目标时限制步长，避免绕圈。 */
    public static Vec3 chainVelocity(Vec3 currentMotion, Vec3 displacement, double targetSpeed,
            double baseSpeed, float seekAngle) {
        double distance = displacement.length();
        if (distance < 1.0E-6 || baseSpeed <= 0)
            return Vec3.ZERO;
        double turn = Math.min(CHAIN_MAX_TURN,
                Math.toRadians(seekAngle * CHAIN_TURN_MULTIPLIER * Math.max(1, currentMotion.length())));
        Vec3 direction = rotateTowards(currentMotion, displacement, turn);
        double speed = Math.max(baseSpeed * CHAIN_SPEED_MULTIPLIER, targetSpeed + baseSpeed * 0.5);
        double alignment = Mth.clamp(direction.dot(displacement.normalize()), 0.2, 1);
        return direction.scale(Math.min(distance, speed) * alignment);
    }

    private static Entity rootTarget(Entity target) {
        return target instanceof PartEntity<?> part ? part.getParent() : target;
    }

    public static boolean isValidTarget(EntityFDPhantomSword sword, Entity target) {
        return target instanceof LivingEntity living && living.isAlive() && living.isAttackable()
                && !living.isSpectator() && sword.getShooter() instanceof LivingEntity shooter
                && living != shooter && TargetRules.ATTACKABLE.test(shooter, living);
    }

    private LivingEntity findNextTarget(EntityFDPhantomSword sword) {
        Vec3 origin = sword.position();
        double radiusSquared = (double) searchRadius * searchRadius;
        return sword.level().getEntitiesOfClass(LivingEntity.class, sword.getBoundingBox().inflate(searchRadius),
                target -> !budget.hasHit(target.getUUID()) && isValidTarget(sword, target)
                        && target.getBoundingBox().getCenter().distanceToSqr(origin) <= radiusSquared)
                .stream()
                .sorted(Comparator.<LivingEntity>comparingDouble(target -> target.distanceToSqr(origin))
                        .thenComparingInt(Entity::getId))
                .filter(target -> canSeeFromImpact(sword, target))
                .findFirst().orElse(null);
    }

    private boolean canSeeFromImpact(EntityFDPhantomSword sword, LivingEntity target) {
        Vec3 origin = sword.position();
        Vec3[] points = { target.getBoundingBox().getCenter(), target.getEyePosition(), target.position().add(0, 0.2, 0) };
        for (Vec3 point : points) {
            if (sword.level().clip(new ClipContext(origin, point, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, sword)).getType() == HitResult.Type.MISS)
                return true;
        }
        return false;
    }

    public int remainingHits() {
        return budget.remaining();
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("RemainingHits", budget.remaining());
        tag.putFloat("SearchRadius", searchRadius);
        ListTag hits = new ListTag();
        for (var id : budget.targets()) {
            CompoundTag hit = new CompoundTag();
            hit.putUUID("Target", id);
            hits.add(hit);
        }
        tag.put("HitTargets", hits);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        budget = new PiercingHitBudget(tag.contains("RemainingHits", Tag.TAG_ANY_NUMERIC)
                ? tag.getInt("RemainingHits") : 3);
        float radius = tag.contains("SearchRadius", Tag.TAG_ANY_NUMERIC) ? tag.getFloat("SearchRadius") : 16f;
        searchRadius = Float.isFinite(radius) ? Mth.clamp(radius, 0, 64) : 16f;
        ListTag hits = tag.getList("HitTargets", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(hits.size(), PiercingHitBudget.MAX_HITS); i++) {
            CompoundTag hit = hits.getCompound(i);
            if (hit.hasUUID("Target"))
                budget.restoreTarget(hit.getUUID("Target"));
        }
        pendingTargetTicks = 0;
    }
}
