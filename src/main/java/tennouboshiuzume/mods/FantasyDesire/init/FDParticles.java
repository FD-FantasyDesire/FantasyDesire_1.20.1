package tennouboshiuzume.mods.FantasyDesire.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.client.particle.GlowingLineParticleOptions;

public class FDParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, FantasyDesire.MODID);

    public static final RegistryObject<ParticleType<GlowingLineParticleOptions>> GLOWING_LINE =
            PARTICLES.register("glowing_line", () ->
                    new ParticleType<>(false, GlowingLineParticleOptions.DESERIALIZER) {
                        @Override
                        public Codec<GlowingLineParticleOptions> codec() {
                            return GlowingLineParticleOptions.CODEC;
                        }
                    });
}
