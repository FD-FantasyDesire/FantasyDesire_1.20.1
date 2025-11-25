package tennouboshiuzume.mods.FantasyDesire.damagesource;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;

import java.util.Arrays;
import java.util.List;

public class FDDamageSource extends DamageSource {
    public static final ResourceKey<DamageType> DIMENSION = register("dimension");
    public static final ResourceKey<DamageType> OMEGA = register("omega");
    public static final ResourceKey<DamageType> RESOLUTION = register("resolution");
    public static final ResourceKey<DamageType> ABSORB = register("absorb");
    public static final ResourceKey<DamageType> ETERNITY = register("eternity");
    public static final ResourceKey<DamageType> WRATH = register("wrath");
    public static final ResourceKey<DamageType> LUST = register("lust");
    public static final ResourceKey<DamageType> SLOTH = register("sloth");
    public static final ResourceKey<DamageType> GLUTTONY = register("gluttony");
    public static final ResourceKey<DamageType> GLOOM = register("gloom");
    public static final ResourceKey<DamageType> PRIDE = register("pride");
    public static final ResourceKey<DamageType> ENVY = register("envy");
    public static final ResourceKey<DamageType> ECHO = register("echo");

    public static void bootstrap(BootstapContext<DamageType> context) {
        context.register(DIMENSION, new DamageType(FantasyDesire.MODID + ".dimension", 0f));
        context.register(OMEGA, new DamageType(FantasyDesire.MODID + ".omega", 0f));
        context.register(RESOLUTION, new DamageType(FantasyDesire.MODID + ".resolution", 0f));
        context.register(ABSORB, new DamageType(FantasyDesire.MODID + ".absorb", 0f));
        context.register(ETERNITY, new DamageType(FantasyDesire.MODID + ".eternity", 0f));
        context.register(WRATH, new DamageType(FantasyDesire.MODID + ".wrath", 0f));
        context.register(LUST, new DamageType(FantasyDesire.MODID + ".lust", 0f));
        context.register(SLOTH, new DamageType(FantasyDesire.MODID + ".sloth", 0f));
        context.register(GLUTTONY, new DamageType(FantasyDesire.MODID + ".gluttony", 0f));
        context.register(GLOOM, new DamageType(FantasyDesire.MODID + ".gloom", 0f));
        context.register(PRIDE, new DamageType(FantasyDesire.MODID + ".pride", 0f));
        context.register(ENVY, new DamageType(FantasyDesire.MODID + ".envy", 0f));
        context.register(ECHO, new DamageType(FantasyDesire.MODID + ".echo", 0f));
    }


    public FDDamageSource(Holder<DamageType> p_270906_, @Nullable Entity p_270796_, @Nullable Entity p_270459_, @Nullable Vec3 p_270623_) {
        super(p_270906_, p_270796_, p_270459_, p_270623_);
    }

    private static ResourceKey<DamageType> register(String name)
    {
        return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(FantasyDesire.MODID, name));
    }

    public static ResourceKey<DamageType> fromString(String id) {
        return register(id);
    }

    public static DamageSource getDamageSource(Level level, ResourceKey<DamageType> type, EntityType<?>... toIgnore) {
        return getEntityDamageSource(level, type, null, toIgnore);
    }

    public static DamageSource entityDamageSource(Level level, ResourceKey<DamageType> type, @Nullable Entity attacker) {
        return getEntityDamageSource(level, type, attacker);
    }

    public static DamageSource getEntityDamageSource(Level level, ResourceKey<DamageType> type, @Nullable Entity attacker, EntityType<?>... toIgnore) {
        return getIndirectEntityDamageSource(level, type, attacker, attacker, toIgnore);
    }

    public static DamageSource indirectEntityDamageSource(Level level, ResourceKey<DamageType> type, @Nullable Entity attacker, @Nullable Entity indirectAttacker){
        return getIndirectEntityDamageSource(level, type, attacker, indirectAttacker);
    }

    public static DamageSource getIndirectEntityDamageSource(Level level, ResourceKey<DamageType> type, @Nullable Entity attacker, @Nullable Entity indirectAttacker, EntityType<?>... toIgnore) {
        return toIgnore.length > 0 ? new EntityExcludedDamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), toIgnore) : source(level, type, attacker, indirectAttacker);
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> type, @Nullable Entity attacker, @Nullable Entity indirectAttacker){
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), attacker, indirectAttacker);
    }

    public static class EntityExcludedDamageSource extends DamageSource {

        protected final List<EntityType<?>> entities;

        public EntityExcludedDamageSource(Holder<DamageType> type, EntityType<?>... entities) {
            super(type);
            this.entities = Arrays.stream(entities).toList();
        }

        @Override
        public Component getLocalizedDeathMessage(LivingEntity living) {
            LivingEntity livingentity = living.getKillCredit();
            String s = "death.attack." + this.type().msgId();
            String s1 = s + ".player";
            if (livingentity != null) {
                for (EntityType<?> entity : entities) {
                    if (livingentity.getType() == entity) {
                        return Component.translatable(s, living.getDisplayName());
                    }
                }
            }
            return livingentity != null ? Component.translatable(s1, living.getDisplayName(), livingentity.getDisplayName()) : Component.translatable(s, living.getDisplayName());
        }
    }
}
