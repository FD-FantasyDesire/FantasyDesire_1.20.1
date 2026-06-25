# FantasyDesire SpecialEffect 重构设计方案

本文档记录了关于 `FDSpecialEffectBase` 及其相关特效系统的总线式架构重构方案，以备后续进行高版本移植和大规模重构时参考。

## 1. 重构的核心目标与思路

目前的特效逻辑往往分散在多个事件监听器中，存在大量冗余的条件判断（如：是否是特定武器、是否有特定附魔、是否是拔刀剑等），并且随着特效增加，事件监听器数量爆炸，不仅难以维护，也严重影响性能。

**核心思路：总线式设计 + 注册表驱动**

- **注册表驱动：** 所有的特殊效果作为单独的实例（单例或基于工厂）注册到一个统一的注册表中（`FDSpecialEffectsRegistry`）。
- **条件解耦（Predicate）：** 将触发条件（哪把刀、哪个事件、什么附魔级别）抽象为 `Predicate<EventContext>` 存储在效果类本身。
- **统一总线（EventDispatcher）：** 游戏事件（如 `LivingHurtEvent`, `TickEvent`）统一由一个（或极少数几个）事件总线接收。总线将事件包装成上下文，然后遍历注册表，通过 `Predicate` 筛选出符合条件的效果，并调用其执行逻辑。
- **分离粗筛与精细处理：** 注册表负责基础事件类型的粗筛，效果类内部处理复杂的渐进式/多条件叠加逻辑。

## 2. 核心基类 `FDSpecialEffectBase` 示例

基类主要负责存储判断条件并定义统一的执行接口。

```java
public abstract class FDSpecialEffectBase<T extends Event> {
    private final List<Predicate<EffectContext<T>>> conditions = new ArrayList<>();

    // 链式添加触发条件
    public FDSpecialEffectBase<T> addCondition(Predicate<EffectContext<T>> condition) {
        this.conditions.add(condition);
        return this;
    }

    // 判断当前上下文是否满足所有触发条件
    public boolean canTrigger(EffectContext<T> context) {
        for (Predicate<EffectContext<T>> condition : conditions) {
            if (!condition.test(context)) {
                return false;
            }
        }
        return true;
    }

    // 实际执行效果的抽象方法
    public abstract void doEffect(EffectContext<T> context);
}

// 封装事件及其周边数据的上下文记录
public record EffectContext<T extends Event>(T event, ItemStack blade, LivingEntity user, LivingEntity target) {}
```

## 3. 具体效果子类示例（如 `ChikeFlareEffect`）

子类专注于具体的业务逻辑，即 `doEffect`。

```java
public class ChikeFlareAttackEffect extends FDSpecialEffectBase<LivingHurtEvent> {

    @Override
    public void doEffect(EffectContext<LivingHurtEvent> context) {
        LivingHurtEvent event = context.event();
        LivingEntity target = context.target();

        // 具体的特效逻辑：例如产生爆炸、附加特殊的 Debuff 等
        target.level().explode(context.user(), target.getX(), target.getY(), target.getZ(), 2.0F, Level.ExplosionInteraction.NONE);
        // ... 其他视觉或伤害逻辑 ...
    }
}
```

## 4. 注册表 `FDSpecialEffectsRegistry` 示例

注册表管理所有效果的生命周期，并利用基类的 `addCondition` 实现链式配置。

```java
public class FDSpecialEffectsRegistry {
    // 按事件类型分类存储效果，便于快速检索
    private static final Map<Class<? extends Event>, List<FDSpecialEffectBase<?>>> EFFECTS = new HashMap<>();

    public static void init() {
        // 注册 ChikeFlare 的攻击效果
        register(LivingHurtEvent.class, new ChikeFlareAttackEffect()
            .addCondition(ctx -> ctx.blade() != null && isChikeFlare(ctx.blade()))
            .addCondition(ctx -> ctx.user() instanceof Player)
        );

        // 可以继续注册其他效果
    }

    private static <T extends Event> void register(Class<T> eventClass, FDSpecialEffectBase<T> effect) {
        EFFECTS.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(effect);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Event> List<FDSpecialEffectBase<T>> getEffectsFor(Class<T> eventClass) {
        return (List<FDSpecialEffectBase<T>>) (List<?>) EFFECTS.getOrDefault(eventClass, Collections.emptyList());
    }

    private static boolean isChikeFlare(ItemStack stack) {
        // ... 判断物品是否为特定武器 ...
        return true;
    }
}
```

## 5. 事件分发器 `SEEventDispatcher` 示例

作为 Forge/NeoForge 事件的唯一（或主要）入口点，负责拦截事件并分发给注册表中的效果。

```java
@Mod.EventBusSubscriber(modid = "fantasydesire", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SEEventDispatcher {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 构建上下文（这里需要提取武器、攻击者等通用逻辑）
        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity user)) return;

        ItemStack heldItem = user.getMainHandItem();
        // 判断是否为拔刀剑，避免后续每个特效都去判断一次
        if (!(heldItem.getItem() instanceof ItemSlashBlade)) return;

        EffectContext<LivingHurtEvent> context = new EffectContext<>(event, heldItem, user, event.getEntity());

        // 获取并触发所有满足条件的 LivingHurtEvent 效果
        for (FDSpecialEffectBase<LivingHurtEvent> effect : FDSpecialEffectsRegistry.getEffectsFor(LivingHurtEvent.class)) {
            if (effect.canTrigger(context)) {
                effect.doEffect(context);
            }
        }
    }
}
```

## 6. 性能优化分析

- **减少事件监听器数量：** 通过 `SEEventDispatcher` 集中处理事件，将原本分散在数十个类中的 `@SubscribeEvent` 收拢为每种事件仅有一个入口。这显著降低了事件总线的反射调用和分发开销。
- **避免冗余的条件检查：**
  - **通用检查前置：** 在 Dispatcher 阶段，统一进行 `source instanceof LivingEntity` 和 `heldItem.getItem() instanceof ItemSlashBlade` 等高频检查。
  - **失败快速返回：** 由于采用了按条件列表 `Predicate` 的短路运算，一旦某把武器不符合条件，立即跳过该效果的后续检查。
- **缓存与分类：** 注册表按 Event Class 分类存储效果列表，事件触发时只需遍历关注该事件的极少部分效果实例。

## 7. 渐进式/多条件叠加效果的最佳实践

对于那些具有不同阶段、随着附魔等级或连击数增强的效果：

- **粗筛交由注册表：** 注册表的 `Predicate` 仅负责筛选“是否触发该类型的特效”（如是否手持该武器、是否造成暴击）。
- **精细逻辑由 `doEffect` 处理：** `doEffect` 内部再去读取上下文数据（如附魔等级 `EnchantmentHelper.getItemEnchantmentLevel(...)`，或者玩家的连击评价 `Rank`）。
- **设计原则：** 不要在 `Predicate` 中写入过于复杂的业务逻辑；`Predicate` 只是守门员，`doEffect` 才是真正的演员。这样能确保架构的清晰和可维护性。
