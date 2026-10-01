package tennouboshiuzume.mods.FantasyDesire.entity.phantomsword;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** 穿刺模式采用总命中次数，包含首个目标；与父类 Pierce 的 N+1 语义分离。 */
public final class PiercingHitBudget {
    public static final int MAX_HITS = 128;
    private int remaining;
    private final Set<UUID> hitTargets = new HashSet<>();

    public PiercingHitBudget(int remaining) {
        this.remaining = Math.max(0, Math.min(MAX_HITS, remaining));
    }

    public boolean accept(UUID target) {
        if (target == null || remaining == 0 || hitTargets.size() >= MAX_HITS || !hitTargets.add(target))
            return false;
        remaining--;
        return true;
    }

    public boolean hasHit(UUID target) {
        return hitTargets.contains(target);
    }

    public int remaining() {
        return remaining;
    }

    public Set<UUID> targets() {
        return Set.copyOf(hitTargets);
    }

    public void restoreTarget(UUID target) {
        if (target != null && hitTargets.size() < MAX_HITS)
            hitTargets.add(target);
        remaining = Math.min(remaining, MAX_HITS - hitTargets.size());
    }
}
