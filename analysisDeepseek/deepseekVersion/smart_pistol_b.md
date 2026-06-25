# 白色之神 智能手枪B (SmartPistol B — OVER_CHARGE) 深度分析报告

> 分析基于 [`FantasyDesire`](src/main/java/tennouboshiuzume/mods/FantasyDesire) 源代码，聚焦 B 形态（Over Charge）的底层实现与玩法设计。与 A 形态共享底层弹药系统、实体投射物基础框架。

---

## 一、基础属性提取

注册于 [`FantasySlashBladeBuiltInRegistry.java:111`](src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:111) 的 `SmartPistolB`。

| 属性             | 值                                                    | 与A形态差异                  |
| ---------------- | ----------------------------------------------------- | ---------------------------- |
| **注册名**       | `smart_pistol_b`                                      | 不同                         |
| **特效颜色**     | `0x99FF00`（荧光绿）                                  | **不同** — A为 `0x00FFFF`    |
| **纹理**         | `models/smartpistol_oc.png`（过载版）                 | **不同**                     |
| **模型**         | `models/smartpistol.obj`                              | 共享                         |
| **携带方式**     | `CarryType.KATANA`（腰间）                            | 相同                         |
| **基础攻击修正** | `3.0F`                                                | 相同                         |
| **最大伤害**     | `256`                                                 | 相同                         |
| **默认剑类型**   | `SwordType.BEWITCHED`（妖刀）                         | 相同                         |
| **出厂附魔**     | `POWER_ARROWS` I, `FALL_PROTECTION` V, `UNBREAKING` V | 相同                         |
| **特殊能量**     | `Ammo`（弹药），最大 36 发，类型 `Gunblade`           | 相同                         |
| **SA**           | `OVER_CHARGE`                                         | **不同** — A为 `CHARGE_SHOT` |
| **SE**           | `EnergyBullet`(60,2), `TripleBullet`(40,3)            | 注册相同，**触发条件不同**   |

**核心差异总结**：A 和 B 共享除 SA、纹理、特效色之外的所有基础属性。**SA 的不同决定了普通攻击模式、SA 爆发方式、以及战术定位的完全不同。**

---

## 二、SA 技能分析：OVER_CHARGE

### 2.1 SA 注册与连段入口

在 [`FDSlashArtRegistry.java:24`](src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSlashArtRegistry.java:24) 中：

```java
OVER_CHARGE = FD_SLASH_ARTS.register("over_charge",
    () -> new FDSlashArts((e) -> FDCombo.OVER_CHARGE.getId(), 2));
```

- **冷却值**: `2`（与 CHARGE_SHOT 相同）
- **连段入口**: [`FDCombo.OVER_CHARGE`](src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:696)

### 2.2 连段流转路径

```
OVER_CHARGE (入口)
  │  AntiNTR 检测：确认主手物品翻译键含 "smart_pistol"
  │  通过 → smart_pistol_b_to_a
  │  不通过 → SlashBlade.prefix("none")
  ▼
SMART_PISTOL_B_TO_A (转换状态, priority=80)
  │  clickAction:
  │    1. SmartPistolMode.BFGShot(entity, state, fdState)  ← BFG巨炮
  │    2. SmartPistolMode.TransformToA(state, fdState)     ← 转为A形态
  ▼
SlashBlade.prefix("none") ← 连段终止
```

**与 A 形态的关键区别**：B 形态的 SA 执行顺序是 **先发射 BFG，后转换为 A 形态**。BFG 消耗全部弹药造成毁灭性伤害，然后切换到 A 形态进入低消耗三连发模式。

### 2.3 BFGShot() 深度解析

定义于 [`SmartPistolMode.java:41`](src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/SmartPistolMode.java:41)。

#### 2.3.1 实体：EntityFDBFG

BFG 发射的实体是 [`EntityFDBFG`](src/main/java/tennouboshiuzume/mods/FantasyDesire/entity/EntityFDBFG.java)，继承链：

```
EntityFDPhantomSword → EntityFDEnergyBullet → EntityFDBFG
```

`EntityFDBFG` 的特性：

| 特性         | 参数                  |
| ------------ | --------------------- |
| 缩放         | 2.0f                  |
| 爆炸半径     | **25.0f**（极大范围） |
| 速度         | 1.0                   |
| 追踪延迟     | 15 tick               |
| 飞行持续时间 | 200 tick（10秒）      |
| 尾迹         | 有                    |
| 多段命中     | 是                    |

#### 2.3.2 伤害计算

```java
float baseDamage = state.getBaseAttackModifier() + state.getAttackAmplifier();
int refine = state.getRefine();
float refineBonus = (float) (refine * 0.1f + Math.sqrt(refine) * 1.5f);
int enchantLevel = blade.getEnchantmentLevel(Enchantments.POWER_ARROWS);
float enchantMultiplier = 1.0f + (enchantLevel * 0.25f);  // ← 注意！比A形态高
float finalDamage = (float) ((baseDamage + refineBonus) * enchantMultiplier * ammo);
```

**关键差异**：BFG 的附魔倍率为 `1 + lv × 0.25`，比 A 形态的 `1 + lv × 0.10`（dumpAmmo）和 `1 + lv × 0.15`（普攻）都要高。力量 V 时：

- B 形态 BFG：`1 + 5 × 0.25 = 2.25`（225% 伤害）
- A 形态 dumpAmmo：`1 + 5 × 0.10 = 1.50`（150% 伤害）

**弹药倍率**：`finalDamage` 直接乘以 `ammo`（剩余弹药数），弹药越多伤害越高。

#### 2.3.3 BFG 特殊伤害机制 — customEffectFired

[`EntityFDBFG.customEffectFired()`](src/main/java/tennouboshiuzume/mods/FantasyDesire/entity/EntityFDBFG.java:42) 每 2 tick 触发一次：

```
每2 tick
  ├─> 锁定半径 25 格内所有目标
  ├─> 对每个目标发射闪电（LightBolt粒子）
  ├─> 每次闪电造成 finalDamage × 0.2 的伤害
  └─> invulnerableTime = 0（无视无敌帧）
```

这意味着 BFG 在 10 秒飞行期间，对范围内的敌人每 2 tick 造成一次伤害，**总计约 100 次伤害判定**。

#### 2.3.4 BFG 爆炸伤害 — doExplosive()

BFG 命中或到达延迟上限时触发 [`EntityFDBFG.doExplosive()`](src/main/java/tennouboshiuzume/mods/FantasyDesire/entity/EntityFDBFG.java:70)：

- 爆炸半径 25 格
- **爆炸伤害计算**：
  ```java
  int tickRemain = getDelay() - tickCount;   // 剩余飞行时间
  float damage = (float) (this.getDamage() * 0.2f);  // 单次闪电伤害
  int remainDamageTimes = tickRemain / 10;             // 剩余可伤害次数
  return target.hurt(source, remainDamageTimes * damage);  // 提前引爆造成剩余全额
  ```
- 提前引爆时（如撞墙），会赔付剩余的全部预期伤害
- 允许 BFG 提前碰撞造成原本要持续 10 秒的总伤害

#### 2.3.5 弹药消耗

执行完毕后 `fdState.setSpecialCharge(0)`，弹药彻底清空。

---

## 三、动作连段解析

### 3.1 普通攻击连段（非SA）

B 形态（OVER_CHARGE）的普通攻击同样由 [`GunBladeEffects.java:40`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/gunblade/GunBladeEffects.java:40) 接管。

**关键条件判断**（与 A 形态对调）：

```java
boolean TripleOn = requireSE(TripleBullet).requireSA(CHARGE_SHOT).match() != null;
boolean EnergyOn = requireSE(EnergyBullet).requireSA(OVER_CHARGE).match() != null;
```

- B 形态绑定 `OVER_CHARGE`，因此 `EnergyOn = true`，`TripleOn = false`
- B 形态的普通攻击模式为 **能量霰弹**，每次消耗 **6 发弹药**

### 3.2 能量霰弹参数

| 参数       | 普通模式                                                  | 雷电模式(ThunderBullet) |
| ---------- | --------------------------------------------------------- | ----------------------- |
| 每次发射数 | **8 发**                                                  | 8 发                    |
| 消耗弹药   | **6 发**                                                  | 6 发                    |
| 单发穿透   | **3 个目标**                                              | 3 个目标                |
| 散射角     | 15°                                                       | 15°                     |
| 速度       | 3.0                                                       | 3.0                     |
| 爆炸半径   | 0                                                         | 6                       |
| 子弹颜色   | 形态特效色                                                | `0xFFFF00`（黄色）      |
| 伤害公式   | `(base + refine×0.2 + √refine×1.5) × (1+lv×0.15) × ratio` | 同左                    |
| 附加冷却   | 10 tick                                                   | 10 tick                 |

**重要差异**：能量弹的精炼系数为 `refine × 0.2`，是追踪弹（`refine × 0.1`）的 **2 倍**。这意味着高精炼对 B 形态的加成远超 A 形态。

### 3.3 B 形态弹药经济学

| 动作                 | 弹药消耗   | 弹药效率                        |
| -------------------- | ---------- | ------------------------------- |
| 普通攻击（能量霰弹） | 6 发/次    | 8 发弹丸/6弹药 = 1.33 弹丸/弹药 |
| SA(BFG)              | 全部       | 伤害 ∝ 弹药数                   |
| 装填(自动)           | 消耗36耀魂 | 回满36发                        |

满弹 36 发可进行 6 次能量霰弹射击（共 48 发弹丸），然后装填。

---

## 四、连段衍生效果分析

### 4.1 SA 触发时的 BFG 巨炮

SA `OVER_CHARGE` 触发的 [`BFGShot()`](src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/SmartPistolMode.java:41) 的衍生效果：

1. **弹药转化为 BFG 伤害**：剩余弹药数直接作为伤害乘数
2. **持续范围压制**：BFG 飞行 10 秒，对 25 格内敌人持续造成伤害
3. **爆炸终结**：到期或碰撞时造成巨额爆炸伤害
4. **无视无敌帧**：`invulnerableTime = 0` 确保高频伤害完全生效
5. **形态转换**：BFG 发射后，武器自动切换为 A 形态

### 4.2 命中能量弹时的穿透效果

[`EntityFDEnergyBullet.doExplosive()`](src/main/java/tennouboshiuzume/mods/FantasyDesire/entity/EntityFDEnergyBullet.java:34) 在命中时：

- 对半径内敌人排序，最多伤害 **5 个目标**
- 每个目标受到 `finalDamage` 的全额伤害
- 附带闪电粒子特效
- 无视无敌帧

### 4.3 形态转换后的状态

转换到 A 形态后：

- SA 变为 `CHARGE_SHOT`
- 普通攻击变为三连发追踪弹（1 发/次）
- 特效色变为青蓝色
- 纹理变为标准版

---

## 五、特殊效果机制分析

### 5.1 EnergyBullet（能量弹）

| 属性         | 值                                 |
| ------------ | ---------------------------------- |
| **注册名**   | `energy_bullet`                    |
| **稀有度**   | 60                                 |
| **SE槽位**   | 2                                  |
| **触发条件** | 拥有 SE + 当前 SA 为 `OVER_CHARGE` |

**机制**：

- 每次挥砍消耗 **6 发 Ammo**
- 发射 8 枚 `EntityFDEnergyBullet`（继承自幻影剑的穿透能量弹）
- 穿透 3 个目标（`pierce = 3`）
- **高精炼收益**：`refine × 0.2`（追踪弹的 2 倍）
- 散射角 15°，可搭配 ThunderBullet SE 获得爆炸能力

### 5.2 ThunderBullet 联动

`ThunderBullet`（80稀有度，1槽位）是 B 形态的可选升级 SE：

- 能量弹从普通穿透弹升级为 **雷电爆炸弹**
- 爆炸半径：6 格（附魔等级加成前）
- 伤害属性从物理变为魔法
- 子弹颜色变为 `0xFFFF00`（黄色闪电）

### 5.3 EnergyBullet 与 TripleBullet 的逻辑互斥

```java
int cost = TripleOn && !EnergyOn ? 1 : 6;
```

A 形态（TripleOn=true）消耗 1 弹药，B 形态（EnergyOn=true）消耗 6 弹药。这种设计使得：

- **A 形态**：低消耗、高频率、持续火力
- **B 形态**：高消耗、大范围、爆发压制

### 5.4 弹药系统（共享底层）

与 A 形态完全共享，参见 A 形态文档的[弹药系统分析](smart_pistol_a.md#五特殊效果机制分析)。

---

## 六、核心玩法流派总结

### 流派一：中距离霰弹压制流

**核心循环**：

```
能量霰弹(×6) → 能量霰弹(×6) → ... → 弹药耗尽 → 自动装填
```

- 每次攻击 8 发穿透弹 + 15° 散射，覆盖范围极大
- 适合清理密集杂鱼群
- 穿透 3 个目标，一发清一排

### 流派二：BFG 核弹终结流

**核心策略**：积攒满弹药 → 释放 SA(OVER_CHARGE) → BFG 巨炮

BFG 的多段伤害机制：

```
发射 → 10秒飞行(每2tick范围伤害) → 爆炸终结(赔付剩余伤害)
      ↓
25格半径内持续压制，造成约100次闪电伤害 + 最终爆炸
```

**满配件（高精炼 + 力量 V + 满弹）下的理论伤害**：

```
baseDamage ≈ 3.0 + attackAmplifier
refineBonus (100次重铸) ≈ 100×0.1 + √100×1.5 = 10 + 15 = 25
enchantMultiplier (力量V) = 1 + 5×0.25 = 2.25
ammo = 36

finalDamage = (base + 25) × 2.25 × 36 ≈ (28) × 81 ≈ 2268
```

每次闪电造成 20%（~453 伤害），持续 100 次判定。**总理论伤害量极其恐怖**。

### 流派三：双形态战术循环

**与 A 形态形成完美互补**：

```
A形态(青蓝) → 三连发消耗攒弹 → SA(108发核弹) → 转为B形态
B形态(荧光绿) → 能量霰弹压制 → SA(BFG巨炮) → 转为A形态
```

B 形态在循环中的定位：

1. **承接**：A 形态打完 SA 后转入 B 形态，此时弹药已空，但 B 形态的 SA（BFG）不需要弹药也可以释放... 不对，BFG 也需要弹药。A→B 转换时 dumpAmmo 已经清空了弹药，所以转过来时 B 形态是 **空弹药** 状态，需要先装填或普攻攒弹。

等等，让我重新审视连段逻辑：

**A形态(CHARGE_SHOT)**:

- SA 按下 → `dumpAmmo()` 消耗全部弹药 → `TransformToB()` 转为 B
- 此时 B 形态弹药为 0

**B形态(OVER_CHARGE)**:

- SA 按下 → `BFGShot()` 消耗全部弹药 → `TransformToA()` 转为 A
- 此时 A 形态弹药为 0

所以实际循环中，弹药是 **共享槽位** 的。转形态时不改变弹药量。

A 形态的战斗流程：

1. 初始满弹 36 → 三连发消耗（1发/次）
2. 弹药耗尽 → 自动装填
3. 弹药充足时释放 SA → dumpAmmo 清空 → 转入 B

B 形态的战斗流程：

1. 从 A 转来时弹药为 0 → 需要先装填
2. 装填后 → 能量霰弹（6发/次）压制
3. 弹药充足时释放 SA → BFG 清空 → 转入 A

这样两个形态形成了更复杂的资源循环。

### 战斗决策树

```
                    ┌─ 霰弹清杂鱼：反复能量霰弹（8发穿透弹丸）
                    │
  持有B形态(荧光绿) ─┼─ 弹药耗尽 → 自动装填(36耀魂) → 继续输出
                    │
                    └─ 精英/Boss：装填满弹 → SA(BFG) → 毁灭打击 → 转A
```

---

## 七、联动设计理念

### 7.1 EnergyBullet 与 OVER_CHARGE 的深度绑定

EnergyBullet SE 的设计要求同时拥有 SE 和 `OVER_CHARGE` SA 才能激活。这种设计使得：

- B 形态的普通攻击必然是高消耗、高回报的能量霰弹
- A 形态即使拥有 EnergyBullet SE 也不会误触发
- **形态即模式切换**：SE 的行为完全由当前 SA 决定

### 7.2 BFG — DOOM 风格的拆迁武器

BFG 的设计明显致敬了经典 FPS《DOOM》：

- 大范围持续伤害
- 闪电链特效
- 无视无敌帧
- 爆炸赔付机制确保提前碰撞不会损失伤害

这是对整个模组远程输出体系的有力补充，也是 **唯一一个** 具有 10 秒持续范围压制的技能。

### 7.3 双形态资源循环经济学

```
A形态(低耗高效)           B形态(高耗爆发)
   │                         │
   │  普攻: 1弹→3追踪弹      │  普攻: 6弹→8穿透弹
   │  SA: 全部→108发弹幕     │  SA: 全部→BFG持续伤害
   │  适合: 风筝/攒弹        │  适合: 爆发/压制
   │                         │
   └────── dumpAmmo() ──────→┘
   ←────── BFGShot() ────────┘
```

武器整体鼓励玩家在两个形态之间循环切换，每个形态都有其独特的弹药经济模型和战术定位，而不是简单地"开火—装填"。

### 7.4 与 A 形态的对比总结

| 维度            | A 形态 (CHARGE_SHOT)                          | B 形态 (OVER_CHARGE)                           |
| --------------- | --------------------------------------------- | ---------------------------------------------- |
| **普攻消耗**    | 1 弹药/次                                     | 6 弹药/次                                      |
| **普攻输出**    | 3 发追踪弹                                    | 8 发穿透弹                                     |
| **精炼加成**    | `refine × 0.1`                                | `refine × 0.2`                                 |
| **SA 伤害公式** | `(base + refineBonus) × (1+power×0.10) × 3.0` | `(base + refineBonus) × (1+power×0.25) × ammo` |
| **SA 形态**     | 散射弹幕(3D球面)                              | 单体BFG(持续范围)                              |
| **战术定位**    | 持续输出/风筝                                 | 爆发压制/收割                                  |
| **弹药效率**    | 极高（1弹→3弹丸）                             | 低（6弹→8弹丸）                                |
| **适用场景**    | 持久战、多目标                                | Boss战、清密集群                               |
| **转换目标**    | dumpAmmo后→B形态                              | BFG后→A形态                                    |

### 7.5 进阶 SE 升级路线

虽然默认只有 `EnergyBullet` 和 `TripleBullet`，但代码预留了完整的升级路径：

| SE                | 稀有度 | 模式 | 对B形态的影响                 |
| ----------------- | ------ | ---- | ----------------------------- |
| `ThunderBullet`   | 80     | 独占 | 能量弹升级为雷电爆炸弹(半径6) |
| `ExplosiveBullet` | 100    | 独占 | 改造整个子弹系统为爆炸模式    |

这些进阶 SE 可以进一步改变 B 形态的作战风格，从穿透流转向元素爆炸流。
