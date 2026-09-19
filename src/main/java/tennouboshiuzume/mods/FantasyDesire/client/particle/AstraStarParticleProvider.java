package tennouboshiuzume.mods.FantasyDesire.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import tennouboshiuzume.mods.FantasyDesire.particle.AstraStarParticleOptions;

public final class AstraStarParticleProvider implements ParticleProvider<AstraStarParticleOptions> {
    @Override
    public Particle createParticle(AstraStarParticleOptions options, ClientLevel level,
            double x, double y, double z, double vx, double vy, double vz) {
        if (!AstraStarParticle.validPosition(x, y, z)
                || !Double.isFinite(vx) || !Double.isFinite(vy) || !Double.isFinite(vz)) {
            return null;
        }
        return new AstraStarParticle(level, x, y, z, vx, vy, vz, options);
    }
}
