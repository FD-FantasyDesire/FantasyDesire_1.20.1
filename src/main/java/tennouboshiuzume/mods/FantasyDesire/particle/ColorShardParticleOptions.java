package tennouboshiuzume.mods.FantasyDesire.particle;

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

public class ColorShardParticleOptions implements ParticleOptions {
    public static final Codec<ColorShardParticleOptions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("color").forGetter(o -> o.color),
            Codec.FLOAT.fieldOf("scale").forGetter(o -> o.scale)).apply(instance, ColorShardParticleOptions::new));

    public static final Deserializer<ColorShardParticleOptions> DESERIALIZER = new Deserializer<>() {
        @Override
        public ColorShardParticleOptions fromCommand(ParticleType<ColorShardParticleOptions> type, StringReader reader)
                throws CommandSyntaxException {
            reader.expect(' ');
            int color = reader.readInt();
            reader.expect(' ');
            float scale = (float) reader.readDouble();
            return new ColorShardParticleOptions(color, scale);
        }

        @Override
        public ColorShardParticleOptions fromNetwork(ParticleType<ColorShardParticleOptions> type,
                FriendlyByteBuf buf) {
            return new ColorShardParticleOptions(buf.readInt(), buf.readFloat());
        }
    };

    public final int color;
    public final float scale;

    public ColorShardParticleOptions(int color, float scale) {
        this.color = color;
        this.scale = scale;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeInt(color);
        buf.writeFloat(scale);
    }

    @Override
    public @NotNull String writeToString() {
        return String.format(Locale.ROOT, "%d %f", color, scale);
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return FDParticles.COLOR_SHARD.get();
    }
}
