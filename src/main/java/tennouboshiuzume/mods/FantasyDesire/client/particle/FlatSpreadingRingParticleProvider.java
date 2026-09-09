package tennouboshiuzume.mods.FantasyDesire.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import org.jetbrains.annotations.Nullable;
import tennouboshiuzume.mods.FantasyDesire.particle.FlatSpreadingRingParticleOptions;

public class FlatSpreadingRingParticleProvider implements ParticleProvider<FlatSpreadingRingParticleOptions> {

    public FlatSpreadingRingParticleProvider() {
    }

    @Nullable
    @Override
    public Particle createParticle(FlatSpreadingRingParticleOptions type, ClientLevel level, double x, double y,
            double z,
            double xSpeed, double ySpeed, double zSpeed) {
        return new FlatSpreadingRingParticle(level, x, y, z, type.color, type.maxRadius, type.thickness, type.lifetime);
    }
}
