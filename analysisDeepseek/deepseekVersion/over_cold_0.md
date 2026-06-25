# OverCold - 冰霜伊始 (P0阶段)

> **永冻纪元 四阶段进化 · 第一阶段**
> 对应注册键: `over_cold_0`
> 能量名称: `Evolution_0` | 特殊类型: `OverCold_0`

---

## 一、基础属性提取

### 1.1 核心数值

| 属性             | 数值                  | 说明                         |
| ---------------- | --------------------- | ---------------------------- |
| **特效色**       | `0x6699FF`            | 冰蓝色调，贯穿全进化阶段不变 |
| **模型**         | `overcold_0.obj`      | 初始冰霜形态，外观最为朴素   |
| **纹理**         | `models/overcold.png` | 全阶段共用纹理               |
| **基础攻击修正** | **3.2F**              | 最低阶段，略高于普通武器基础 |
| **maxDamage**    | 144                   | 全阶段统一耐久上限           |
| **能量名称**     | `Evolution_0`         | 标识当前为进化第0阶段        |
| **最大特殊能量** | **300**               | 最低容量，凑满可升P1         |
| **specialType**  | `OverCold_0`          | 用于运行时识别阶段           |

### 1.2 注册参数对照

参见 [`FantasySlashBladeBuiltInRegistry.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:237)

```java
// P0 注册 (L237-261)
bootstrap.register(OverColdP0, new FantasySlashBladeDefinition(
    FantasyDesire.prefix("over_cold"),
    RenderDefinition.Builder.newInstance()
        .effectColor(0x6699FF)
        .textureName(FantasyDesire.prefix("models/overcold.png"))
        .modelName(FantasyDesire.prefix("models/overcold_0.obj"))
        .standbyRenderType(CarryType.RNINJA)
        .build(),
    PropertiesDefinition.Builder.newInstance()
        .baseAttackModifier(3.2F)
        .defaultSwordType(List.of(SwordType.BEWITCHED))
        .maxDamage(144)
        .addSpecialEffect(FDSpecialEffectsRegistry.EvolutionIce.getId())
        .addSpecialEffect(FDSpecialEffectsRegistry.ColdLeak.getId())
        .slashArtsType(FDSlashArtRegistry.FREEZE_ZERO.getId())
        .build(),
    FantasyDefinition.Builder.newInstance()
        .specialChargeName("Evolution_0")
        .maxSpecialCharge(300)
        .specialType("OverCold_0")
        .build(),
    List.of(new EnchantmentDefinition(getEnchantmentID(Enchantments.FROST_WALKER), 5),
            new EnchantmentDefinition(getEnchantmentID(Enchantments.FIRE_PROTECTION), 3),
            new EnchantmentDefinition(getEnchantmentID(Enchantments.UNBREAKING), 3))
));
```

### 1.3 共有属性（全阶段一致）

| 属性                    | 值                                                  |
| ----------------------- | --------------------------------------------------- |
| **SA (Special Attack)** | `FREEZE_ZERO`（冰霜风暴）                           |
| **SE (Special Effect)** | `EvolutionIce`（冰霜进化）+ `ColdLeak`（寒冰泄露）  |
| **携带方式**            | `CarryType.RNINJA`（忍者背持）                      |
| **剑类型**              | `SwordType.BEWITCHED`（被诅咒的剑）                 |
| **附魔**                | FROST_WALKER V, FIRE_PROTECTION III, UNBREAKING III |

---

## 二、SA技能分析 - FreezeZero（冰霜风暴）

### 2.1 SA注册信息

参见 [`FDSlashArtRegistry.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSlashArtRegistry.java:28)

```java
public static final RegistryObject<SlashArts> FREEZE_ZERO = FD_SLASH_ARTS.register("freeze_zero",
    () -> new FDSlashArts((e) -> FDCombo.FREEZE_ZERO.getId(), 8)
        .setComboStateJust((e) -> FDCombo.FREEZE_ZERO_JUST.getId()));
```

- **消耗点数**: **8**（较高的SA消耗）
- **JUST变体**: 存在 `FREEZE_ZERO_JUST`（`freeze_zero_just`），速度0.75倍，优先级45

### 2.2 SA触发流程

参见 [`FreezeZero.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/FreezeZero.java:17)

```java
public static void FreezeZero(LivingEntity entity) {
    // 1. 通过AntiNTR验证是否为OverCold持有者
    // 2. 获取当前specialType并解析进化等级
    int evolutionTier = OverColdEffects.getEvolutionTier(specialType); // P0 → 0
    // 3. 检查是否已有FROST_STORM效果
    if (current == null) {
        StartStorm(entity, evolutionTier);   // 首次使用 → 启动风暴
    } else {
        StackStorm(entity, evolutionTier, current); // 已有风暴 → 叠加
    }
}
```

### 2.3 P0阶段的StartStorm()

```java
public static void StartStorm(LivingEntity entity, int evolutionTier) {
    entity.addEffect(
        new MobEffectInstance(FDPotionEffects.FROST_STORM.get(),
            20 * (6 + evolutionTier * 3),  // P0: 20 * (6 + 0*3) = 120 ticks = 6秒
            evolutionTier));                // P0: 增幅器 = 0
}
```

| 属性                 | P0值             |
| -------------------- | ---------------- |
| **基础持续时间**     | `6 + 0*3 = 6` 秒 |
| **首次风暴持续时间** | 120 ticks（6秒） |
| **风暴增幅器**       | **0级**（最弱）  |

### 2.4 P0阶段的StackStorm()

```java
public static void StackStorm(LivingEntity entity, int evolutionTier, MobEffectInstance current) {
    int durationExtension = Math.max(20 * evolutionTier * 3, 30);
    // P0: Math.max(20 * 0 * 3, 30) = 30 ticks = 1.5秒
    int newAmplifier = Math.min(current.getAmplifier() + 1, 14);
    // 每叠加一次 +1级，上限14
}
```

> **P0的特点**: 首次风暴仅有6秒/0级增幅器。每次叠加仅延长1.5秒（30 ticks），但叠加后增幅器+1。P0阶段受限于低进化等级，风暴的基础强度和延长幅度都是最低的。

### 2.5 P0在进化链中的SA定位

P0是**冰霜法师流派的起点**。虽然风暴持续时间最短、强度最低，但每次使用SA叠加风暴时**增幅器+1**的特性使得即使P0也能通过频繁使用SA叠加出高等级风暴——只是代价是消耗点数（8点）和较短的持续时间延长。

### 2.6 连段路径

参见 [`FDCombo.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:632)

```
FREEZE_ZERO (检测) → FREEZE_ZERO_0 (施放动画,帧1923-1928) → FREEZE_ZERO_END (收刀,帧1928-1963)
                                                                    ↓
                                                              SlashBlade.prefix("none")
```

- **FREEZE_ZERO_0**: 启动和结束帧1923-1928，优先级50
  - 第1帧调用 `FreezeZero.FreezeZero(entityIn)` 并播放施法音效
  - 超时后自动进入结束状态
- **FREEZE_ZERO_END**: 结束动画1928-1963，包含收刀音效和快速拔刀释放
- **JUST变体** (`FREEZE_ZERO_JUST`): 速度0.75F（更慢但更精准），会循环调用自身直到超时进入结束

```java
// 连段触发核心 (L649-653)
.addTickAction(ComboState.TimeLineTickAction.getBuilder()
    .put(1, entityIn -> {
        FreezeZero.FreezeZero(entityIn);
        entityIn.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0f, 1.2f);
    }).build())
```

---

## 三、特殊效果机制分析 - P0阶段

### 3.1 EvolutionIce（冰霜进化）

参见 [`OverColdEffects.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/overcold/OverColdEffects.java:22)

```java
@SubscribeEvent
public static void OnAddProudSoul(SlashBladeEvent.AddProudSoulEvent event) {
    // 每次获得灵魂点数时检测
    int evolutionTier = getEvolutionTier(fdState.getSpecialType()); // P0 → 0
    int finalMultiple = evolutionTier > 1 ? 3 : 1;  // P0: 倍率=1
    CapabilityUtils.addSpecialCharge(fdState, event.getOriginCount() * finalMultiple);

    // 满值进化检测
    if (fdState.getSpecialCharge() >= fdState.getMaxSpecialCharge() && evolutionTier != 3) {
        switch (evolutionTier) {
            case 0: // P0 → P1
                state.setModel(new ResourceLocation(FantasyDesire.MODID, "models/overcold_1.obj"));
                state.setBaseAttackModifier(4.0f);
                fdState.setSpecialChargeName("Evolution_1");
                fdState.setMaxSpecialCharge(3000);
                fdState.setSpecialType("OverCold_1");
                break;
        }
    }
}
```

**P0阶段的进化条件**:

| 条件               | 值                     |
| ------------------ | ---------------------- |
| **当前能量上限**   | 300                    |
| **进化触发**       | 特殊能量累计达到 ≥ 300 |
| **进化后形态**     | P1（OverCold_1）       |
| **能量收集倍率**   | ×1（基础倍率）         |
| **进化后攻击力**   | 3.2F → **4.0F**        |
| **进化后能量上限** | 300 → **3000**         |

### 3.2 ColdLeak（寒冰泄露）

参见 [`OverColdEffects.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/overcold/OverColdEffects.java:71)

```java
@SubscribeEvent
public static void OnHit(SlashBladeEvent.HitEvent event) {
    // 命中时触发
    int evolutionTier = getEvolutionTier(fdState.getSpecialType()); // P0 → 0
    // 给目标施加FROST_BITE
    target.addEffect(new MobEffectInstance(FDPotionEffects.FROST_BITE.get(), 120, evolutionTier));
    // P0: 120 ticks (6秒), 增幅器0
}
```

**P0阶段的FROST_BITE效果**:

| 属性         | P0值                              |
| ------------ | --------------------------------- |
| **持续时间** | 120 ticks（6秒）                  |
| **增幅器**   | 0级（最弱）                       |
| **减速效果** | `-0.2 * 0 = 0%`（P0无减速效果）   |
| **下拉力**   | `-0.02 * 0 = 0`（P0没有重力下拉） |

> **P0的ColdLeak定位**: 虽然P0阶段的FROST_BITE没有实质性的减速控制效果，但它为后续阶段的冰冻连锁奠定了基础。在P0阶段，FROST_BITE更多是作为一个**状态标记**，配合后续进化产生质变。

### 3.3 FROST_STORM（冰霜风暴）效果 - P0视角

参见 [`FrostStormEffect.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/potioneffect/FrostStormEffect.java:31)

```java
// 每tick执行 (P0: amplifier = 0)
double r = Math.min(4 + 0 * 3.0, 16);  // 半径 = 4格
double yPos = entity.getY() + entity.getBbHeight() + Math.min(4 + 0, 8); // 高度偏移4

// 每2tick生成幻影剑攻击
if (entity.tickCount % 2 == 0) {
    int evolutionTier = 0;  // P0
    // 能量消耗: Math.max(0 - 0, 0) = 0 (P0不消耗能量!)
    // 剑数量: 1 + 0/2 = 1把
    // 伤害: 3.0 + 0 * 5.0 = 3.0
}
```

**P0风暴效果汇总**:

| 属性                  | P0值                                 |
| --------------------- | ------------------------------------ |
| **风暴半径**          | 4格                                  |
| **每2tick能量消耗**   | 0（免费！）                          |
| **每2tick幻影剑数量** | 1把                                  |
| **幻影剑伤害**        | 3.0基础伤害                          |
| **FROST_BITE施加**    | 对范围内敌人每2tick施加5秒FROST_BITE |

> **P0风暴的关键优势**: 由于 `max(amplifier - evolutionTier, 0)` = `max(0-0, 0)` = 0，P0阶段的风暴**不消耗任何特殊能量**，这意味着P0玩家可以在不消耗进化能量的情况下持续维持风暴，利用风暴生成幻影剑和FROST_BITE效果。这是P0的独特战术价值。

---

## 四、五维深度分析摘要

### 维度一：基础属性

| 项目     | P0值                             |
| -------- | -------------------------------- |
| 基础攻击 | 3.2F（极低，进化链起点）         |
| 能量上限 | 300（最低，示为初始状态）        |
| 能量倍率 | ×1（基础收集率）                 |
| 模型     | `overcold_0.obj`（初始冰霜形态） |

### 维度二：SA技能

| 项目         | P0值                  |
| ------------ | --------------------- |
| SA消耗       | 8点                   |
| 首次风暴时长 | 6秒                   |
| 增幅器等级   | 0级                   |
| 每次叠加延长 | 1.5秒                 |
| **能量消耗** | **免费（0点/2tick）** |

### 维度三：动作连段

- SA释放触发连段: `FREEZE_ZERO → FREEZE_ZERO_0 → FREEZE_ZERO_END`
- 连段帧范围: 1923-1963（40帧总长）
- 施法速度较快，但相比JUST变体（0.75倍速）普通版更快

### 维度四：连段衍生效果

- SA释放时调用 `FreezeZero.FreezeZero()` 启动风暴
- 风暴持续期间，每2tick自动生成幻影剑攻击
- 幻影剑伤害：3.0基础伤害，受风暴增幅器影响

### 维度五：特殊效果

| 项目         | P0表现                                 |
| ------------ | -------------------------------------- |
| EvolutionIce | 收集能量倍率×1，目标300能量进化P1      |
| ColdLeak     | 命中施加6秒FROST_BITE(0级)，无实质减速 |
| FROST_STORM  | 半径4格，不消耗能量，1把幻影剑/2tick   |

---

## 五、进化到下一阶段的条件

### P0 → P1 进化路线

```
1. 获取灵魂点数(击杀怪物/破坏物体)
2. 每次获得灵魂时，EvolutionIce将相同数值的能量存入特殊能量槽
3. 等待特殊能量累计达到 300/300
4. 触发自动进化:
   ✓ 模型 → overcold_1.obj
   ✓ 基础攻击 → 4.0F
   ✓ 能量名称 → Evolution_1
   ✓ 最大能量 → 3000
   ✓ specialType → OverCold_1
```

### 进化前后的数值变化

| 属性         | P0 (进化前) | P1 (进化后) | 提升幅度 |
| ------------ | ----------- | ----------- | -------- |
| 基础攻击     | 3.2F        | 4.0F        | **+25%** |
| 能量上限     | 300         | 3000        | **×10**  |
| 进化等级     | 0           | 1           | -        |
| 风暴基础时长 | 6秒         | 9秒         | **+50%** |
| 叠加延长     | 1.5秒       | 3秒         | **×2**   |
| 能量消耗     | 0/2tick     | 有消耗      | 质变     |

---

## 六、该阶段在进化链中的定位

### 🧊 冰霜伊始 - 启蒙阶段

**P0是永冻进化的起点**，也是整个OverCold系统中最独特的阶段：

1. **入门门槛低**: 基础攻击3.2F适中，300能量上限容易达成，适合新手熟悉冰霜机制
2. **免费风暴**: P0阶段的风暴不消耗能量，是最廉价的控场手段，可以在初期无成本刷怪
3. **功能完整**: 即使是最低阶段，SA（FREEZE_ZERO）、SE（EvolutionIce + ColdLeak）、幻影剑系统都已完整可用
4. **战术价值**: 虽然数值最低，但免费风暴的机制让P0在特定场景下（如大量低血量怪物）反而有独特优势
5. **快速过渡**: 300能量上限意味着不需要很长时间就能进化到P1

**核心玩法建议（P0阶段）**:

- 频繁使用SA叠加风暴层数，由于免费消耗可以持续维持
- 利用风暴幻影剑快速击杀怪物积累灵魂点数
- 尽快累计300能量进入P1阶段获取更强力属性

---

## 七、核心玩法流派前瞻

### ❄️ 进化成长流

从P0起步，以最快速度累计300能量→P1，为后续强大进化铺路。

### ❄️ 冰霜控场流

P0的免费风暴是低成本控场利器，即使能量未满也能通过频繁SA维持风暴区域。

### ❄️ 寒冰法师流

利用SA叠加机制，在P0阶段就开始练习风暴叠加技巧，为后续高等级风暴做准备。
