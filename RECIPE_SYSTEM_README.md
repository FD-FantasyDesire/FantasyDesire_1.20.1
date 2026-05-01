# FantasyDesire 合成表序列化生成系统

## 系统概述

本系统为FantasyDesire mod提供了完整的合成表序列化生成机制，完全基于前置mod SlashBlade的设计，专门适配了`fantasydesire:fantasyslashblade`物品。

## 核心组件

### 1. FantasySlashBladeShapedRecipe
合成表实现类，继承自`ShapedRecipe`，负责处理合成逻辑。

**主要功能：**
- 支持输出`fantasydesire:fantasyslashblade`物品
- 自动处理刀的状态合并（ProudSoul、KillCount、Refine）
- 支持从`FantasySlashBladeDefinition`注册表获取刀的定义
- 自动合并和升级附魔

### 2. FantasySlashBladeShapedRecipeBuilder
合成表构建器，提供流式API来构建合成表。

**主要方法：**
- `shaped(ResourceLocation blade)` - 创建指定刀的合成表
- `define(Character key, Ingredient ingredient)` - 定义配方键
- `pattern(String pattern)` - 定义配方模式
- `unlockedBy(String key, CriterionTriggerInstance trigger)` - 设置解锁条件

### 3. FantasySlashBladeShapedRecipeSerializer
JSON序列化器，负责合成表的序列化和反序列化。

**主要功能：**
- 将合成表序列化为JSON格式
- 从JSON反序列化为合成表对象
- 处理网络传输

### 4. FDRecipeSerializerRegistry
序列化器注册类，注册`fantasydesire:shaped_fantasy_blade`合成表类型。

### 5. FantasySlashBladeRecipeProvider
数据生成器，自动生成所有刀的合成表JSON文件。

## 使用方法

### 1. 在DataGen中注册RecipeProvider
已在`DataGen.java`中完成：
```java
dataGenerator.addProvider(event.includeServer(), new FantasySlashBladeRecipeProvider(packOutput));
```

### 2. 在主类中注册RecipeSerializerRegistry
已在`FantasyDesire.java`中完成：
```java
FDRecipeSerializerRegistry.register(eventBus);
```

### 3. 在FantasySlashBladeRecipeProvider中定义合成表

**基础合成表示例：**
```java
FantasySlashBladeShapedRecipeBuilder.shaped(FantasySlashBladeBuiltInRegistry.ChikeFlare.location())
    .pattern("  L").pattern(" L ").pattern("BG ")
    .define('B', SlashBladeIngredient.of(SBItems.slashblade,
            RequestDefinition.Builder.newInstance().killCount(100).addSwordType(SwordType.BROKEN).build()))
    .define('L', Ingredient.of(Items.GOLD_INGOT))
    .define('G', Ingredient.of(Tags.Items.INGOTS_GOLD))
    .unlockedBy(getHasName(SBItems.slashblade), has(SBItems.slashblade))
    .save(consumer, FantasyDesire.prefix("chikeflare"));
```

**升级合成表示例：**
```java
FantasySlashBladeShapedRecipeBuilder.shaped(FantasySlashBladeBuiltInRegistry.SmartPistolB.location())
    .pattern(" EI").pattern("PBD").pattern("SI ")
    .define('B', SlashBladeIngredient.of(FantasySlashBladeBuiltInRegistry.SmartPistolA.location(),
            RequestDefinition.Builder.newInstance().killCount(200).build()))
    .define('I', Ingredient.of(Tags.Items.INGOTS_IRON))
    .define('S', Ingredient.of(Tags.Items.STRING))
    .define('P', Ingredient.of(Items.PAPER))
    .define('E', Ingredient.of(Items.ENDER_PEARL))
    .define('D', Ingredient.of(Tags.Items.DYES_GREEN))
    .unlockedBy(getHasName(FDItems.fantasyslashblade), has(FDItems.fantasyslashblade))
    .save(consumer, FantasyDesire.prefix("smart_pistol_b"));
```

## 合成表JSON格式

```json
{
  "type": "fantasydesire:shaped_fantasy_blade",
  "pattern": [
    "  L",
    " L ",
    "BG "
  ],
  "key": {
    "B": {
      "type": "slashblade:ingredient",
      "item": "slashblade:slashblade",
      "request": {
        "name": "slashblade:none",
        "proud_soul": 0,
        "kill": 100,
        "refine": 0,
        "enchantments": [],
        "sword_type": ["broken"]
      }
    },
    "L": {"item": "minecraft:gold_ingot"},
    "G": {"tag": "forge:ingots/gold"}
  },
  "result": {
    "item": "fantasydesire:fantasyslashblade"
  },
  "blade": "fantasydesire:chikeflare",
  "show_notification": true
}
```

## 关键特性

### 输出物品
所有合成表都输出`fantasydesire:fantasyslashblade`，通过`blade`字段指定从哪个`FantasySlashBladeDefinition`获取刀的属性。

### 状态要求
使用`SlashBladeIngredient`和`RequestDefinition`指定输入刀的状态要求：
- `name` - 刀的名称
- `proud_soul` - 最小ProudSoul数量
- `kill` - 最小击杀数
- `refine` - 最小精炼等级
- `enchantments` - 附魔要求
- `sword_type` - 剑类型要求

### 状态合并
合成时自动合并输入刀的状态：
- ProudSoul：累加
- KillCount：累加
- Refine：根据配置累加或取最大值

### 附魔合并
自动合并和升级附魔，确保附魔兼容性。

## 已实现的合成表

系统已为以下刀创建了合成表：
- ChikeFlare - 基础幻想刀
- SmartPistolA/B - 智能手枪系列
- CrimsonScythe - 深红镰刀
- TwinBladeL/R - 双刀系列
- PureSnow - 纯雪
- ArdorBlossomStar - 炽热星辰
- StarlessNight - 无星之夜
- Crucible - 熔炉
- GireiKen - 义剑
- OverCold系列 - 极寒系列

## 运行数据生成

运行以下命令生成合成表JSON文件：
```bash
./gradlew runData
```

生成的文件将位于：`src/generated/resources/data/fantasydesire/recipes/`

## 系统兼容性

本系统完全兼容前置mod SlashBlade的设计：
- 使用相同的`SlashBladeIngredient`和`RequestDefinition`
- 支持相同的刀状态系统
- 兼容相同的附魔系统
- 可以与SlashBlade的刀进行合成

## 扩展说明

要添加新的合成表，只需在`FantasySlashBladeRecipeProvider`的`buildRecipes`方法中添加相应的构建代码即可。系统会自动生成对应的JSON文件。

## 注意事项

1. 确保所有刀都在`FantasySlashBladeBuiltInRegistry`中注册
2. 合成表的`blade`字段必须对应已注册的刀定义
3. 使用`SlashBladeIngredient`时，确保`RequestDefinition`的要求合理
4. 合成表解锁条件应该与游戏进度相匹配

## 技术支持

如有问题，请参考前置mod SlashBlade的文档或查看示例代码。