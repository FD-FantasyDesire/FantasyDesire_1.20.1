package tennouboshiuzume.mods.FantasyDesire.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.client.particle.ColorShardParticleOptions;
import tennouboshiuzume.mods.FantasyDesire.client.particle.BladeRiftParticleOptions;
import tennouboshiuzume.mods.FantasyDesire.client.particle.FlatSpreadingRingParticleOptions;
import tennouboshiuzume.mods.FantasyDesire.client.particle.GlowingLineParticleOptions;
import tennouboshiuzume.mods.FantasyDesire.client.particle.SpreadingRingParticleOptions;

public class FDParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister
            .create(ForgeRegistries.PARTICLE_TYPES, FantasyDesire.MODID);

    public static final RegistryObject<ParticleType<SpreadingRingParticleOptions>> SPREADING_RING = PARTICLES
            .register("spreading_ring", () -> new ParticleType<>(false, SpreadingRingParticleOptions.DESERIALIZER) {
                @Override
                public Codec<SpreadingRingParticleOptions> codec() {
                    return SpreadingRingParticleOptions.CODEC;
                }
            });

    public static final RegistryObject<ParticleType<FlatSpreadingRingParticleOptions>> FLAT_SPREADING_RING = PARTICLES
            .register("flat_spreading_ring",
                    () -> new ParticleType<>(false, FlatSpreadingRingParticleOptions.DESERIALIZER) {
                        @Override
                        public Codec<FlatSpreadingRingParticleOptions> codec() {
                            return FlatSpreadingRingParticleOptions.CODEC;
                        }
                    });

    public static final RegistryObject<ParticleType<GlowingLineParticleOptions>> GLOWING_LINE = PARTICLES
            .register("glowing_line", () -> new ParticleType<>(false, GlowingLineParticleOptions.DESERIALIZER) {
                @Override
                public Codec<GlowingLineParticleOptions> codec() {
                    return GlowingLineParticleOptions.CODEC;
                }
            });

    public static final RegistryObject<ParticleType<ColorShardParticleOptions>> COLOR_SHARD = PARTICLES
            .register("color_shard", () -> new ParticleType<>(false, ColorShardParticleOptions.DESERIALIZER) {
                @Override
                public Codec<ColorShardParticleOptions> codec() {
                    return ColorShardParticleOptions.CODEC;
                }
            });

    public static final RegistryObject<SimpleParticleType> ENDER_SHARD = PARTICLES.register("ender_shard",
            () -> new SimpleParticleType(false));

    public static final RegistryObject<ParticleType<BladeRiftParticleOptions>> BLADE_RIFT = PARTICLES.register(
            "blade_rift",
            () -> new ParticleType<>(false, BladeRiftParticleOptions.DESERIALIZER) {
                @Override
                public Codec<BladeRiftParticleOptions> codec() {
                    return BladeRiftParticleOptions.CODEC;
                }
            });
}
