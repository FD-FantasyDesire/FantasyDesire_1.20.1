# OverCold - 绝对零度 (P3阶段)

> **永冻纪元 四阶段进化 · 最终阶段**
> 对应注册键: `over_cold_3`
> 能量名称: `Evolution_3` | 特殊类型: `OverCold_3`

---

## 一、基础属性提取

### 1.1 核心数值

| 属性             | 数值                             | 说明                                 |
| ---------------- | -------------------------------- | ------------------------------------ |
| **特效色**       | `0x6699FF`                       | 冰蓝色调，贯穿全进化阶段不变         |
| **模型**         | `overcold_3.obj`                 | 绝对零度终极形态，寒冰之力的巅峰体现 |
| **纹理**         | `models/overcold.png`            | 全阶段共用纹理                       |
| **基础攻击修正** | **13.0F**                        | 四阶段最高，进化链终点               |
| **maxDamage**    | 144                              | 全阶段统一耐久上限                   |
| **能量名称**     | `Evolution_3`                    | 最终进化阶段标识                     |
| **最大特殊能量** | **Integer.MAX_VALUE (≈21.47亿)** | 实际上限已被移除                     |
| **specialType**  | `OverCold_3`                     | 最终阶段标识                         |

### 1.2 注册参数对照

参见 [`FantasySlashBladeBuiltInRegistry.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:324)

```java
// P3 注册 (L324-354)
bootstrap.register(OverColdP3, new FantasySlashBladeDefinition(
    FantasyDesire.prefix("over_cold"),
    RenderDefinition.Builder.newInstance()
        .effectColor(0x6699FF)
        .textureName(FantasyDesire.prefix("models/overcold.png"))
        .modelName(FantasyDesire.prefix("models/overcold_3.obj"))
        .standbyRenderType(CarryType.RNINJA)
        .build(),
    PropertiesDefinition.Builder.newInstance()
        .baseAttackModifier(13.0F)
        .defaultSwordType(List.of(SwordType.BEWITCHED))
        .maxDamage(144)
        .addSpecialEffect(FDSpecialEffectsRegistry.EvolutionIce.getId())
        .addSpecialEffect(FDSpecialEffectsRegistry.ColdLeak.getId())
        .slashArtsType(FDSlashArtRegistry.FREEZE_ZERO.getId())
        .build(),
    FantasyDefinition.Builder.newInstance()
        .specialChargeName("Evolution_3")
        .maxSpecialCharge(Integer.MAX_VALUE)
        .specialType("OverCold_3")
        .build(),
    List.of(new EnchantmentDefinition(getEnchantmentID(Enchantments.FROST_WALKER), 5),
            new EnchantmentDefinition(getEnchantmentID(Enchantments.FIRE_PROTECTION), 3),
            new EnchantmentDefinition(getEnchantmentID(Enchantments.UNBREAKING), 3))
));
```

### 1.3 完整进化链数值对比

| 属性           | P0   | P1   | P2    | **P3**        |
| -------------- | ---- | ---- | ----- | ------------- |
| 基础攻击       | 3.2F | 4.0F | 7.2F  | **13.0F**     |
| 能量上限       | 300  | 3000 | 30000 | **∞(无上限)** |
| 能量倍率       | ×1   | ×1   | ×3    | **×3**        |
| 进化等级       | 0    | 1    | 2     | **3 (满级)**  |
| 相对于P0总增幅 | -    | +25% | +125% | **+306%**     |

---

## 二、SA技能分析 - FreezeZero（冰霜风暴）

### 2.1 P3在SA中的表现

参见 [`FreezeZero.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/FreezeZero.java:24)

```java
int evolutionTier = OverColdEffects.getEvolutionTier(specialType); // P3 → 3
```

### 2.2 P3阶段的StartStorm()

```java
public static void StartStorm(LivingEntity entity, int evolutionTier) {
    entity.addEffect(
        new MobEffectInstance(FDPotionEffects.FROST_STORM.get(),
            20 * (6 + evolutionTier * 3),  // P3: 20 * (6 + 3*3) = 300 ticks = 15秒
            evolutionTier));                // P3: 增幅器 = 3
}
```

| 属性                 | P3值                  | 与P2对比           |
| -------------------- | --------------------- | ------------------ |
| **首次风暴持续时间** | 300 ticks（**15秒**） | P2: 12秒，**+25%** |
| **风暴增幅器**       | **3级**               | P2: 2级，增强50%   |

### 2.3 P3阶段的StackStorm()

```java
int durationExtension = Math.max(20 * evolutionTier * 3, 30);
// P3: Math.max(20 * 3 * 3, 30) = 180 ticks = 9秒
// P2: 120 ticks (6秒)，P3是P2的1.5倍
```

| 属性             | P3值                 | 与P2对比          |
| ---------------- | -------------------- | ----------------- |
| **每次叠加延长** | **180 ticks（9秒）** | P2: 6秒，**+50%** |
| **叠加后增幅器** | +1，上限14           | 同P2              |

### 2.4 P3风暴能量消耗分析 - 无限续航

参见 [`FrostStormEffect.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/potioneffect/FrostStormEffect.java:68)

```java
// P3风暴中 (evolutionTier=3)
if (!CapabilityUtils.tryConsumeSpecialCharge(ctx.fantasyState,
    Math.max(amplifier - evolutionTier, 0), entity, null)) {
    // P3: Math.max(amplifier - 3, 0)
    // 当amplifier<=3时: 0 (免费!)
    // 当amplifier>=4时: amplifier-3 (消耗)
}
```

**P3阶段的能量消耗分析**:

| 风暴层数  | 每2tick消耗 | 说明             |
| --------- | ----------- | ---------------- |
| 1-3级风暴 | **0**       | 免费！全部免费！ |
| 4级风暴   | **1**       | 开始消耗         |
| 5级风暴   | **2**       | -                |
| ...       | ...         | ...              |
| 14级风暴  | **11**      | 最高能耗         |

> **P3的无限能源**: 由于 `Integer.MAX_VALUE` 的能量上限和 `evolutionTier = 3` 的免费额度，P3在以下方面具有无与伦比的优势：
>
> 1. **前3级风暴完全免费**——可以维持3级风暴（半径13格/高伤害）零成本
> 2. **能量槽实际上限**: `Integer.MAX_VALUE = 2,147,483,647` ≈ 21.47亿，基本上永不枯竭
> 3. **命中回能**: P3命中时额外获得2点能量（参见ColdLeak）
> 4. **×3收集倍率**: 击杀怪物以3倍效率补充能量

### 2.5 Storm持续时间叠加对比（全阶段）

| 叠加次数            | P0       | P1       | P2       | **P3**   |
| ------------------- | -------- | -------- | -------- | -------- |
| 首次(StartStorm)    | 6秒      | 9秒      | 12秒     | **15秒** |
| +1次叠加            | +1.5秒   | +3秒     | +6秒     | **+9秒** |
| +2次叠加            | +1.5秒   | +3秒     | +6秒     | **+9秒** |
| +3次叠加            | +1.5秒   | +3秒     | +6秒     | **+9秒** |
| ...                 | ...      | ...      | ...      | ...      |
| **5次叠加后总时长** | **12秒** | **21秒** | **36秒** | **51秒** |

> **P3的持续作战能力**: 经过5次叠加后，风暴总时长可达51秒以上，配合9秒/次的延长效率，P3可以维持近乎常驻的超高等级风暴。

---

## 三、特殊效果机制分析 - P3阶段

### 3.1 EvolutionIce（冰霜进化）- 满级状态

参见 [`OverColdEffects.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/overcold/OverColdEffects.java:40)

```java
int evolutionTier = getEvolutionTier(fdState.getSpecialType()); // P3 → 3
int finalMultiple = evolutionTier > 1 ? 3 : 1;  // P3: 倍率 = 3

// 满值检测 (P3不再进化)
if (fdState.getSpecialCharge() >= fdState.getMaxSpecialCharge() && evolutionTier != 3) {
    // P3: evolutionTier == 3, 此分支永不执行
}
```

**P3进化状态**:

| 项目         | P3值                  | 说明                          |
| ------------ | --------------------- | ----------------------------- |
| 当前进化等级 | **3 (满级)**          | 不再有进一步进化              |
| 能量收集倍率 | **×3**                | 满级仍享受高速回复            |
| 当前能量上限 | **Integer.MAX_VALUE** | 永无止境                      |
| 进化检测     | **已禁用**            | `evolutionTier != 3` 阻止检测 |

> **P3的终极特性**: 满级后进化触发器被禁用，但×3倍率和无限能量上限依然有效。能量槽在P3阶段不再有"填满"的概念——它只是不断增长的数值。

### 3.2 ColdLeak（寒冰泄露）- 满级强化

参见 [`OverColdEffects.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/overcold/OverColdEffects.java:81)

```java
// P3独有机制：命中回能
if (evolutionTier == 3) {
    CapabilityUtils.addSpecialCharge(fdState, 2);  // 每次命中额外获得2点能量
}
// 施加FROST_BITE
target.addEffect(new MobEffectInstance(FDPotionEffects.FROST_BITE.get(), 120, evolutionTier));
// P3: 120 ticks (6秒), 增幅器3
```

**P3独有的命中回能机制**:

| 机制               | 效果             | 价值                 |
| ------------------ | ---------------- | -------------------- |
| **命中回能**       | 每次命中+2点能量 | 攻击即充电，无限续航 |
| **FROST_BITE强度** | 3级              | 最高控场等级         |

**P3阶段的FROST_BITE效果**:

```java
// P3: amplifier = 3
entity.setDeltaMovement(0, -0.02 * 3, 0);  // 每秒拉动0.06格向下
// 减速:
Math.max(-0.2 * 3, -1.0) = -0.6  // 移动速度 -60%！
```

| 属性     | P2         | **P3**         | 变化             |
| -------- | ---------- | -------------- | ---------------- |
| 持续时间 | 6秒        | **6秒**        | 不变             |
| 增幅器   | 2          | **3**          | +50%             |
| 减速效果 | -40%       | **-60%**       | **几乎瘫痪敌人** |
| 下拉力   | -0.04/tick | **-0.06/tick** | 飞天敌人瞬间坠地 |
| 命中回能 | 无         | **+2点/次**    | **P3独有！**     |

> **P3的终极控场**: 60%的减速使敌人几乎无法移动，配合3级风暴的13格半径范围，P3可以在大范围内造成"绝对零度"级别的控制效果——敌人被减速60%且持续下拉，同时承受风暴幻影剑的持续打击。

### 3.3 FROST_STORM（冰霜风暴）效果 - P3满级状态

```java
// P3风暴参数 (amplifier ≥ 3)
double r = Math.min(4 + 3 * 3.0, 16);  // P3: 半径 = 13格，上限16格
double yPos = entity.getY() + entity.getBbHeight() + Math.min(4 + 3, 8); // 高度偏移7

// 每2tick生成幻影剑
int swordCount = 1 + amplifier / 2;  // P3(3级): 1+1 = 2把; P3(6级): 1+3 = 4把
double damage = 3.0 + amplifier * 5.0;  // P3(3级): 18.0; P3(6级): 33.0
```

**P3风暴效果汇总**:

| 属性                  | P3(3级风暴-免费)    | P3(14级风暴-满叠加) |
| --------------------- | ------------------- | ------------------- |
| **风暴半径**          | **13格**            | **16格（上限）**    |
| **每2tick能量消耗**   | **0（免费！）**     | 11                  |
| **每2tick幻影剑数量** | **2把**             | **8把**             |
| **幻影剑伤害**        | **18.0**            | **73.0**            |
| **FROST_BITE施加**    | 5秒/2tick, -60%减速 | 同左                |
| **能量消耗/秒**       | 0                   | ~110点/秒           |

### 3.4 全阶段风暴参数完整对比

| 属性           | P0   | P1   | P2   | **P3**   |
| -------------- | ---- | ---- | ---- | -------- |
| 首次风暴半径   | 4格  | 7格  | 10格 | **13格** |
| 最大半径       | 16格 | 16格 | 16格 | **16格** |
| 首次伤害       | 3.0  | 8.0  | 13.0 | **18.0** |
| 最大伤害(14级) | 73.0 | 73.0 | 73.0 | **73.0** |
| 免费风暴层数   | 0级  | 1级  | 2级  | **3级**  |
| 命中回能       | 无   | 无   | 无   | **+2点** |
| 能量倍率       | ×1   | ×1   | ×3   | **×3**   |

---

## 四、五维深度分析摘要

### 维度一：基础属性

| 项目     | P2参考       | **P3值**                     | 变化                |
| -------- | ------------ | ---------------------------- | ------------------- |
| 基础攻击 | 7.2F         | **13.0F**                    | **+80%（总+306%）** |
| 能量上限 | 30000        | **∞ (Integer.MAX_VALUE)**    | **无限！**          |
| 能量倍率 | ×3           | **×3**                       | 不变但收集无上限    |
| 模型     | `overcold_2` | **`overcold_3`（最终形态）** | 终极外观            |

### 维度二：SA技能

| 项目         | P2参考  | **P3值**    | 变化         |
| ------------ | ------- | ----------- | ------------ |
| SA消耗       | 8点     | 8点         | 不变         |
| 首次风暴时长 | 12秒    | **15秒**    | +25%         |
| 增幅器等级   | 2级     | **3级**     | +50%         |
| 每次叠加延长 | 6秒     | **9秒**     | +50%         |
| 免费风暴层数 | 2级免费 | **3级免费** | 免费额度最高 |

### 维度三：动作连段

- 与之前阶段相同的连段路径: `FREEZE_ZERO → FREEZE_ZERO_0 → FREEZE_ZERO_END`
- **SA消耗无意义**: P3的无限能量上限和×3倍率意味着8点SA消耗几乎可以忽略不计
- JUST变体 (0.75倍速) 在P3的强力风暴下更具性价比——持续时间更长意味着AOE输出更高

### 维度四：连段衍生效果

P3的风暴衍生效果达到巅峰:

- 首次风暴即达13格半径、18.0伤害
- 前3级风暴完全免费，可常驻
- 命中回能机制保证风暴可持续性
- 最高可达8把幻影剑/2tick（14级）、73.0伤害

### 维度五：特殊效果

| 项目         | P2参考            | **P3表现**                     |
| ------------ | ----------------- | ------------------------------ |
| EvolutionIce | ×3倍率, 上限30000 | **×3倍率, 无限上限, 进化禁用** |
| ColdLeak     | 命中-40%减速      | **命中-60%减速 +2点回能**      |
| FROST_STORM  | 半径10, 伤害13.0  | **半径13, 伤害18.0+**          |

---

## 五、该阶段在进化链中的定位

### 🧊 绝对零度 - 终极形态

**P3是OverCold的完全体，寒冰之力的终极体现**：

1. **无上限能量**: `Integer.MAX_VALUE` 意味着能量系统在P3阶段变成了纯粹的战斗力指标——不再有"存满进化"的概念，能量只需用于消耗
2. **三重免费风暴**: 3级风暴完全免费（半径13格/伤害18.0），相当于常驻大范围AOE。这是P3最具战术价值的特性
3. **命中回能循环**: 每次命中+2点能量 + ×3收集倍率 = 能量永不枯竭。即使使用高等级风暴（14级消耗11点/2tick），通过攻击也能快速补充
4. **-60%终极减速**: FROST_BITE的60%减速和下拉使敌人完全失去行动能力，配合风暴形成真正的"绝对零度领域"
5. **13.0F超高攻击**: 四阶段累计涨幅超过300%，P3的平A就已经具备极高的爆发力

**核心玩法建议（P3阶段）**:

- 常驻3级免费风暴作为被动AOE
- 面对强敌时通过多次叠加将风暴提升至14级满级（73.0伤害/8把剑/2tick）
- 利用命中回能机制，在战斗中可以维持高等级风暴而不用担心能量消耗
- 充分发挥60%减速控制，结合风暴伤害实现"冻住-秒杀"循环

---

## 六、核心玩法流派总结

### ❄️ 进化成长流 - 完整进化路线

**从P0到P3的成长路线规划**:

```
P0 (冰霜伊始) ─── 积累300能量 ──→ P1 (寒冰觉醒)
  │                               │
  │ 免费风暴刷怪                     │ 积累3000能量(×1倍率)
  │ 快速过渡                         │ 熟悉冰霜控制
  ▼                               ▼
P1 (寒冰觉醒) ─── 积累3000能量 ──→ P2 (永冻领域)
  │                               │
  │ -20%减速控场                     │ ×3倍率加速进化
  │ 首次免费风暴                      │ 2级免费风暴刷怪
  ▼                               ▼
P2 (永冻领域) ─── 积累30000能量 ──→ P3 (绝对零度)
  │                               │
  │ ×3倍率高速积累                    │ 无限能量 - 终极形态
  │ 最佳性价比阶段                     │ -60%减速 + 命中回能
  ▼                               ▼
                          ★ 进化完成 ★
```

**各阶段核心成长任务**:

| 阶段 | 主要任务                                        | 所需时间估计     |
| ---- | ----------------------------------------------- | ---------------- |
| P0   | 快速积累300能量，熟悉SA操作                     | 短（几分钟）     |
| P1   | 积累3000能量（×1倍率），掌握控场                | 中等（十多分钟） |
| P2   | 积累30000能量（×3倍率=实际约10000），享受强势期 | 较短（数分钟）   |
| P3   | 终极形态，无需积累                              | 无需             |

### ❄️ 冰霜控场流 - FROST_STORM + FROST_BITE 冰冻连锁

**核心机制**: 通过ColdLeak命中施加FROST_BITE + FROST_STORM范围持续施加FROST_BITE，形成双重控制。

**控制链构成**:

```
ColdLeak(命中) ─→ FROST_BITE(-60%减速, 下拉) ─→ 敌人无法移动
                                                        │
FROST_STORM(每2tick) ─→ FROST_BITE刷新(5秒) ────────────┘
                                                        │
                              ┌─────────────────────────┘
                              ▼
              敌人被永久冻结在风暴范围内
                              │
              幻影剑持续攻击(最多8把/2tick)
                              │
              伤害18.0~73.0/把，持续输出
```

**阶段完整控制成长**:

| 阶段 | 减速效果                | 风暴半径 | 控制强度   |
| ---- | ----------------------- | -------- | ---------- |
| P0   | **无**（0级FROST_BITE） | 4格      | 几乎无控制 |
| P1   | **-20%**                | 7格      | 入门控制   |
| P2   | **-40%**                | 10格     | 强力控制   |
| P3   | **-60%**                | 13格     | 绝对控制   |

### ❄️ 寒冰法师流 - SA叠加风暴层数

**核心机制**: 利用SA（FREEZE_ZERO）叠加FROST_STORM层数，通过风暴增幅器等级增强幻影剑伤害和数量。

**叠加公式回顾**:

```
StartStorm(entity, evolutionTier):
  持续时间 = 20 * (6 + evolutionTier * 3) ticks
  初始增幅器 = evolutionTier

StackStorm(entity, evolutionTier, current):
  延长 = max(20 * evolutionTier * 3, 30) ticks
  新增幅器 = min(current.amplifier + 1, 14)
```

**全阶段叠层效率对比**:

| 阶段   | 首次时长 | 每次延长 | 叠到14级需SA次数 | 14级总时长          |
| ------ | -------- | -------- | ---------------- | ------------------- |
| P0     | 6秒      | +1.5秒   | 14次             | 6+1.5×13=**25.5秒** |
| P1     | 9秒      | +3秒     | 13次             | 9+3×12=**45秒**     |
| P2     | 12秒     | +6秒     | 12次             | 12+6×11=**78秒**    |
| **P3** | **15秒** | **+9秒** | **11次**         | **15+9×10=105秒**   |

**P3满级风暴的终极数据**:

- **14级增幅器** × **8把幻影剑/2tick** × **73.0伤害/把** = **584.0伤害/2tick** = **14,600 DPS**
- 配合-60%减速FROST_BITE，敌人被困在风暴中持续承受恐怖伤害

**经济性分析**:

| 阶段   | 每次SA消耗 | 能量倍率          | 净消耗/次SA    |
| ------ | ---------- | ----------------- | -------------- |
| P0     | 8点        | ×1                | 8点            |
| P1     | 8点        | ×1                | 8点            |
| P2     | 8点        | ×3                | 净赚16点       |
| **P3** | **8点**    | **×3 + 命中回能** | **持续净增长** |

> **P3才是真正的"寒冰法师"**: 在P3阶段，SA的消耗(8点)被×3倍率(击杀回复更多)和命中回能(+2点/次)完全覆盖。每次SA不仅不消耗能量，反而还能产生正能量循环——这就是"绝对零度"真正的恐怖之处：**无限叠加、无限输出、无限控制**。

---

## 七、总评

OverCold（永冻纪元）是一把以**冰霜进化**为核心机制的拔刀剑，四阶段分别代表了寒冰之力的四个觉醒层次：

| 阶段  | 主题     | 核心特点               | 战力指数 |
| ----- | -------- | ---------------------- | -------- |
| P0 🧊 | 冰霜伊始 | 免费低效风暴，快速过渡 | ★★☆☆☆    |
| P1 🧊 | 寒冰觉醒 | 首次控场能力，入门控制 | ★★★☆☆    |
| P2 🧊 | 永冻领域 | ×3加速，黄金强势期     | ★★★★☆    |
| P3 🧊 | 绝对零度 | 无限能量，终极控场输出 | ★★★★★    |

**设计亮点**:

1. **进化机制**: 通过灵魂点数→特殊能量的转化驱动进化，使玩家在正常游戏过程中自然成长
2. **梯度提升**: 每阶段×10的能量上限增长和+25%~80%的攻击力提升，形成清晰的成长曲线
3. **免费风暴层数**: 根据进化等级提供免费风暴层数（0/1/2/3级），鼓励高阶段玩家更频繁地使用技能
4. **×3倍率门槛**: 在P2才解锁×3倍率，让玩家在P1阶段充分体验基础机制后再加速成长
5. **命中回能**: P3独有的命中回能机制，象征"绝对零度"已无需外力补充
