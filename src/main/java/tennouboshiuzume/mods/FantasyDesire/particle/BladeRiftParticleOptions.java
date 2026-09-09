package tennouboshiuzume.mods.FantasyDesire.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import tennouboshiuzume.mods.FantasyDesire.init.FDParticles;
import tennouboshiuzume.mods.FantasyDesire.utils.ColorUtils;
import java.util.Locale;

public class BladeRiftParticleOptions implements ParticleOptions {
    public static final Codec<BladeRiftParticleOptions> CODEC = RecordCodecBuilder.create(i -> i.group(
            Vec3.CODEC.fieldOf("start").forGetter(o -> o.start), Vec3.CODEC.fieldOf("end").forGetter(o -> o.end),
            Codec.INT.fieldOf("lifetime").forGetter(o -> o.lifetime),
            Codec.FLOAT.fieldOf("riftLength").forGetter(o -> o.riftLength),
            Codec.FLOAT.fieldOf("riftWidth").forGetter(o -> o.riftWidth),
            Codec.INT.fieldOf("coreColor").forGetter(o -> o.coreColor),
            Codec.INT.fieldOf("energyColor").forGetter(o -> o.energyColor)).apply(i, BladeRiftParticleOptions::new));

    public static final Deserializer<BladeRiftParticleOptions> DESERIALIZER = new Deserializer<>() {
        private void space(StringReader r) throws CommandSyntaxException {
            r.expect(' ');
        }

        @Override
        public BladeRiftParticleOptions fromCommand(ParticleType<BladeRiftParticleOptions> type, StringReader r)
                throws CommandSyntaxException {
            space(r);
            double sx = r.readDouble();
            space(r);
            double sy = r.readDouble();
            space(r);
            double sz = r.readDouble();
            space(r);
            double ex = r.readDouble();
            space(r);
            double ey = r.readDouble();
            space(r);
            double ez = r.readDouble();
            space(r);
            int lifetime = r.readInt();
            space(r);
            float length = (float) r.readDouble();
            space(r);
            float width = (float) r.readDouble();
            space(r);
            int core = ColorUtils.readRgb(r);
            space(r);
            int energy = ColorUtils.readRgb(r);
            return new BladeRiftParticleOptions(new Vec3(sx, sy, sz), new Vec3(ex, ey, ez), lifetime, length, width,
                    core, energy);
        }

        @Override
        public BladeRiftParticleOptions fromNetwork(ParticleType<BladeRiftParticleOptions> type, FriendlyByteBuf b) {
            Vec3 s = new Vec3(b.readDouble(), b.readDouble(), b.readDouble()),
                    e = new Vec3(b.readDouble(), b.readDouble(), b.readDouble());
            return new BladeRiftParticleOptions(s, e, b.readInt(), b.readFloat(), b.readFloat(), b.readInt(),
                    b.readInt());
        }
    };
    public final Vec3 start, end;
    public final int lifetime, coreColor, energyColor;
    public final float riftLength, riftWidth;

    public BladeRiftParticleOptions(Vec3 start, Vec3 end, int lifetime, float riftLength, float riftWidth,
            int coreColor, int energyColor) {
        this.start = start;
        this.end = end;
        this.lifetime = Math.max(1, lifetime);
        this.riftLength = Math.max(.001f, riftLength);
        this.riftWidth = Math.max(.001f, riftWidth);
        this.coreColor = coreColor & 0xFFFFFF;
        this.energyColor = energyColor & 0xFFFFFF;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf b) {
        b.writeDouble(start.x);
        b.writeDouble(start.y);
        b.writeDouble(start.z);
        b.writeDouble(end.x);
        b.writeDouble(end.y);
        b.writeDouble(end.z);
        b.writeInt(lifetime);
        b.writeFloat(riftLength);
        b.writeFloat(riftWidth);
        b.writeInt(coreColor);
        b.writeInt(energyColor);
    }

    @Override
    public @NotNull String writeToString() {
        return String.format(Locale.ROOT, "%.6f %.6f %.6f %.6f %.6f %.6f %d %.6f %.6f %s %s", start.x, start.y, start.z,
                end.x, end.y, end.z, lifetime, riftLength, riftWidth, ColorUtils.formatRgb(coreColor),
                ColorUtils.formatRgb(energyColor));
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return FDParticles.BLADE_RIFT.get();
    }
}
