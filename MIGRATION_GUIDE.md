# FantasyDesire 物品注册系统迁移指南

## 概述

前置mod SlashBlade已经从旧的静态字段方式（SBItems）迁移到了新的DeferredRegister方式（SlashBladeItems）。本指南说明如何将FantasyDesire mod更新到符合新规范。

## 旧规范 vs 新规范

### 旧规范（已弃用）
```java
// 使用 @ObjectHolder 注解
@ObjectHolder(registryName = "minecraft:item", value = "modid:item")
public static final Item item = null;

// 或使用静态字段
public static final Item item = SomeRegistry.ITEM.get();
```

### 新规范（推荐）
```java
// 使用 DeferredRegister
public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

public static final RegistryObject<Item> ITEM = ITEMS.register("item", () ->
        new CustomItem(new Item.Properties()));

// 使用时调用 .get()
public static final Item item = ITEM.get();
```

## 迁移步骤

### 1. 创建新的FDItemsRegistry类

**文件位置：** `src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDItemsRegistry.java`

```java
package tennouboshiuzume.mods.FantasyDesire.init;

import mods.flammpfeil.slashblade.item.ItemTierSlashBlade;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;
import tennouboshiuzume.mods.FantasyDesire.items.fantasyslashblade.ItemFantasySlashBlade;

public class FDItemsRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FantasyDesire.MODID);

    public static final RegistryObject<Item> FANTASY_SLASHBLADE = ITEMS.register("fantasyslashblade", () ->
            new ItemFantasySlashBlade(new ItemTierSlashBlade(40, 4F), 4, -2.4F, new Item.Properties()));

    public static void register(net.minecraftforge.eventbus.api.IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
```

### 2. 更新FDItems类为兼容层

**文件位置：** `src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDItems.java`

```java
package tennouboshiuzume.mods.FantasyDesire.init;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ObjectHolder;
import org.jetbrains.annotations.NotNull;
import tennouboshiuzume.mods.FantasyDesire.FantasyDesire;

public class FDItems {
    @NotNull
    @Deprecated
    public static Item fantasyslashblade = FDItemsRegistry.FANTASY_SLASHBLADE.get();
}
```

### 3. 更新主类注册

**文件位置：** `src/main/java/tennouboshiuzume/mods/FantasyDesire/FantasyDesire.java`

```java
public FantasyDesire() {
    IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
    // 其他注册...
    FDItemsRegistry.register(eventBus); // 添加新注册
}

// 移除旧的RegisterEvent注册方法
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public static class RegistryEvents {
    // 移除这个方法，使用FDItemsRegistry替代
    // @SubscribeEvent
    // public static void register(RegisterEvent event) { ... }
}
```

### 4. 更新所有引用

#### 在数据生成器中
```java
// 旧方式
import tennouboshiuzume.mods.FantasyDesire.init.FDItems;
FDItems.fantasyslashblade

// 新方式
import tennouboshiuzume.mods.FantasyDesire.init.FDItemsRegistry;
FDItemsRegistry.FANTASY_SLASHBLADE.get()
```

#### 在客户端处理器中
```java
// 旧方式
import tennouboshiuzume.mods.FantasyDesire.init.FDItems;
ItemProperties.register(FDItems.fantasyslashblade, ...);

// 新方式
import tennouboshiuzume.mods.FantasyDesire.init.FDItemsRegistry;
ItemProperties.register(FDItemsRegistry.FANTASY_SLASHBLADE.get(), ...);
```

#### 在刀定义中
```java
// 旧方式
public ItemStack getBlade() {
    return this.getBlade(FDItems.fantasyslashblade);
}

// 新方式
public ItemStack getBlade() {
    return this.getBlade(FDItemsRegistry.FANTASY_SLASHBLADE.get());
}
```

### 5. 更新前置mod引用

```java
// 旧方式
import mods.flammpfeil.slashblade.init.SBItems;
SBItems.slashblade
SBItems.proudsoul_crystal.get()

// 新方式
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
SlashBladeItems.SLASHBLADE.get()
SlashBladeItems.PROUDSOUL_CRYSTAL.get()
```

## 已完成的迁移

### ✅ 核心文件更新
- **FDItemsRegistry.java** - 新建，符合新规范
- **FDItems.java** - 更新为兼容层，标记为@Deprecated
- **FantasyDesire.java** - 移除旧的RegisterEvent，添加FDItemsRegistry注册

### ✅ 引用更新
- **FantasySlashBladeDefinition.java** - 使用FDItemsRegistry替代FDItems
- **ClientHandler.java** - 使用FDItemsRegistry替代FDItems
- **FantasySlashBladeRecipeProvider.java** - 使用SlashBladeItems替代SBItems
- **FDTab.java** - 使用SlashBladeItems替代SBItems

## 新规范的优势

### 1. 类型安全
```java
// 旧方式：可能为null
Item item = FDItems.fantasyslashblade;

// 新方式：保证非null
Item item = FDItemsRegistry.FANTASY_SLASHBLADE.get();
```

### 2. 编译时检查
```java
// 旧方式：运行时才能发现错误
Item item = FDItems.wrongitem; // 编译通过，运行时错误

// 新方式：编译时就能发现错误
Item item = FDItemsRegistry.WRONG_ITEM.get(); // 编译错误
```

### 3. 更好的IDE支持
```java
// 新方式：IDE可以自动完成和导航
FDItemsRegistry.FANTASY_SLASHBLADE.get()
```

### 4. 符合现代Forge规范
- 与前置mod保持一致
- 遵循Forge最佳实践
- 更容易维护和扩展

## 迁移检查清单

- [ ] 创建FDItemsRegistry类
- [ ] 更新FDItems为兼容层
- [ ] 在主类中注册FDItemsRegistry
- [ ] 移除旧的RegisterEvent方法
- [ ] 更新所有FDItems引用
- [ ] 更新所有SBItems引用
- [ ] 测试物品注册是否正常
- [ ] 测试合成表是否正常
- [ ] 测试创造模式物品栏是否正常

## 常见问题

### Q: 为什么要保留FDItems类？
A: 为了向后兼容。保留FDItems类作为兼容层，可以逐步迁移所有引用，避免一次性修改太多代码。

### Q: 什么时候可以完全移除FDItems？
A: 当所有引用都更新到FDItemsRegistry后，可以完全移除FDItems类。

### Q: 新规范会影响性能吗？
A: 不会。DeferredRegister在游戏启动时注册，运行时性能相同。

### Q: 如何添加新物品？
A: 在FDItemsRegistry中添加新的RegistryObject：
```java
public static final RegistryObject<Item> NEW_ITEM = ITEMS.register("new_item", () ->
        new CustomItem(new Item.Properties()));
```

## 参考资源

- [Forge Documentation - DeferredRegister](https://docs.minecraftforge.net/en/latest/concepts/registries/)
- [SlashBlade Source Code](https://github.com/Flammpfeil/SlashBlade)
- [Minecraft Forge Best Practices](https://docs.minecraftforge.net/en/latest/concepts/bestpractices/)

## 总结

通过这次迁移，FantasyDesire mod现在完全符合前置mod SlashBlade的新规范，提供了更好的类型安全性和可维护性。建议在后续开发中直接使用FDItemsRegistry，逐步淘汰旧的FDItems引用。