package tennouboshiuzume.mods.FantasyDesire.damagesource;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;

public class FDDamageTypes {
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


    private static ResourceKey<DamageType> register(String name)
    {
        return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(FantasyDesire.MODID, name));
    }

    public static void bootstrap(BootstapContext<DamageType> context) {
        context.register(DIMENSION, new DamageType(FantasyDesire.MODID + ":dimension", 0f));
        context.register(OMEGA, new DamageType(FantasyDesire.MODID + ":omega", 0f));
        context.register(RESOLUTION, new DamageType(FantasyDesire.MODID + ":resolution", 0f));
        context.register(ABSORB, new DamageType(FantasyDesire.MODID + ":absorb", 0f));
        context.register(ETERNITY, new DamageType(FantasyDesire.MODID + ":eternity", 0f));
        context.register(WRATH, new DamageType(FantasyDesire.MODID + ":wrath", 0f));
        context.register(LUST, new DamageType(FantasyDesire.MODID + ":lust", 0f));
        context.register(SLOTH, new DamageType(FantasyDesire.MODID + ":sloth", 0f));
        context.register(GLUTTONY, new DamageType(FantasyDesire.MODID + ":gluttony", 0f));
        context.register(GLOOM, new DamageType(FantasyDesire.MODID + ":gloom", 0f));
        context.register(PRIDE, new DamageType(FantasyDesire.MODID + ":pride", 0f));
        context.register(ENVY, new DamageType(FantasyDesire.MODID + ":envy", 0f));
        context.register(ECHO, new DamageType(FantasyDesire.MODID + ":echo", 0f));

    }
}
