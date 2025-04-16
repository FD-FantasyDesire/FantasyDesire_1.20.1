package tennouboshiuzume.mods.FantasyDesire.utils;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

public class DamageUtils {
    // 从 registryAccess 获取 Holder<DamageType>
    public static Holder<DamageType> getDamageTypeHolder(ResourceKey<DamageType> typeKey, RegistryAccess registryAccess) {
        return registryAccess
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolder(typeKey)
                .orElseThrow(() -> new IllegalArgumentException("找不到伤害类型: " + typeKey));
    }

    // 有源伤害（来自实体）
    public static DamageSource createWithEntity(ResourceKey<DamageType> typeKey, LivingEntity source, RegistryAccess registryAccess) {
        DamageSources sources = new DamageSources(registryAccess);
        Holder<DamageType> holder = getDamageTypeHolder(typeKey, registryAccess);
        return new DamageSource(holder, source);
    }

    // 无源伤害（比如场景伤害）
    public static DamageSource createWithoutEntity(ResourceKey<DamageType> typeKey, RegistryAccess registryAccess) {
        DamageSources sources = new DamageSources(registryAccess);
        Holder<DamageType> holder = getDamageTypeHolder(typeKey, registryAccess);
        return new DamageSource(holder);
    }

}
