# OverCold - 永冻领域 (P2阶段)

> **永冻纪元 四阶段进化 · 第三阶段**
> 对应注册键: `over_cold_2`
> 能量名称: `Evolution_2` | 特殊类型: `OverCold_2`

---

## 一、基础属性提取

### 1.1 核心数值

| 属性             | 数值                  | 说明                             |
| ---------------- | --------------------- | -------------------------------- |
| **特效色**       | `0x6699FF`            | 冰蓝色调，贯穿全进化阶段不变     |
| **模型**         | `overcold_2.obj`      | 永冻领域外观，展现强大的寒冰之力 |
| **纹理**         | `models/overcold.png` | 全阶段共用纹理                   |
| **基础攻击修正** | **7.2F**              | 相比P1提升80%！                  |
| **maxDamage**    | 144                   | 全阶段统一耐久上限               |
| **能量名称**     | `Evolution_2`         | 标识当前为进化第2阶段            |
| **最大特殊能量** | **30000**             | 相比P1提升10倍                   |
| **specialType**  | `OverCold_2`          | 用于运行时识别阶段               |

### 1.2 注册参数对照

参见 [`FantasySlashBladeBuiltInRegistry.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:293)

```java
// P2 注册 (L293-323)
bootstrap.register(OverColdP2, new FantasySlashBladeDefinition(
    FantasyDesire.prefix("over_cold"),
    RenderDefinition.Builder.newInstance()
        .effectColor(0x6699FF)
        .textureName(FantasyDesire.prefix("models/overcold.png"))
        .modelName(FantasyDesire.prefix("models/overcold_2.obj"))
        .standbyRenderType(CarryType.RNINJA)
        .build(),
    PropertiesDefinition.Builder.newInstance()
        .baseAttackModifier(7.2F)
        .defaultSwordType(List.of(SwordType.BEWITCHED))
        .maxDamage(144)
        .addSpecialEffect(FDSpecialEffectsRegistry.EvolutionIce.getId())
        .addSpecialEffect(FDSpecialEffectsRegistry.ColdLeak.getId())
        .slashArtsType(FDSlashArtRegistry.FREEZE_ZERO.getId())
        .build(),
    FantasyDefinition.Builder.newInstance()
        .specialChargeName("Evolution_2")
        .maxSpecialCharge(30000)
        .specialType("OverCold_2")
        .build(),
    List.of(new EnchantmentDefinition(getEnchantmentID(Enchantments.FROST_WALKER), 5),
            new EnchantmentDefinition(getEnchantmentID(Enchantments.FIRE_PROTECTION), 3),
            new EnchantmentDefinition(getEnchantmentID(Enchantments.UNBREAKING), 3))
));
```

### 1.3 进化链中的数值对比

| 属性         | P0   | P1   | **P2**    | 相对于P1的变化    |
| ------------ | ---- | ---- | --------- | ----------------- |
| 基础攻击     | 3.2F | 4.0F | **7.2F**  | **+80%**          |
| 能量上限     | 300  | 3000 | **30000** | **×10**           |
| 能量收集倍率 | ×1   | ×1   | **×3**    | **×3 加速进化！** |
| 进化等级     | 0    | 1    | **2**     | 进入高速进化阶段  |

---

## 二、SA技能分析 - FreezeZero（冰霜风暴）

### 2.1 P2在SA中的表现

参见 [`FreezeZero.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/FreezeZero.java:24)

```java
int evolutionTier = OverColdEffects.getEvolutionTier(specialType); // P2 → 2
```

### 2.2 P2阶段的StartStorm()

```java
public static void StartStorm(LivingEntity entity, int evolutionTier) {
    entity.addEffect(
        new MobEffectInstance(FDPotionEffects.FROST_STORM.get(),
            20 * (6 + evolutionTier * 3),  // P2: 20 * (6 + 2*3) = 240 ticks = 12秒
            evolutionTier));                // P2: 增幅器 = 2
}
```

| 属性                 | P2值                  | 与P1对比          |
| -------------------- | --------------------- | ----------------- |
| **首次风暴持续时间** | 240 ticks（**12秒**） | P1: 9秒，**+33%** |
| **风暴增幅器**       | **2级**               | P1: 1级，效果翻倍 |

### 2.3 P2阶段的StackStorm()

```java
int durationExtension = Math.max(20 * evolutionTier * 3, 30);
// P2: Math.max(20 * 2 * 3, 30) = 120 ticks = 6秒
// P1: 60 ticks (3秒)，P2是P1的2倍
```

| 属性             | P2值                 | 与P1对比        |
| ---------------- | -------------------- | --------------- |
| **每次叠加延长** | **120 ticks（6秒）** | P1: 3秒，**×2** |
| **叠加后增幅器** | +1，上限14           | 同P1            |

### 2.4 P2风暴能量消耗分析

参见 [`FrostStormEffect.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/potioneffect/FrostStormEffect.java:68)

```java
// P2风暴中 (evolutionTier=2)
if (!CapabilityUtils.tryConsumeSpecialCharge(ctx.fantasyState,
    Math.max(amplifier - evolutionTier, 0), entity, null)) {
    // P2: Math.max(amplifier - 2, 0)
    // 当amplifier<=2时: 0 (免费!)
    // 当amplifier>=3时: amplifier-2 (消耗)
}
```

**P2阶段的能量消耗分析**:

| 风暴层数  | 每2tick消耗 | 说明     |
| --------- | ----------- | -------- |
| 1-2级风暴 | **0**       | 免费！   |
| 3级风暴   | **1**       | 开始消耗 |
| 4级风暴   | **2**       | -        |
| ...       | ...         | ...      |
| 14级风暴  | **12**      | 最高能耗 |

> **P2的关键优势**: 由于 `evolutionTier = 2`，前**2级风暴都是免费的**！这意味着P2玩家可以维持2级风暴而不消耗任何能量。同时，×3的能量收集倍率让能量回复极快，即使消耗也能迅速补充。

---

## 三、特殊效果机制分析 - P2阶段

### 3.1 EvolutionIce（冰霜进化）- 关键转折点

参见 [`OverColdEffects.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/overcold/OverColdEffects.java:35)

```java
int evolutionTier = getEvolutionTier(fdState.getSpecialType()); // P2 → 2
int finalMultiple = evolutionTier > 1 ? 3 : 1;  // P2: 2 > 1, 倍率 = 3！
```

**P2的最关键机制：×3能量收集倍率！**

这意味着在P2阶段，每获得1点灵魂点数，EvolutionIce会转化为**3点**特殊能量存入能量槽。这使得P2→P3的进化虽然需要30000能量，但实际获取速度远快于P1→P2。

**P2进化状态**:

| 项目         | P2值             | 说明             |
| ------------ | ---------------- | ---------------- |
| 当前进化等级 | 2                | **高等级进化**   |
| 能量收集倍率 | **×3**           | **大幅加速！**   |
| 当前能量上限 | 30000            | 需要填满以进化P3 |
| 进化目标     | P3（OverCold_3） | 最终阶段         |

**P2 → P3 进化触发**:

```java
case 2: // P2 → P3
    state.setModel(new ResourceLocation(FantasyDesire.MODID, "models/overcold_3.obj"));
    state.setBaseAttackModifier(13.0f);        // 7.2 → 13.0 (+80%)
    fdState.setSpecialChargeName("Evolution_3");
    fdState.setMaxSpecialCharge(Integer.MAX_VALUE);  // 30000 → Integer.MAX_VALUE
    fdState.setSpecialType("OverCold_3");
    break;
```

### 3.2 ColdLeak（寒冰泄露）

参见 [`OverColdEffects.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/overcold/OverColdEffects.java:85)

```java
// P2命中时
target.addEffect(new MobEffectInstance(FDPotionEffects.FROST_BITE.get(), 120, evolutionTier));
// P2: 120 ticks (6秒), 增幅器2
```

**P2阶段的FROST_BITE效果**:

```java
// P2: amplifier = 2
entity.setDeltaMovement(0, -0.02 * 2, 0);  // 每秒拉动0.04格向下
// 减速:
Math.max(-0.2 * 2, -1.0) = -0.4  // 移动速度 -40%！
```

| 属性     | P1         | P2             | 变化       |
| -------- | ---------- | -------------- | ---------- |
| 持续时间 | 6秒        | **6秒**        | 不变       |
| 增幅器   | 1          | **2**          | 翻倍       |
| 减速效果 | -20%       | **-40%**       | **翻倍！** |
| 下拉力   | -0.02/tick | **-0.04/tick** | **翻倍！** |

> **P2的ColdLeak质变**: 40%的减速效果和双倍的下拉力，使FROST_BITE在P2阶段成为强力的控制手段。被击中的目标几乎无法移动，并且持续被拉向地面，空中单位会被直接拽落。

### 3.3 FROST_STORM（冰霜风暴）效果 - P2视角

```java
// P2风暴参数 (amplifier ≥ 2)
double r = Math.min(4 + 2 * 3.0, 16);  // P2: 半径 = 10格 (P1: 7格)
double yPos = entity.getY() + entity.getBbHeight() + Math.min(4 + 2, 8); // 高度偏移6

// 每2tick生成幻影剑
int swordCount = 1 + amplifier / 2;  // P2(2级): 1+1 = 2把; P2(4级): 1+2 = 3把
double damage = 3.0 + amplifier * 5.0;  // P2(2级): 13.0; P2(4级): 23.0
```

**P2风暴效果汇总**:

| 属性                  | P2(2级风暴)         | P2(高叠加风暴)       |
| --------------------- | ------------------- | -------------------- |
| **风暴半径**          | **10格**            | 随叠加增加，上限16格 |
| **每2tick能量消耗**   | **0（免费！）**     | 1+                   |
| **每2tick幻影剑数量** | **2把**             | 3+把                 |
| **幻影剑伤害**        | **13.0**            | 23.0+                |
| **FROST_BITE施加**    | 5秒/2tick, -40%减速 | 同左                 |

---

## 四、五维深度分析摘要

### 维度一：基础属性

| 项目     | P1参考       | P2值             | 变化          |
| -------- | ------------ | ---------------- | ------------- |
| 基础攻击 | 4.0F         | **7.2F**         | **+80%**      |
| 能量上限 | 3000         | **30000**        | **×10**       |
| 能量倍率 | ×1           | **×3**           | **×3 加速！** |
| 模型     | `overcold_1` | **`overcold_2`** | 永冻领域外观  |

### 维度二：SA技能

| 项目         | P1参考  | P2值        | 变化         |
| ------------ | ------- | ----------- | ------------ |
| SA消耗       | 8点     | 8点         | 不变         |
| 首次风暴时长 | 9秒     | **12秒**    | +33%         |
| 增幅器等级   | 1级     | **2级**     | 翻倍         |
| 每次叠加延长 | 3秒     | **6秒**     | ×2           |
| 免费风暴层数 | 1级免费 | **2级免费** | 更多免费额度 |

### 维度三：动作连段

- 与P0/P1相同的连段路径: `FREEZE_ZERO → FREEZE_ZERO_0 → FREEZE_ZERO_END`
- 连段结构完全一致
- **差异**: 由于P2的×3能量收集倍率，SA消耗的8点能量可以更快恢复

### 维度四：连段衍生效果

P2的风暴衍生效果进入成熟期:

- 风暴半径10格，大范围AOE
- 幻影剑伤害13.0（2级风暴），高叠加可达23.0+
- 每周期的幻影剑数量2把起

### 维度五：特殊效果

| 项目         | P1参考         | P2表现                   |
| ------------ | -------------- | ------------------------ |
| EvolutionIce | ×1倍率         | **×3倍率（加速进化！）** |
| ColdLeak     | -20%减速       | **-40%减速（强力控场）** |
| FROST_STORM  | 半径7, 伤害8.0 | **半径10, 伤害13.0+**    |

---

## 五、进化到下一阶段的条件

### P2 → P3 进化路线

```
1. 使用OverCold P2进行战斗
2. 每次获得灵魂时，EvolutionIce以×3倍率转化能量
   (这意味着实际效率相当于×3，远比P1快)
3. 等待特殊能量累计达到 30000/30000
4. 触发自动进化 - 最终阶段:
   ✓ 模型 → overcold_3.obj（终极形态）
   ✓ 基础攻击 → 13.0F（+80%！）
   ✓ 能量名称 → Evolution_3
   ✓ 最大能量 → Integer.MAX_VALUE（无限！）
   ✓ specialType → OverCold_3
```

### 进化前后数值变化

| 属性           | P2 (进化前) | P3 (进化后)           | 提升幅度   |
| -------------- | ----------- | --------------------- | ---------- |
| 基础攻击       | 7.2F        | **13.0F**             | **+80%**   |
| 能量上限       | 30000       | **Integer.MAX_VALUE** | **无限！** |
| 能量收集倍率   | ×3          | ×3（不变）            | -          |
| 进化等级       | 2           | **3（满级）**         | 最终形态   |
| 风暴基础时长   | 12秒        | **15秒**              | +25%       |
| 叠加延长       | 6秒         | **9秒**               | +50%       |
| FROST_BITE强度 | 2级         | **3级**               | 最高强度   |
| 命中回能       | 无          | **2点/次**            | 新机制解锁 |

---

## 六、该阶段在进化链中的定位

### 🧊 永冻领域 - 强势期

**P2是OverCold的黄金强势期，拥有最平衡的性能与成长性**：

1. **×3能量收集倍率**: 这是P2最核心的优势——技能消耗的能量可以以3倍效率快速补充，使得P2在能量循环上极为高效
2. **2级免费风暴**: 能维持2级风暴（半径10格/伤害13.0）而不消耗任何能量，配合×3倍率，P2在清怪效率上达到顶峰
3. **-40%强力减速**: FROST_BITE的40%减速使敌人寸步难行，P2的控场能力达到质变
4. **高性价比**: 7.2F的攻击力、免费的高等级风暴、×3的能量回复，P2是性价比最高的阶段

**核心玩法建议（P2阶段）**:

- 充分利用×3倍率频繁使用SA叠加风暴
- 维持2级免费风暴作为常驻AOE，几乎零成本
- 积极使用SA叠加风暴到更高层数以应对强敌
- 累积30000能量冲击P3终极形态

---

## 七、核心玩法流派参考

### ❄️ 进化成长流

P2的×3倍率是进化的加速器——相比P1漫长的积累期，P2以3倍效率快速向P3冲刺。

### ❄️ 冰霜控场流

P2的2级免费风暴（半径10格）配合-40%减速的FROST_BITE，形成恐怖的冰冻控制领域，敌人几乎无法逃离。

### ❄️ 寒冰法师流

P2拥有最佳的"投入产出比"——2级免费风暴层数+×3能量回复，可以频繁叠加到高等级风暴而不担心能量枯竭。
