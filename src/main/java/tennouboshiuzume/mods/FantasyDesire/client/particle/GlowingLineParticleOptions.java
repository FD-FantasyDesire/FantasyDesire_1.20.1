package tennouboshiuzume.mods.FantasyDesire.client.particle;

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

import java.util.Locale;

public class GlowingLineParticleOptions implements ParticleOptions {
    public static final Codec<GlowingLineParticleOptions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Vec3.CODEC.fieldOf("start").forGetter(o -> o.start),
                    Vec3.CODEC.fieldOf("end").forGetter(o -> o.end),
                    Codec.INT.fieldOf("color").forGetter(o -> o.color),
                    Codec.FLOAT.fieldOf("thickness").forGetter(o -> o.thickness),
                    Codec.FLOAT.fieldOf("alpha").forGetter(o -> o.alpha),
                    Codec.BOOL.fieldOf("fade").forGetter(o -> o.fade),
                    Codec.INT.fieldOf("lifetime").forGetter(o -> o.lifetime)
            ).apply(instance, GlowingLineParticleOptions::new)
    );

    public static final Deserializer<GlowingLineParticleOptions> DESERIALIZER =
            new Deserializer<>() {
                @Override
                public GlowingLineParticleOptions fromCommand(ParticleType<GlowingLineParticleOptions> type, StringReader reader) throws CommandSyntaxException {
                    reader.expect(' ');
                    double sx = reader.readDouble(); reader.expect(' ');
                    double sy = reader.readDouble(); reader.expect(' ');
                    double sz = reader.readDouble(); reader.expect(' ');
                    double ex = reader.readDouble(); reader.expect(' ');
                    double ey = reader.readDouble(); reader.expect(' ');
                    double ez = reader.readDouble(); reader.expect(' ');
                    int color = reader.readInt(); reader.expect(' ');
                    float thickness = (float) reader.readDouble(); reader.expect(' ');
                    float alpha = (float) reader.readDouble(); reader.expect(' ');
                    boolean fade = reader.readBoolean(); reader.expect(' ');
                    int lifetime = reader.readInt();

                    return new GlowingLineParticleOptions(
                            new Vec3(sx, sy, sz),
                            new Vec3(ex, ey, ez),
                            color, thickness, alpha, fade, lifetime
                    );
                }

                @Override
                public GlowingLineParticleOptions fromNetwork(ParticleType<GlowingLineParticleOptions> type, FriendlyByteBuf buf) {
                    Vec3 start = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                    Vec3 end = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                    int color = buf.readInt();
                    float thickness = buf.readFloat();
                    float alpha = buf.readFloat();
                    boolean fade = buf.readBoolean();
                    int lifetime = buf.readInt();
                    return new GlowingLineParticleOptions(start, end, color, thickness, alpha, fade, lifetime);
                }
            };

    public final Vec3 start;
    public final Vec3 end;
    public final int color;
    public final float thickness;
    public final float alpha;
    public final boolean fade;
    public final int lifetime;

    public GlowingLineParticleOptions(Vec3 start, Vec3 end, int color, float thickness, float alpha, boolean fade, int lifetime) {
        this.start = start;
        this.end = end;
        this.color = color;
        this.thickness = thickness;
        this.alpha = alpha;
        this.fade = fade;
        this.lifetime = lifetime;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeDouble(start.x);
        buf.writeDouble(start.y);
        buf.writeDouble(start.z);
        buf.writeDouble(end.x);
        buf.writeDouble(end.y);
        buf.writeDouble(end.z);
        buf.writeInt(color);
        buf.writeFloat(thickness);
        buf.writeFloat(alpha);
        buf.writeBoolean(fade);
        buf.writeInt(lifetime);
    }

    @Override
    public @NotNull String writeToString() {
        return String.format(Locale.ROOT,
                "%f %f %f %f %f %f %d %f %f %b %d",
                start.x, start.y, start.z,
                end.x, end.y, end.z,
                color, thickness, alpha, fade, lifetime);
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return FDParticles.GLOWING_LINE.get();
    }
}
