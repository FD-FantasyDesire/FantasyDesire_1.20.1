package tennouboshiuzume.mods.FantasyDesire.entity.phantomsword;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** 只注册工厂；每枚剑各自持有实例，避免共享目标、计数器或命中记录。 */
public final class BehaviorRegistry<T> {
    private final Map<ResourceLocation, Supplier<? extends T>> factories = new HashMap<>();

    public synchronized ResourceLocation register(ResourceLocation id, Supplier<? extends T> factory) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(factory);
        if (factories.putIfAbsent(id, factory) != null)
            throw new IllegalArgumentException("重复的幻影剑行为 ID: " + id);
        return id;
    }

    public synchronized ResourceLocation resolve(String id, ResourceLocation fallback) {
        ResourceLocation parsed = id == null ? null : ResourceLocation.tryParse(id);
        return factories.containsKey(parsed) ? parsed : fallback;
    }

    public synchronized T create(ResourceLocation id) {
        Supplier<? extends T> factory = factories.get(id);
        if (factory == null)
            throw new IllegalArgumentException("未注册的幻影剑行为 ID: " + id);
        return Objects.requireNonNull(factory.get());
    }
}
