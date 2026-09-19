package tennouboshiuzume.mods.FantasyDesire.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import tennouboshiuzume.mods.FantasyDesire.init.FDParticles;
import tennouboshiuzume.mods.FantasyDesire.utils.ColorUtils;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.stream.Stream;

/** 独立十字星芒；位置和速度走原版粒子参数，长度为格、旋转为度、寿命为 tick。 */
public final class AstraStarParticleOptions implements ParticleOptions {
    private static final Codec<Integer> RGB_CODEC = Codec.STRING.comapFlatMap(value -> {
        try {
            return DataResult.success(parseColor(value));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }, ColorUtils::formatRgb);
    private static final Codec<Integer> LIFETIME_CODEC = Codec.INT.comapFlatMap(value -> value >= 1 && value <= 1200
            ? DataResult.success(value) : DataResult.error(() -> "星芒 lifetime 必须在 1–1200 tick 内"), value -> value);

    public static final Codec<AstraStarParticleOptions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RGB_CODEC.fieldOf("color").forGetter(o -> o.color),
            withDefault("radius", finiteRange(0.01f, 8f), 0.25f).forGetter(o -> o.radius),
            withDefault("width", finiteRange(0.001f, 4f), 0.025f).forGetter(o -> o.width),
            withDefault("alpha", finiteRange(0f, 1f), 1f).forGetter(o -> o.alpha),
            withDefault("trailLength", finiteRange(0f, 64f), 1f).forGetter(o -> o.trailLength),
            withDefault("rotation", finiteRange(-Float.MAX_VALUE, Float.MAX_VALUE), 0f).forGetter(o -> o.rotation),
            withDefault("lifetime", LIFETIME_CODEC, 20).forGetter(o -> o.lifetime)
    ).apply(instance, AstraStarParticleOptions::new));

    public static final Deserializer<AstraStarParticleOptions> DESERIALIZER = new Deserializer<>() {
        @Override
        public AstraStarParticleOptions fromCommand(ParticleType<AstraStarParticleOptions> type,
                StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            String color = reader.readString();
            reader.expect(' ');
            float radius = reader.readFloat();
            reader.expect(' ');
            float width = reader.readFloat();
            reader.expect(' ');
            float alpha = reader.readFloat();
            reader.expect(' ');
            float trailLength = reader.readFloat();
            reader.expect(' ');
            float rotation = reader.readFloat();
            reader.expect(' ');
            int lifetime = reader.readInt();
            try {
                return new AstraStarParticleOptions(parseColor(color), radius, width, alpha, trailLength,
                        rotation, lifetime);
            } catch (IllegalArgumentException exception) {
                throw new SimpleCommandExceptionType(Component.literal(exception.getMessage())).createWithContext(reader);
            }
        }

        @Override
        public AstraStarParticleOptions fromNetwork(ParticleType<AstraStarParticleOptions> type,
                FriendlyByteBuf buffer) {
            return new AstraStarParticleOptions(buffer.readInt(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readVarInt());
        }
    };

    public final int color, lifetime;
    public final float radius, width, alpha, trailLength, rotation;

    public AstraStarParticleOptions(int color) {
        this(color, 0.25f, 0.025f, 1f, 1f, 0f, 20);
    }

    public AstraStarParticleOptions(int color, float radius, float width, float alpha, float trailLength,
            float rotation, int lifetime) {
        if (color < 0 || color > 0xFFFFFF) {
            throw new IllegalArgumentException("星芒颜色必须是 0xRRGGBB，透明度请使用 alpha");
        }
        requireRange("radius", radius, 0.01f, 8f);
        requireRange("width", width, 0.001f, 4f);
        requireRange("alpha", alpha, 0f, 1f);
        requireRange("trailLength", trailLength, 0f, 64f);
        requireRange("rotation", rotation, -Float.MAX_VALUE, Float.MAX_VALUE);
        if (lifetime < 1 || lifetime > 1200) {
            throw new IllegalArgumentException("星芒 lifetime 必须在 1–1200 tick 内");
        }
        this.color = color;
        this.radius = radius;
        this.width = width;
        this.alpha = alpha;
        this.trailLength = trailLength;
        // 先取余再计算三角函数，避免极大角度丢失精度；正角度在屏幕平面内逆时针旋转。
        this.rotation = rotation % 360f;
        this.lifetime = lifetime;
    }

    private static int parseColor(String value) {
        if (!value.matches("0[xX][0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("星芒颜色必须使用 0xRRGGBB 格式");
        }
        return ColorUtils.parseRgb(value);
    }

    private static Codec<Float> finiteRange(float min, float max) {
        return Codec.FLOAT.comapFlatMap(value -> Float.isFinite(value) && value >= min && value <= max
                ? DataResult.success(value)
                : DataResult.error(() -> "星芒参数必须是 [" + min + ", " + max + "] 内的有限值"), value -> value);
    }

    private static <A> MapCodec<A> withDefault(String name, Codec<A> codec, A defaultValue) {
        // 1.20.1 的 optionalFieldOf 会吞掉非法字段；只允许缺失时回退默认值。
        return new MapCodec<>() {
            @Override
            public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
                T value = input.get(name);
                return value == null ? DataResult.success(defaultValue) : codec.parse(ops, value);
            }

            @Override
            public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return prefix.add(name, codec.encodeStart(ops, input));
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.of(ops.createString(name));
            }
        };
    }

    private static void requireRange(String name, float value, float min, float max) {
        if (!Float.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException("星芒 " + name + " 必须是 [" + min + ", " + max + "] 内的有限值");
        }
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeInt(color);
        buffer.writeFloat(radius);
        buffer.writeFloat(width);
        buffer.writeFloat(alpha);
        buffer.writeFloat(trailLength);
        buffer.writeFloat(rotation);
        buffer.writeVarInt(lifetime);
    }

    @Override
    public @NotNull String writeToString() {
        return String.format(Locale.ROOT, "fantasydesire:astra_star %s %s %s %s %s %s %d",
                ColorUtils.formatRgb(color), plain(radius), plain(width), plain(alpha), plain(trailLength),
                plain(rotation), lifetime);
    }

    private static String plain(float value) {
        // Brigadier 不读取科学计数法，微小透明度与角度仍需完整往返。
        return new BigDecimal(Float.toString(value)).toPlainString();
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return FDParticles.ASTRA_STAR.get();
    }
}
