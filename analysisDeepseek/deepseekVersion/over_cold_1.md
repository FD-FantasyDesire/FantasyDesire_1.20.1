# OverCold - 寒冰觉醒 (P1阶段)

> **永冻纪元 四阶段进化 · 第二阶段**
> 对应注册键: `over_cold_1`
> 能量名称: `Evolution_1` | 特殊类型: `OverCold_1`

---

## 一、基础属性提取

### 1.1 核心数值

| 属性             | 数值                  | 说明                             |
| ---------------- | --------------------- | -------------------------------- |
| **特效色**       | `0x6699FF`            | 冰蓝色调，贯穿全进化阶段不变     |
| **模型**         | `overcold_1.obj`      | 进化的冰霜外观，比P0更具寒冰质感 |
| **纹理**         | `models/overcold.png` | 全阶段共用纹理                   |
| **基础攻击修正** | **4.0F**              | 相比P0提升25%                    |
| **maxDamage**    | 144                   | 全阶段统一耐久上限               |
| **能量名称**     | `Evolution_1`         | 标识当前为进化第1阶段            |
| **最大特殊能量** | **3000**              | 相比P0提升10倍                   |
| **specialType**  | `OverCold_1`          | 用于运行时识别阶段               |

### 1.2 注册参数对照

参见 [`FantasySlashBladeBuiltInRegistry.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:262)

```java
// P1 注册 (L262-292)
bootstrap.register(OverColdP1, new FantasySlashBladeDefinition(
    FantasyDesire.prefix("over_cold"),
    RenderDefinition.Builder.newInstance()
        .effectColor(0x6699FF)
        .textureName(FantasyDesire.prefix("models/overcold.png"))
        .modelName(FantasyDesire.prefix("models/overcold_1.obj"))
        .standbyRenderType(CarryType.RNINJA)
        .build(),
    PropertiesDefinition.Builder.newInstance()
        .baseAttackModifier(4.0F)
        .defaultSwordType(List.of(SwordType.BEWITCHED))
        .maxDamage(144)
        .addSpecialEffect(FDSpecialEffectsRegistry.EvolutionIce.getId())
        .addSpecialEffect(FDSpecialEffectsRegistry.ColdLeak.getId())
        .slashArtsType(FDSlashArtRegistry.FREEZE_ZERO.getId())
        .build(),
    FantasyDefinition.Builder.newInstance()
        .specialChargeName("Evolution_1")
        .maxSpecialCharge(3000)
        .specialType("OverCold_1")
        .build(),
    List.of(new EnchantmentDefinition(getEnchantmentID(Enchantments.FROST_WALKER), 5),
            new EnchantmentDefinition(getEnchantmentID(Enchantments.FIRE_PROTECTION), 3),
            new EnchantmentDefinition(getEnchantmentID(Enchantments.UNBREAKING), 3))
));
```

### 1.3 与P0的核心变化对比

| 属性     | P0           | P1               | 变化幅度 | 意义                       |
| -------- | ------------ | ---------------- | -------- | -------------------------- |
| 基础攻击 | 3.2F         | **4.0F**         | **+25%** | 首次攻击力跃升             |
| 能量上限 | 300          | **3000**         | **×10**  | 需要更长时间积累来达成进化 |
| 进化等级 | 0            | **1**            | -        | 正式进入寒冰觉醒阶段       |
| 模型     | `overcold_0` | **`overcold_1`** | 外观进化 | 视觉上体现寒冰之力觉醒     |

---

## 二、SA技能分析 - FreezeZero（冰霜风暴）

### 2.1 P1在SA中的表现

参见 [`FreezeZero.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/FreezeZero.java:17)

```java
int evolutionTier = OverColdEffects.getEvolutionTier(specialType); // P1 → 1
```

### 2.2 P1阶段的StartStorm()

```java
public static void StartStorm(LivingEntity entity, int evolutionTier) {
    entity.addEffect(
        new MobEffectInstance(FDPotionEffects.FROST_STORM.get(),
            20 * (6 + evolutionTier * 3),  // P1: 20 * (6 + 1*3) = 180 ticks = 9秒
            evolutionTier));                // P1: 增幅器 = 1
}
```

| 属性                 | P1值                 | 与P0对比                |
| -------------------- | -------------------- | ----------------------- |
| **首次风暴持续时间** | 180 ticks（**9秒**） | P0: 6秒，**+50%**       |
| **风暴增幅器**       | **1级**              | P0: 0级，开始有实际效果 |

### 2.3 P1阶段的StackStorm()

```java
int durationExtension = Math.max(20 * evolutionTier * 3, 30);
// P1: Math.max(20 * 1 * 3, 30) = 60 ticks = 3秒
// P0: 30 ticks (1.5秒)，P1是P0的2倍
```

| 属性             | P1值                | 与P0对比          |
| ---------------- | ------------------- | ----------------- |
| **每次叠加延长** | **60 ticks（3秒）** | P0: 1.5秒，**×2** |
| **叠加后增幅器** | +1，上限14          | 同P0              |

### 2.4 P1风暴能量消耗分析

参见 [`FrostStormEffect.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/potioneffect/FrostStormEffect.java:68)

```java
// P1风暴中 (evolutionTier=1, amplifier>=1)
if (!CapabilityUtils.tryConsumeSpecialCharge(ctx.fantasyState,
    Math.max(amplifier - evolutionTier, 0), entity, null)) {
    // P1: Math.max(amplifier - 1, 0)
    // 当amplifier=1(首次风暴)时: Math.max(1-1,0) = 0 (免费!)
    // 当amplifier>=2(叠加后)时: amplifier-1 (开始消耗能量)
    entity.removeEffect(this);  // 能量不足时移除风暴
    return;
}
```

**P1阶段的能量消耗分析**:

| 风暴层数           | 每2tick消耗 | 说明           |
| ------------------ | ----------- | -------------- |
| 1级风暴（首次）    | **0**       | 免费！同P0机制 |
| 2级风暴（叠加1次） | **1**       | 开始消耗能量   |
| 3级风暴（叠加2次） | **2**       | 消耗逐渐增加   |
| ...                | ...         | ...            |
| 14级风暴（满叠加） | **13**      | 最高能耗       |

> **P1的转折点**: 与P0不同，P1在风暴叠加1次后开始消耗能量。但由于风暴消耗公式为 `max(amplifier - evolutionTier, 0)`，P1的首次风暴仍然是免费的。这给了玩家一定的操作空间：可以在不消耗能量的情况下维持1级风暴。

---

## 三、特殊效果机制分析 - P1阶段

### 3.1 EvolutionIce（冰霜进化）

参见 [`OverColdEffects.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/overcold/OverColdEffects.java:40)

```java
int evolutionTier = getEvolutionTier(fdState.getSpecialType()); // P1 → 1
int finalMultiple = evolutionTier > 1 ? 3 : 1;  // P1: 1 < 2, 倍率仍为1
```

**P1进化状态**:

| 项目         | P1值             | 说明                    |
| ------------ | ---------------- | ----------------------- |
| 当前进化等级 | 1                | 已从P0进化而来          |
| 能量收集倍率 | ×1               | 未达到2级，仍为基础倍率 |
| 当前能量上限 | 3000             | 需要填满以进化P2        |
| 进化目标     | P2（OverCold_2） | 需要3000能量            |

**P1 → P2 进化触发**:

```java
// 当specialCharge >= maxSpecialCharge (3000) 时
case 1: // P1 → P2
    state.setModel(new ResourceLocation(FantasyDesire.MODID, "models/overcold_2.obj"));
    state.setBaseAttackModifier(7.2f);      // 4.0 → 7.2 (+80%)
    fdState.setSpecialChargeName("Evolution_2");
    fdState.setMaxSpecialCharge(30000);      // 3000 → 30000 (×10)
    fdState.setSpecialType("OverCold_2");
    break;
```

### 3.2 ColdLeak（寒冰泄露）

参见 [`OverColdEffects.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/overcold/OverColdEffects.java:85)

```java
// P1命中时
target.addEffect(new MobEffectInstance(FDPotionEffects.FROST_BITE.get(), 120, evolutionTier));
// P1: 120 ticks (6秒), 增幅器1
```

**P1阶段的FROST_BITE效果**:

参见 [`FrostBiteEffect.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/potioneffect/FrostBiteEffect.java:24)

```java
// P1: amplifier = 1
entity.setDeltaMovement(0, -0.02 * 1, 0);  // 每秒拉动0.02格向下
// 减速:
Math.max(-0.2 * 1, -1.0) = -0.2  // 移动速度 -20%
```

| 属性     | P0  | P1              | 变化               |
| -------- | --- | --------------- | ------------------ |
| 持续时间 | 6秒 | **6秒**         | 不变               |
| 增幅器   | 0   | **1**           | **有实际效果！**   |
| 减速效果 | 0%  | **-20%**        | 首次出现减速控制   |
| 下拉力   | 0   | **-0.02/ tick** | 开始产生下拉效果   |
| 无重力   | 否  | **是**          | P1开始禁用目标重力 |

> **P1的ColdLeak质变**: 从P1开始，FROST_BITE效果产生了实际的控制效果——20%的移动速度降低和持续下拉。这使得P1在对战中具备了初级的冰冻控场能力。

### 3.3 FROST_STORM（冰霜风暴）效果 - P1视角

```java
// P1风暴参数 (amplifier ≥ 1)
double r = Math.min(4 + 1 * 3.0, 16);  // P1: 半径 = 7格 (P0: 4格)
double yPos = entity.getY() + entity.getBbHeight() + Math.min(4 + 1, 8); // 高度偏移5

// 每2tick生成幻影剑
int swordCount = 1 + amplifier / 2;  // P1(1级): 1+0 = 1把; P1(2级): 1+1 = 2把
double damage = 3.0 + amplifier * 5.0;  // P1(1级): 8.0; P1(2级): 13.0
```

**P1风暴效果汇总**:

| 属性                  | P1(1级风暴)       | P1(高叠加风暴) |
| --------------------- | ----------------- | -------------- |
| **风暴半径**          | 7格               | 随叠加增加     |
| **每2tick能量消耗**   | 0(首次)/1+        | 高叠加消耗更大 |
| **每2tick幻影剑数量** | 1把               | 2+把           |
| **幻影剑伤害**        | 8.0               | 13.0+          |
| **FROST_BITE施加**    | 5秒/2tick         | 同左           |
| **范围粒子效果**      | 边缘云雾+内部雪花 | 随半径增加     |

---

## 四、五维深度分析摘要

### 维度一：基础属性

| 项目     | P0参考       | P1值                         |
| -------- | ------------ | ---------------------------- |
| 基础攻击 | 3.2F         | **4.0F**（+25%）             |
| 能量上限 | 300          | **3000**（×10）              |
| 能量倍率 | ×1           | ×1（未达到2级）              |
| 模型     | `overcold_0` | **`overcold_1`**（外观进化） |

### 维度二：SA技能

| 项目         | P0参考   | P1值                   |
| ------------ | -------- | ---------------------- |
| SA消耗       | 8点      | 8点（不变）            |
| 首次风暴时长 | 6秒      | **9秒**（+50%）        |
| 增幅器等级   | 0级      | **1级**                |
| 每次叠加延长 | 1.5秒    | **3秒**（×2）          |
| 能量消耗模式 | 完全免费 | **首次免费，之后消耗** |

### 维度三：动作连段

- 与P0相同的连段路径: `FREEZE_ZERO → FREEZE_ZERO_0 → FREEZE_ZERO_END`
- 连段结构、帧数、优先级与P0完全一致
- SA释放的时机和效果相同，区别在于风暴等级参数的不同

### 维度四：连段衍生效果

P1的风暴衍生效果相比P0有显著增强:

- 风暴半径更大: 4格→7格
- 幻影剑伤害更高: 3.0→8.0（1级风暴）
- FROST_BITE附加强度从0级→1级，有了实际控制效果

### 维度五：特殊效果

| 项目         | P0参考          | P1表现              |
| ------------ | --------------- | ------------------- |
| EvolutionIce | 倍率×1, 目标300 | 倍率×1, 目标3000    |
| ColdLeak     | 无实质减速      | **-20%减速 + 下拉** |
| FROST_STORM  | 半径4, 伤害3.0  | **半径7, 伤害8.0+** |

---

## 五、进化到下一阶段的条件

### P1 → P2 进化路线

```
1. 使用OverCold P1进行战斗，通过击杀怪物积累特殊能量
2. 每次获得灵魂时，EvolutionIce将相同数值的能量存入
   特殊能量槽（P1倍率仍为×1）
3. 等待特殊能量累计达到 3000/3000
4. 触发自动进化:
   ✓ 模型 → overcold_2.obj
   ✓ 基础攻击 → 7.2F（+80%！）
   ✓ 能量名称 → Evolution_2
   ✓ 最大能量 → 30000（×10）
   ✓ specialType → OverCold_2
```

### 进化前后数值变化

| 属性           | P1 (进化前) | P2 (进化后) | 提升幅度         |
| -------------- | ----------- | ----------- | ---------------- |
| 基础攻击       | 4.0F        | **7.2F**    | **+80%**         |
| 能量上限       | 3000        | **30000**   | **×10**          |
| 能量收集倍率   | ×1          | **×3**      | **×3！**         |
| 进化等级       | 1           | **2**       | 进入高速进化阶段 |
| 风暴基础时长   | 9秒         | **12秒**    | +33%             |
| 叠加延长       | 3秒         | **6秒**     | ×2               |
| FROST_BITE强度 | 1级         | **2级**     | 双倍减速         |

---

## 六、该阶段在进化链中的定位

### 🧊 寒冰觉醒 - 质变阶段

**P1是OverCold从"玩具"到"工具"的质变阶段**：

1. **控制能力觉醒**: FROST_BITE从P1开始具备实质性的20%减速和下拉效果，让OverCold在P1阶段首次具备了实战控场能力
2. **风暴强度提升**: 风暴半径从4格扩大到7格，基础伤害从3.0提升到8.0，配合首次风暴免费的机制，P1在清怪效率上有明显提升
3. **过渡期挑战**: 3000能量上限是P0的10倍，且P1的收集倍率仍为×1，这意味着P1→P2的进化过程会比P0→P1漫长得多
4. **能量管理入门**: 从P1开始，叠加风暴后需要消耗能量，玩家需要开始学习管理特殊能量

**核心玩法建议（P1阶段）**:

- 利用首次风暴免费的机制，在需要时开启1级风暴进行清怪
- 适当叠加风暴层数换取更高伤害，但注意控制能量消耗，避免影响进化进度
- 利用FROST_BITE的-20%减速控制移动型怪物
- 累积3000能量进化P2，届时将获得×3能量收集倍率的加成

---

## 七、核心玩法流派参考

### ❄️ 进化成长流

P1是进化路线中最需要耐心的阶段——3000能量×1倍率，合理分配风暴消耗以加快进化速度。

### ❄️ 冰霜控场流

P1的FROST_BITE（-20%减速+下拉）使冰冻控场成为可行战术，配合风暴减速范围形成控制区域。

### ❄️ 寒冰法师流

P1的首次风暴免费机制让"叠层流"在低消耗下运行，练习风暴叠加技巧的同时积累进化能量。
