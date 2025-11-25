package tennouboshiuzume.mods.FantasyDesire.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;

public class GlowingLineParticleProvider implements ParticleProvider<GlowingLineParticleOptions> {

    public GlowingLineParticleProvider() {
    }

    @Override
    public Particle createParticle(GlowingLineParticleOptions data, ClientLevel level,
                                   double x, double y, double z,
                                   double vx, double vy, double vz) {
        GlowingLineParticle particle = new GlowingLineParticle(
                level,
                data.start,
                data.end,
                data.color,
                data.thickness,
                data.alpha,
                data.fade,
                data.lifetime
        );

        return particle;
    }
}