# 白色之神 智能手枪A (SmartPistol A — CHARGE_SHOT) 深度分析报告

> 分析基于 [`FantasyDesire`](src/main/java/tennouboshiuzume/mods/FantasyDesire) 源代码，聚焦 A 形态（Charge Shot）的底层实现与玩法设计。

---

## 一、基础属性提取

注册于 [`FantasySlashBladeBuiltInRegistry.java:80`](src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:80) 的 `SmartPistolA`。

| 属性             | 值                                                    | 说明                       |
| ---------------- | ----------------------------------------------------- | -------------------------- |
| **注册名**       | `smart_pistol_a`                                      | 归属 `smart_pistol` 系列   |
| **特效颜色**     | `0x00FFFF`（青蓝）                                    | 弹幕/尾迹/粒子颜色         |
| **纹理**         | `models/smartpistol.png`                              | 白色手枪纹理               |
| **模型**         | `models/smartpistol.obj`                              | 共享模型                   |
| **携带方式**     | `CarryType.KATANA`（腰间）                            | 收刀姿态                   |
| **基础攻击修正** | `3.0F`                                                |                            |
| **最大伤害**     | `256`                                                 |                            |
| **默认剑类型**   | `SwordType.BEWITCHED`（妖刀）                         |                            |
| **出厂附魔**     | `POWER_ARROWS` I, `FALL_PROTECTION` V, `UNBREAKING` V | 力量附魔直接影响伤害计算   |
| **特殊能量**     | `Ammo`（弹药），最大 36 发，类型 `Gunblade`           | 核心资源                   |
| **SA**           | `CHARGE_SHOT`                                         | 弹药倾泻 + 转B形态         |
| **SE**           | `EnergyBullet`(60,2), `TripleBullet`(40,3)            | 两个SE共享，但触发条件不同 |

---

## 二、SA 技能分析：CHARGE_SHOT

### 2.1 SA 注册与连段入口

在 [`FDSlashArtRegistry.java:22`](src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSlashArtRegistry.java:22) 中：

```java
CHARGE_SHOT = FD_SLASH_ARTS.register("charge_shot",
    () -> new FDSlashArts((e) -> FDCombo.CHARGE_SHOT.getId(), 2));
```

- **冷却值**: `2`（相比于 OVER_CHARGE 的 `2`，两者冷却相同）
- **连段入口**: [`FDCombo.CHARGE_SHOT`](src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:684)

### 2.2 连段流转路径

```
CHARGE_SHOT (入口)
  │  AntiNTR 检测：确认主手物品翻译键含 "smart_pistol"
  │  通过 → smart_pistol_a_to_b
  │  不通过 → SlashBlade.prefix("none")
  ▼
SMART_PISTOL_A_TO_B (转换状态, priority=80)
  │  clickAction:
  │    1. SmartPistolMode.dumpAmmo(entity, state, fdState)  ← 弹药倾泻
  │    2. SmartPistolMode.TransformToB(state, fdState)       ← 转为B形态
  ▼
SlashBlade.prefix("none") ← 连段终止
```

**关键点**：A形态的 SA 执行顺序是 **先倾泻弹药，后转换形态**。这意味着当你按下 SA 键时，会先用当前形态的弹量打出一轮爆发，然后无缝切换到 B 形态继续作战。

### 2.3 dumpAmmo() 深度解析

定义于 [`SmartPistolMode.java:82`](src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/SmartPistolMode.java:82)。

#### 2.3.1 弹药裂变逻辑

```java
int ammo = fdState.getSpecialCharge();
boolean explosiveOn = /* 检测是否有 ExplosiveBullet SE */;
int volleyCount = explosiveOn ? ammo : ammo * 3;
```

| 模式                               | 每发弹药产出 | 满弹(36)总投射数 |
| ---------------------------------- | ------------ | ---------------- |
| **普通模式**（无 ExplosiveBullet） | 3 发幻影剑   | **108 发**       |
| **爆炸模式**（有 ExplosiveBullet） | 1 发精炼导弹 | **36 发**        |

#### 2.3.2 伤害计算

```java
float baseDamage = state.getBaseAttackModifier() + state.getAttackAmplifier();  // 基础 + 连段加成
int refine = state.getRefine();
float refineBonus = (float) (refine * 0.1f + Math.sqrt(refine) * 1.5f);        // 精炼加成
int enchantLevel = blade.getEnchantmentLevel(Enchantments.POWER_ARROWS);
float enchantMultiplier = 1.0f + (enchantLevel * 0.10f);                        // 附魔倍率
float finalDamage = (float) ((baseDamage + refineBonus) * enchantMultiplier * ratio);  // ratio=3.0
```

- **精炼公式**: `refine * 0.1 + sqrt(refine) * 1.5` — 前100次重铸收益高，后续线性增长
- **附魔倍率**: `1.0 + level * 0.10` — 力量 V 提供 50% 额外伤害
- **每发伤害倍率**: `ratio = 3.0`，即每发子弹造成基础伤害 × 3
- **爆炸模式** 额外 ×5 伤害倍率

#### 2.3.3 索敌与追踪

```java
float lockDistance = (explosiveOn ? 35 : 15) + sweepLevel * sweepRangeMult;
```

- 普通模式索敌范围：`15 + 扫荡等级 × 5`
- 爆炸模式索敌范围：`35 + 扫荡等级 × 15`

使用 [`FDTargetSelector.getTargetsInSight()`](src/main/java/tennouboshiuzume/mods/FantasyDesire/utils/FDTargetSelector.java) 获取视野内目标，按距离排序，通过 `斐波那契球面分布（Fibonacci Sphere）` 生成 3D 球面弹幕。

#### 2.3.4 爆炸模式的 MISSILE_LOCKED 机制

爆炸模式下，每发精炼导弹附加 [`MissileLockedEffect`](src/main/java/tennouboshiuzume/mods/FantasyDesire/potioneffect/MissileLockedEffect.java)（红色发光，持续 3 秒），层数叠加最高 18 层。此效果标记目标，引导后续导弹优先攻击未锁定目标，实现智能分配。

#### 2.3.5 弹药消耗

执行完毕后 `fdState.setSpecialCharge(0)`，弹药彻底清空。

### 2.4 TransformToA() / TransformToB()

定义于 [`SmartPistolMode.java:29-39`](src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/SmartPistolMode.java:29)：

| 方法             | 纹理                 | SA            | 特效色     |
| ---------------- | -------------------- | ------------- | ---------- |
| `TransformToA()` | `smartpistol.png`    | `CHARGE_SHOT` | `0x00FFFF` |
| `TransformToB()` | `smartpistol_oc.png` | `OVER_CHARGE` | `0x99FF00` |

---

## 三、动作连段解析

### 3.1 普通攻击连段（非SA）

A形态（CHARGE_SHOT）的普通攻击在 [`GunBladeEffects.java:40`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/gunblade/GunBladeEffects.java:40) 中被完全重写：

```
玩家左/右键点击
  │
  ├─> 按住 Shift？ → 执行原版拔刀剑近战（不触发 SE）
  │
  ├─> Ammo < 消耗量？ → 执行 reload()（消耗36耀魂，3秒冷却）
  │
  └─> Ammo ≥ 消耗量
        │
        ├─> TripleOn && !EnergyOn（状态一）
        │   消耗 1 Ammo → shootSmartBullets() → 3枚追踪幻影剑
        │
        └─> EnergyOn && !TripleOn（状态二）
            消耗 6 Ammo → shootEnergyBullet() → 8枚穿透能量弹
            附加 10 tick 冷却
```

**关键条件判断**：

```java
boolean TripleOn = requireSE(TripleBullet).requireSA(CHARGE_SHOT).match() != null;
boolean EnergyOn = requireSE(EnergyBullet).requireSA(OVER_CHARGE).match() != null;
```

- A 形态绑定 `CHARGE_SHOT`，因此 `TripleOn = true`，`EnergyOn = false`
- A 形态的普通攻击模式为 **三连发智能追踪弹**，每次消耗 **1 发弹药**

### 3.2 三连发追踪弹参数

| 参数       | 普通模式         | 爆炸模式     |
| ---------- | ---------------- | ------------ |
| 每轮发射数 | 3                | 1            |
| 散布角     | 15°              | 3°           |
| 速度       | 1.0              | 0.33         |
| 尾迹节点   | 8                | 48           |
| 索敌范围   | 15 + 扫荡×5      | 35 + 扫荡×15 |
| 追踪角度   | 18°              | 6°           |
| 爆炸半径   | 0                | 2 + 附魔等级 |
| 伤害倍率   | ×1               | ×5           |
| 穿墙       | 是 (noClip=true) | 否           |

### 3.3 伤害公式对比

| 场景                       | 伤害公式                                                   |
| -------------------------- | ---------------------------------------------------------- |
| 三连发追踪弹（A形态普攻）  | `(base + refine*0.1+√refine*1.5) × (1+power*0.15) × ratio` |
| dumpAmmo 幻影剑（A形态SA） | `(base + refine*0.1+√refine*1.5) × (1+power*0.10) × 3.0`   |

---

## 四、连段衍生效果分析

### 4.1 SA 触发时的弹药倾泻

SA `CHARGE_SHOT` 触发的 [`dumpAmmo()`](src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/SmartPistolMode.java:82) 产生以下衍生效果：

1. **弹药转化**：全部剩余弹药按 1:3 转化为幻影剑（或 1:1 为导弹）
2. **球面弹幕**：使用斐波那契球面算法，生成 3D 均匀分布的弹幕
3. **自动索敌分配**：优先攻击锁定目标，无锁定时按距离最近分配
4. **爆炸模式叠加锁定**：导弹命中前持续追踪，同时叠加 MISSILE_LOCKED 效果
5. **形态转换**：弹药清空后，自动切换为 B 形态

### 4.2 形态转换后的状态

转换到 B 形态后：

- SA 变为 `OVER_CHARGE`
- 普通攻击变为能量霰弹（6 发/次）
- 特效色变为荧光绿
- 纹理变为过载版

---

## 五、特殊效果机制分析

### 5.1 TripleBullet（三连弹）

| 属性         | 值                                 |
| ------------ | ---------------------------------- |
| **注册名**   | `triple_bullet`                    |
| **稀有度**   | 40（未升级）                       |
| **SE槽位**   | 3                                  |
| **触发条件** | 拥有 SE + 当前 SA 为 `CHARGE_SHOT` |

**机制**：

- 每次挥砍消耗 **1 发 Ammo**
- 发射 3 枚 SEEK 模式幻影剑
- 子弹使用 `MovingMode.SEEK`，自动追踪最近目标
- 随机散布角 15°，穿透地形
- 每发伤害计算公式同 dumpAmmo 但 `ratio` 继承自挥砍倍率

### 5.2 弹药系统综合分析

#### 装填机制（Reload）

定义于 [`GunBladeEffects.java:301`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/gunblade/GunBladeEffects.java:301)：

| 条件          | 代价    | 效果           | 冷却          |
| ------------- | ------- | -------------- | ------------- |
| Ammo < 消耗量 | 36 耀魂 | Ammo 回满至 36 | 60 tick (3秒) |

#### 弹药消耗规则

| 形态   | 动作                 | 消耗             |
| ------ | -------------------- | ---------------- |
| A 形态 | 普通攻击（三连发）   | 1 发             |
| A 形态 | SA（弹药倾泻）       | 全部（最高36发） |
| B 形态 | 普通攻击（能量霰弹） | 6 发             |
| B 形态 | SA（BFG 巨炮）       | 全部（最高36发） |

---

## 六、核心玩法流派总结

### 流派一：远程风筝流 — 持续火力压制

**核心循环**：

```
走位射击 → Ammo耗尽 → 自动装填(耀魂) → 继续射击
```

- 每次攻击消耗 1 发弹药，发射 3 枚追踪弹
- 满弹 36 发可连续射击 36 轮（108 发追踪弹），这是极恐怖的火力持续性
- 只要有耀魂储备，理论上无限弹药
- **优势**：远程安全输出，完全不需要瞄准，追踪弹自动索敌

### 流派二：爆发终结流 — SA 核弹倾泻

**核心循环**：

```
攒满弹药 → 释放 SA(CHARGE_SHOT) → dumpAmmo() → 108发追踪弹幕 → 清空弹药 → 转为B形态
```

- 满弹 SA 产生 108 发追踪幻影剑（无爆炸 SE）
- 配合 ExplosiveBullet SE 则产生 36 发精炼导弹，每发带爆炸 + MISSILE_LOCKED
- **高精炼 + 高附魔**时伤害极其恐怖

### 流派三：双形态战术循环

**完整循环**：

```
A形态(CHARGE_SHOT) → SA倾泻弹药 → 转为B形态
B形态(OVER_CHARGE)  → BFG巨炮 → 转为A形态
```

这是最核心的玩法设计。A形态负责用低消耗三连发攒弹药、风筝消耗。弹药满后释放 SA 打出核弹爆发，自动切入 B 形态。B 形态可以立即再用 SA（BFG）打出第二次爆发，再变回 A 形态。形成 **A→B→A→B** 的无限循环。

### 战斗决策树

```
                    ┌─ 杂鱼战：反复普攻（三连发追踪）
                    │
  持有A形态(青蓝) ──┼─ 精英战：普攻攒弹 → 满弹SA(108枚核弹) → 转B
                    │
                    └─ Boss战：攒满弹药 → SA倾泻 → 转B → BFG → 转A → 循环
```

---

## 七、联动设计理念

### 7.1 TripleBullet + EnergyBullet 的形态绑定

两个 SE 同时注册在武器上，但通过 SA 检测实现 **技能激活切换**：

```
A形态(CHARGE_SHOT)  → TripleOn = true  → 三连发追踪弹（低消耗高频率）
B形态(OVER_CHARGE)  → EnergyOn = true  → 能量霰弹（高消耗大范围）
```

一把武器，两种射击模式，通过形态转换实现无缝切换。

### 7.2 弹药系统与 SA 的协同

弹药不仅仅是普通攻击的消耗品，更是 SA 爆发的 **伤害倍率放大器**：

- `dumpAmmo()`: 伤害 ∝ 弹药数 × 3（幻影剑数量）
- `BFGShot()`: 伤害 ∝ 弹药数（直接作为伤害乘数）

**弹药管理**成为整个武器的核心 Skill Floor/Skill Ceiling：何时普攻消耗、何时攒弹爆发、何时切换形态，构成完整的决策循环。

### 7.3 精炼与附魔的深度耦合

- **POWER_ARROWS** 替代传统锋利：A形态倍率 `1 + lv × 0.15`（力量 V = 175%），B形态 BFG 倍率 `1 + lv × 0.25`（力量 V = 225%）
- **SWEEPING_EDGE** 增加索敌范围：`15 + lv × 5`，直接提升追踪弹的实战效能
- **重铸系统**：`sqrt(refine)` 前期收益极高，鼓励玩家投入重铸资源

这打破了传统拔刀剑「锋利 + 横扫 + 节肢」的附魔定式，开创了全新的 **Gunblade 附魔体系**。
