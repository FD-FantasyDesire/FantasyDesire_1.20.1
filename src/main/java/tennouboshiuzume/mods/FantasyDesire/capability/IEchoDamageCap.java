package tennouboshiuzume.mods.FantasyDesire.capability;

import java.util.Map;
import java.util.UUID;

public interface IEchoDamageCap {
    void addDamage(UUID attacker, float amount);

    float getDamage(UUID attacker);

    Map<UUID, Float> getAllDamage();

    void clearDamage();
}