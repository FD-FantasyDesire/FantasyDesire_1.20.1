package tennouboshiuzume.mods.FantasyDesire.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import tennouboshiuzume.mods.FantasyDesire.particle.BladeRiftParticleOptions;

public class BladeRiftParticleProvider implements ParticleProvider<BladeRiftParticleOptions> {
    @Override
    public Particle createParticle(BladeRiftParticleOptions data, ClientLevel level,
            double x, double y, double z, double vx, double vy, double vz) {
        return new BladeRiftParticle(level, data);
    }
}
