package tennouboshiuzume.mods.FantasyDesire.capability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EchoDamageCap implements IEchoDamageCap {
    private final Map<UUID, Float> damageMap = new HashMap<>();

    @Override
    public void addDamage(UUID attacker, float amount) {
        if (attacker != null && amount > 0) {
            damageMap.put(attacker, damageMap.getOrDefault(attacker, 0f) + amount);
        }
    }

    @Override
    public float getDamage(UUID attacker) {
        return attacker != null ? damageMap.getOrDefault(attacker, 0f) : 0f;
    }

    @Override
    public Map<UUID, Float> getAllDamage() {
        return damageMap;
    }

    @Override
    public void clearDamage() {
        damageMap.clear();
    }
}