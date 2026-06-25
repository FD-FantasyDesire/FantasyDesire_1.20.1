package tennouboshiuzume.mods.FantasyDesire.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import org.jetbrains.annotations.Nullable;

public class SpreadingRingParticleProvider implements ParticleProvider<SpreadingRingParticleOptions> {

    public SpreadingRingParticleProvider() {
    }

    @Nullable
    @Override
    public Particle createParticle(SpreadingRingParticleOptions type, ClientLevel level, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed) {
        return new SpreadingRingParticle(level, x, y, z, type.color, type.maxRadius, type.thickness, type.lifetime);
    }
}
