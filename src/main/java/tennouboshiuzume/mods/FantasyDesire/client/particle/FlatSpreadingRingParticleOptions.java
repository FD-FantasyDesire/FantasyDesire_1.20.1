package tennouboshiuzume.mods.FantasyDesire.client.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import tennouboshiuzume.mods.FantasyDesire.init.FDParticles;

import java.util.Locale;

public class FlatSpreadingRingParticleOptions implements ParticleOptions {
    public static final Codec<FlatSpreadingRingParticleOptions> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    Codec.INT.fieldOf("color").forGetter(o -> o.color),
                    Codec.FLOAT.fieldOf("max_radius").forGetter(o -> o.maxRadius),
                    Codec.FLOAT.fieldOf("thickness").forGetter(o -> o.thickness),
                    Codec.INT.fieldOf("lifetime").forGetter(o -> o.lifetime))
            .apply(instance, FlatSpreadingRingParticleOptions::new));

    public static final Deserializer<FlatSpreadingRingParticleOptions> DESERIALIZER = new Deserializer<>() {
        @Override
        public FlatSpreadingRingParticleOptions fromCommand(ParticleType<FlatSpreadingRingParticleOptions> type,
                StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int color = reader.readInt();
            reader.expect(' ');
            float maxRadius = (float) reader.readDouble();
            reader.expect(' ');
            float thickness = (float) reader.readDouble();
            reader.expect(' ');
            int lifetime = reader.readInt();

            return new FlatSpreadingRingParticleOptions(color, maxRadius, thickness, lifetime);
        }

        @Override
        public FlatSpreadingRingParticleOptions fromNetwork(ParticleType<FlatSpreadingRingParticleOptions> type,
                FriendlyByteBuf buf) {
            int color = buf.readInt();
            float maxRadius = buf.readFloat();
            float thickness = buf.readFloat();
            int lifetime = buf.readInt();
            return new FlatSpreadingRingParticleOptions(color, maxRadius, thickness, lifetime);
        }
    };

    public final int color;
    public final float maxRadius;
    public final float thickness;
    public final int lifetime;

    public FlatSpreadingRingParticleOptions(int color, float maxRadius, float thickness, int lifetime) {
        this.color = color;
        this.maxRadius = maxRadius;
        this.thickness = thickness;
        this.lifetime = lifetime;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeInt(color);
        buf.writeFloat(maxRadius);
        buf.writeFloat(thickness);
        buf.writeInt(lifetime);
    }

    @Override
    public @NotNull String writeToString() {
        return String.format(Locale.ROOT,
                "%d %f %f %d",
                color, maxRadius, thickness, lifetime);
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return FDParticles.FLAT_SPREADING_RING.get();
    }
}
