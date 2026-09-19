package tennouboshiuzume.mods.FantasyDesire.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import tennouboshiuzume.mods.FantasyDesire.particle.AstraLightningParticleOptions;

public final class AstraLightningParticleProvider implements ParticleProvider<AstraLightningParticleOptions> {
    @Override
    public Particle createParticle(AstraLightningParticleOptions options, ClientLevel level,
            double x, double y, double z, double vx, double vy, double vz) {
        // 参数中的 start 是唯一锚点，命令外层的随机偏移与速度不会移动整条闪电。
        return new AstraLightningParticle(level, options);
    }
}
