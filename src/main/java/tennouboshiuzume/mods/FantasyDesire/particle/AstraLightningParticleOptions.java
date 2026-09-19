package tennouboshiuzume.mods.FantasyDesire.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import tennouboshiuzume.mods.FantasyDesire.init.FDParticles;
import tennouboshiuzume.mods.FantasyDesire.utils.ColorUtils;

import java.util.Locale;

/** 一份参数描述整条星座闪电；坐标为世界坐标，长度单位为格，寿命单位为 tick。 */
public final class AstraLightningParticleOptions implements ParticleOptions {
    public static final int MAX_ENDPOINTS = 33;
    private static final Codec<Integer> RGB_CODEC = Codec.STRING.comapFlatMap(value -> {
        try {
            return DataResult.success(ColorUtils.parseRgb(value));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }, ColorUtils::formatRgb);
    private static final Codec<Vec3> POSITION_CODEC = Vec3.CODEC.comapFlatMap(value -> validPosition(value)
            ? DataResult.success(value) : DataResult.error(() -> "星座闪电坐标必须是世界范围内的有限值"), value -> value);

    public static final Codec<AstraLightningParticleOptions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            POSITION_CODEC.fieldOf("start").forGetter(o -> o.start),
            POSITION_CODEC.fieldOf("end").forGetter(o -> o.end),
            RGB_CODEC.fieldOf("color").forGetter(o -> o.color),
            Codec.floatRange(0.001f, 4f).optionalFieldOf("thickness", 0.05f).forGetter(o -> o.thickness),
            Codec.intRange(1, 1200).optionalFieldOf("lifetime", 20).forGetter(o -> o.lifetime),
            Codec.floatRange(0f, 1f).optionalFieldOf("alpha", 1f).forGetter(o -> o.alpha),
            Codec.BOOL.optionalFieldOf("fade", true).forGetter(o -> o.fade),
            Codec.doubleRange(0, 64).optionalFieldOf("randomness", 0.7).forGetter(o -> o.randomness),
            Codec.intRange(2, MAX_ENDPOINTS).fieldOf("maxEndpoints").forGetter(o -> o.maxEndpoints),
            Codec.floatRange(0.01f, 8f).optionalFieldOf("starRadius", 0.22f).forGetter(o -> o.starRadius),
            Codec.LONG.fieldOf("seed").forGetter(o -> o.seed)
    ).apply(instance, AstraLightningParticleOptions::new));

    public static final Deserializer<AstraLightningParticleOptions> DESERIALIZER = new Deserializer<>() {
        @Override
        public AstraLightningParticleOptions fromCommand(ParticleType<AstraLightningParticleOptions> type,
                StringReader reader) throws CommandSyntaxException {
            Vec3 start = readPosition(reader);
            Vec3 end = readPosition(reader);
            reader.expect(' ');
            int color = ColorUtils.readRgb(reader);
            reader.expect(' ');
            int maxEndpoints = reader.readInt();
            reader.expect(' ');
            long seed = reader.readLong();
            reader.expect(' ');
            float thickness = reader.readFloat();
            reader.expect(' ');
            int lifetime = reader.readInt();
            reader.expect(' ');
            float alpha = reader.readFloat();
            reader.expect(' ');
            boolean fade = reader.readBoolean();
            reader.expect(' ');
            double randomness = reader.readDouble();
            reader.expect(' ');
            float starRadius = reader.readFloat();
            try {
                return new AstraLightningParticleOptions(start, end, color, thickness, lifetime, alpha, fade,
                        randomness, maxEndpoints, starRadius, seed);
            } catch (IllegalArgumentException exception) {
                throw new SimpleCommandExceptionType(Component.literal(exception.getMessage())).createWithContext(reader);
            }
        }

        private Vec3 readPosition(StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            double x = reader.readDouble();
            reader.expect(' ');
            double y = reader.readDouble();
            reader.expect(' ');
            return new Vec3(x, y, reader.readDouble());
        }

        @Override
        public AstraLightningParticleOptions fromNetwork(ParticleType<AstraLightningParticleOptions> type,
                FriendlyByteBuf buffer) {
            Vec3 start = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
            Vec3 end = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
            return new AstraLightningParticleOptions(start, end, buffer.readInt(), buffer.readFloat(),
                    buffer.readVarInt(), buffer.readFloat(), buffer.readBoolean(), buffer.readDouble(),
                    buffer.readVarInt(), buffer.readFloat(), buffer.readLong());
        }
    };

    public final Vec3 start, end;
    public final int color, lifetime, maxEndpoints;
    public final float thickness, alpha, starRadius;
    public final boolean fade;
    public final double randomness;
    public final long seed;

    public AstraLightningParticleOptions(Vec3 start, Vec3 end, int color, int maxEndpoints, long seed) {
        this(start, end, color, 0.05f, 20, 1f, true, 0.7, maxEndpoints, 0.22f, seed);
    }

    public AstraLightningParticleOptions(Vec3 start, Vec3 end, int color, float thickness, int lifetime,
            float alpha, boolean fade, double randomness, int maxEndpoints, float starRadius, long seed) {
        if (!validPosition(start) || !validPosition(end)) {
            throw new IllegalArgumentException("星座闪电坐标必须是世界范围内的有限值");
        }
        if (!Float.isFinite(thickness) || !Float.isFinite(alpha) || !Double.isFinite(randomness)
                || !Float.isFinite(starRadius)) {
            throw new IllegalArgumentException("星座闪电尺寸、透明度与扰动必须是有限值");
        }
        this.start = start;
        this.end = end;
        this.color = color & 0xFFFFFF;
        this.thickness = Mth.clamp(thickness, 0.001f, 4f);
        this.lifetime = Mth.clamp(lifetime, 1, 1200);
        this.alpha = Mth.clamp(alpha, 0f, 1f);
        this.fade = fade;
        this.randomness = Mth.clamp(randomness, 0, 64);
        // 总节点数包括起点与终点；短距离会减少节点，重合端点只绘制一颗星。
        this.maxEndpoints = Mth.clamp(maxEndpoints, 2, MAX_ENDPOINTS);
        this.starRadius = Mth.clamp(starRadius, 0.01f, 8f);
        this.seed = seed;
    }

    private static boolean validPosition(Vec3 value) {
        return value != null && Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z)
                && Math.abs(value.x) <= 3.0E7 && Math.abs(value.y) <= 3.0E7 && Math.abs(value.z) <= 3.0E7;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeDouble(start.x);
        buffer.writeDouble(start.y);
        buffer.writeDouble(start.z);
        buffer.writeDouble(end.x);
        buffer.writeDouble(end.y);
        buffer.writeDouble(end.z);
        buffer.writeInt(color);
        buffer.writeFloat(thickness);
        buffer.writeVarInt(lifetime);
        buffer.writeFloat(alpha);
        buffer.writeBoolean(fade);
        buffer.writeDouble(randomness);
        buffer.writeVarInt(maxEndpoints);
        buffer.writeFloat(starRadius);
        buffer.writeLong(seed);
    }

    @Override
    public @NotNull String writeToString() {
        return String.format(Locale.ROOT, "fantasydesire:astra_lightning %s %s %s %s %s %s %s %d %d %s %d %s %b %s %s",
                plain(start.x), plain(start.y), plain(start.z), plain(end.x), plain(end.y), plain(end.z),
                ColorUtils.formatRgb(color), maxEndpoints, seed, plain(thickness), lifetime, plain(alpha), fade,
                plain(randomness), plain(starRadius));
    }

    private static String plain(Number value) {
        // Brigadier 的数字读取不接受科学计数法，仍需保留远处和微小坐标的精度。
        return new java.math.BigDecimal(value.toString()).toPlainString();
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return FDParticles.ASTRA_LIGHTNING.get();
    }
}
