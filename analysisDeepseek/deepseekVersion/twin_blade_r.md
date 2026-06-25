# 双子圣灵·右（TwinBladeR）深度分析文档

> **对应形态：** TWIN_SYSTEM_R（终结程式：DOOM 末日）
> **注册名：** `twin_blade_r`
> **特效色：** `0xFF0089`（粉红）
> **纹理：** [`twinbladeright.png`](../../src/main/resources/assets/fantasydesire/models/twinbladeright.png)

---

## 一、基础属性

| 属性                        | 值                                                                                                                                                                        |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **注册名**                  | `twin_blade_r`                                                                                                                                                            |
| **翻译键**                  | `item.fantasydesire.twin_blade`（与L共享）                                                                                                                                |
| **特效颜色**                | `0xFF0089`（粉红）                                                                                                                                                        |
| **纹理**                    | `twinbladeright.png`                                                                                                                                                      |
| **模型**                    | [`twinblade.obj`](../../src/main/resources/assets/fantasydesire/models/twinblade.obj)（与L共享）                                                                          |
| **携带方式**                | `KATANA`                                                                                                                                                                  |
| **基础攻击修正**            | `2.5F`                                                                                                                                                                    |
| **最大伤害**                | `1024`                                                                                                                                                                    |
| **剑类型**                  | `BEWITCHED`                                                                                                                                                               |
| **特殊能量**                | 无（未设置 `maxSpecialCharge`）                                                                                                                                           |
| **特殊类型（specialType）** | `TwinBladeR`                                                                                                                                                              |
| **特殊攻击效果**            | [`resolution`](#resolution伤害类型详解)                                                                                                                                   |
| **SA（Slash Arts）**        | [`TWIN_SYSTEM_R`](#二sa技能分析twin_system_r--doom_slash末日连段) → [`FDCombo.DOOM_SLASH`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:336) |
| **SE（Special Effect）**    | [`TwinSet`](#三特殊效果机制分析)（双刀共鸣）                                                                                                                              |

### 附魔

| 附魔                           | 等级 |
| ------------------------------ | ---- |
| `FLAMING_ARROWS`（火焰箭）     | V    |
| `BLAST_PROTECTION`（爆炸保护） | III  |
| `FIRE_PROTECTION`（火焰保护）  | III  |
| `INFINITY_ARROWS`（无限）      | I    |

> **注册代码位置：** [`FantasySlashBladeBuiltInRegistry.java:207-235`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:207)

### 与 TwinBladeL 的差异对比

| 属性        | TwinBladeR（右）                                                                 | TwinBladeL（左）                                                                 |
| ----------- | -------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| 特效色      | `0xFF0089` 粉红                                                                  | `0x00C8FF` 青蓝                                                                  |
| 纹理        | `twinbladeright.png`                                                             | `twinbladeleft.png`                                                              |
| specialType | `TwinBladeR`                                                                     | `TwinBladeL`                                                                     |
| SA          | [`TWIN_SYSTEM_R` → `DOOM_SLASH`](#二sa技能分析twin_system_r--doom_slash末日连段) | [`TWIN_SYSTEM_L` → `MOOD_SLASH`](#二sa技能分析twin_system_l--mood_slash心境连段) |
| 连段风格    | **末日连段**（DOOM）：瞬移→预热乱舞→烧血循环→终结重击                            | **心境连段**（MOOD）：蓄力瞬步→跃升斩→双旋斩→重锤落+符文剑                       |

---

## 二、SA技能分析：TWIN_SYSTEM_R → DOOM_SLASH（末日连段）

### 2.1 整体设计理念

`TWIN_SYSTEM_R` 注册于 [`FDSlashArtRegistry.java:20`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSlashArtRegistry.java:20)，其连段入口为 [`FDCombo.DOOM_SLASH`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:336)。

> 代码注释原文："参考自血天下鸡舞乱刀。施放后瞬移自动锁定15m内敌人。主动输入攻击键可以循环2，3连段。每次输入攻击循环会烧血。直到停止输入或者玩家低于50%血量。"

R形态的DOOM连段是一套**高风险高回报的地面爆发型combo**，核心思路为：**DominateStep瞬移追敌 → 预热乱舞 → 烧血循环（可重复） → 终结重击 + 闪电风暴**。

### 2.2 连段结构详解

#### 阶段0：施放前检测 [`DOOM_SLASH`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:336)

```java
// 伪逻辑
if (TwinSlash.AntiNTR(entity))
    → doom_slash_0  // 双持验证通过，进入连段
else
    → twin_mode     // 未双持，进入形态切换模式
```

- **优先级：** 50（低于MOOD的80）
- **核心验证：** [`TwinSlash.AntiNTR()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/TwinSlash.java:181)
- 未满足双持条件时回退到 `TWIN_MODE`

#### 阶段1：闪击 [`DOOM_SLASH_0`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:346)

| 属性       | 值                         |
| ---------- | -------------------------- |
| 帧范围     | 1-33                       |
| **优先级** | **100**（所有combo中最高） |
| 动画       | `testLocation`（默认姿势） |
| 超时跳转   | 第32帧→ `doom_slash_1`     |

> **优先级 100 的意义：** DOOM_SLASH_0 的优先级是 MOOD_SLASH_0（80）以及 MOOD_SLASH 入口（80）等的最高级，确保 DOMINATE 瞬步在任何情况下优先执行。

**核心动作：**

- 第30帧（约1.5秒动画后）调用 [`TwinSlash.DominateStep()`](#dominatestep-支配瞬步) 执行**支配瞬步**
- 与 RippedStep 不同，DominateStep 范围更小（15格）但传送后**自动面向目标**

#### 阶段2：预热 [`DOOM_SLASH_1`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:358)

| 属性       | 值                     |
| ---------- | ---------------------- |
| 帧范围     | 700-720                |
| **优先级** | **100**                |
| 动画       | `ExMotionLocation`     |
| 超时跳转   | 第13帧→ `doom_slash_2` |

**核心动作（第6-14 tick）：多方向DoomSlash乱舞**

| Tick | 动作                                        | 参数      |
| ---- | ------------------------------------------- | --------- |
| 6    | `DoomSlash(roll=-30, ratio=0.244)`          | 前方偏左  |
| 6    | `DoomSlash(roll=180-35, ratio=0.244)`       | 后方      |
| 7    | `DoomSlash(roll=-90+随机*180, ratio=0.244)` | 随机方向1 |
| 8    | `DoomSlash(roll=90+随机*180, ratio=0.244)`  | 随机方向2 |
| 9    | `DoomSlash(roll=-90+随机*180, ratio=0.244)` | 随机方向3 |
| 10   | `DoomSlash(roll=90+随机*180, ratio=0.244)`  | 随机方向4 |
| 11   | `DoomSlash(roll=-90+随机*180, ratio=0.244)` | 随机方向5 |
| 12   | `DoomSlash(roll=90+随机*180, ratio=0.244)`  | 随机方向6 |
| 13   | `DoomSlash(roll=-90+随机*180, ratio=0.244)` | 随机方向7 |
| 14   | `DoomSlash(roll=90+随机*180, ratio=0.244)`  | 随机方向8 |

> **预热阶段特点：** 共 10 次 `DoomSlash`，伤害比率为 0.244（较低的起手伤害），方向随机化。这是一个"热身"阶段，用低伤害乱舞锁定目标位置。

**击中效果：** 目标眩晕

#### 阶段3：循环狂热（循环A） [`DOOM_SLASH_2`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:401)

| 属性              | 值                                                   |
| ----------------- | ---------------------------------------------------- |
| 帧范围            | 710-720                                              |
| 优先级            | 80                                                   |
| 超时跳转（第3帧） | `health > maxHealth/2` → `doom_slash_3`（继续循环）  |
|                   | `health <= maxHealth/2` → `doom_slash_4`（强制终结） |

**核心动作（第0 tick）：**

1. `DoomSlash(roll=-90+随机, ratio=0.244)` — 随机方向斩击
2. **`DominateStep()`** — 再次瞬移追敌（每次循环都会重新瞬移）
3. 如果使用者是玩家 → **自伤 2 点**（`player.hurt(player.damageSources().playerAttack(player), 2f)`）

**第1-6 tick：** 额外 6 次随机方向 DoomSlash

#### 阶段4：循环狂热（循环B） [`DOOM_SLASH_3`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:440)

| 属性              | 值                                                   |
| ----------------- | ---------------------------------------------------- |
| 帧范围            | 710-720                                              |
| 优先级            | 80                                                   |
| 超时跳转（第3帧） | `health > maxHealth/2` → `doom_slash_2`（返回循环A） |
|                   | `health <= maxHealth/2` → `doom_slash_4`（强制终结） |

**功能与 `doom_slash_2` 完全一致**，只是跳转目标不同（`2→3→2→3→...` 形成交替循环）。

> **循环逻辑：**
>
> ```
> doom_slash_2 ←→ doom_slash_3  （交替循环）
>     │                              │
>     └── health <= 50% ────────────┘
>              │
>              ▼
>         doom_slash_4（终结）
> ```
>
> **每次循环消耗 2 点生命值**，当玩家生命 ≤ 50% 时强制结束循环。

#### 阶段5：重锤终结 [`DOOM_SLASH_4`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:479)

| 属性     | 值                 |
| -------- | ------------------ |
| 帧范围   | 500-576            |
| 优先级   | 80                 |
| 动画     | `ExMotionLocation` |
| 超时跳转 | 第26帧→ `none`     |

**核心动作（第8 tick）：**

1. `AttackManager.doSlash(entityIn, 90-15, false, false, 2.875f)` — **前方重锤**（伤害系数 2.875）
2. `AttackManager.doSlash(entityIn, 90+15, true, false, 2.875f)` — **后方重锤**
3. `entityIn.moveRelative(0.8f, new Vec3(0, -0.5, 5.25))` — **超快速下坠突进**（水平移动距离 5.25，远大于MOOD的1.25）
4. **关键：** [`TwinSlash.ConvertForm()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/TwinSlash.java:202) 同时转换**主手和副手**的形态！
   - 主手 `TwinBladeR` → `TwinBladeL`
   - 副手 `TwinBladeL` → `TwinBladeR`

**击中效果（闪电风暴爆发）：**

- 目标眩晕 40 tick
- 如果目标还存活且攻击者存在：
  - **4 次** 闪电粒子爆发（青蓝 `0x00C8FF` + 粉红 `0xFF0089` 各 4 次，共 8 道闪电）
  - 闪电从目标位置向 **随机 360° 方向** 延伸 10 格
  - 这是 DOOM 连段最华丽的视觉效果

### 2.3 DOOM连段总览图

```
TWIN_SYSTEM_R (SA激活)
    │
    ├─ AntiNTR() 验证失败 → TWIN_MODE（形态切换）
    │
    └─ AntiNTR() 验证通过 → DOOM_SLASH_0 (帧1-33, 优先级100)
         │ 第30帧: DominateStep() 支配瞬步(15格)
         ▼
         DOOM_SLASH_1 (帧700-720, 优先级100)
         │ 第6-14帧: 预热乱舞 (10次随机DoomSlash)
         ▼
    ┌── DOOM_SLASH_2 (帧710-720)
    │   │ 每次循环: DoomSlash + DominateStep + 自伤2点
    │   │ 第3帧检测: Health > 50%? → DOOM_SLASH_3
    │   │            Health ≤ 50%? → DOOM_SLASH_4
    │   ▼
    │   DOOM_SLASH_3 (帧710-720)
    │   │ 每次循环: DoomSlash + DominateStep + 自伤2点
    │   │ 第3帧检测: Health > 50%? → DOOM_SLASH_2 (继续循环)
    │   │            Health ≤ 50%? → DOOM_SLASH_4 (强制终结)
    └── ─ ─ ─ ─ ─ ┘
              │
              ▼
         DOOM_SLASH_4 (帧500-576)
         │ 第8帧: 重锤落地 + ConvertForm()形态转换
         │ 击中: 4次闪电风暴 (青蓝+粉红交替)
         ▼
         none (连段结束)
```

---

## 三、特殊效果机制分析

> **TwinSet、Resolution、VoidTransform 三个机制的详细分析已在 [`twin_blade_l.md`](./twin_blade_l.md#三特殊效果机制分析) 中完整阐述。** 此处仅作简要引用，并补充 R 形态视角的特殊要点。

### 3.1 TwinSet（双刀共鸣）SE — R形态视角

**事件监听器：** [`TwinBladeEffects.onTwinSlash()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/twinblade/TwinBladeEffects.java:19)

当主手为 TwinBladeR 时触发 TwinSet：

```java
// 副手追加攻击使用副手的 colorCode
int offColor = offCtx.state.getColorCode();  // 副手如果是L则为0x00C8FF
AddonSlashUtils.doAddonSlash(player, roll - 180, ..., offColor, ...);
```

- 主手 R（粉红）攻击 → 副手 L（青蓝）追加反向斩击
- 视觉效果：粉红主斩 + 青蓝交叉斩

### 3.2 Resolution（决断）伤害类型

详细分析见 [`twin_blade_l.md#3.2`](./twin_blade_l.md#32-resolution决断伤害类型)。

**核心效果：** 每次攻击造成 50% 额外魔法伤害，重置无敌帧。

### 3.3 VoidTransform（虚空转化）

详细分析见 [`twin_blade_l.md#3.3`](./twin_blade_l.md#33-voidtransform虚空转化)。

**转化时有 50% 概率变为 TwinBladeR。**

---

## 四、形态转换系统

> 详细分析见 [`twin_blade_l.md#四`](./twin_blade_l.md#四形态转换系统)。

### R形态视角的关键点：

1. **DOOM_SLASH_4 终结后自动 ConvertForm()** — 主手 R→L，副手 L→R
2. 转换后主手变为 TwinBladeL，SA 切换为 `TWIN_SYSTEM_L`（MOOD）
3. **战术意义：** DOOM 连段结束后自动进入 MOOD 连段的入口状态，实现 **R(末日) → L(心境) 的无缝衔接**
4. 配合 MOOD_SLASH_3 的转换（L→R），形成完整的 **L→R→L→R** 永续循环

---

## 五、核心玩法流派总结

### 5.1 R形态（DOOM末日）流派特点

| 维度         | 特点                                           |
| ------------ | ---------------------------------------------- |
| **定位**     | 地面爆发型 / 高风险高回报                      |
| **核心位移** | `DominateStep`（15格瞬移至目标身边并面向目标） |
| **连段节奏** | 中速，但**可持续循环**                         |
| **控制力**   | 中等（仅眩晕）                                 |
| **爆发点**   | 循环期间的连续 DoomSlash + 终结重锤            |
| **唯一风险** | **每次循环自伤 2 点**，生命 ≤ 50% 强制终结     |
| **形态转换** | 终结时自动触发                                 |
| **适用场景** | 群体战、需要持续压制的战斗、Boss战消耗         |

### 5.2 DOOM 连段的烧血机制

**这是双子圣灵双刀流中最具特色的机制：**

1. 每次循环（`doom_slash_2` 或 `doom_slash_3`）消耗玩家 2 点生命值（1❤）
2. 循环可以**无限持续**，条件是玩家生命值 > 50%
3. 当生命值 ≤ 50% 时，强制跳转到终结阶段 `doom_slash_4`
4. 循环期间每次都会触发 `DominateStep` 瞬移追敌，确保不丢失目标

**战术考量：**

- **高生命时：** 可以长时间维持循环，持续输出高额伤害
- **低生命时：** 自动终结，安全性设计防止玩家"烧死自己"
- **配合 Resolution：** 每次攻击 50% 额外魔法伤害，即使自伤也能通过高输出来弥补
- **配合治疗手段：** 如果有生命恢复 buff 或吸血效果，可以延长循环持续时间

### 5.3 双刀流整体战术循环

```
[起始] 主手R + 副手L
    │
    ├─ SA激活 → DOOM连段（末日）
    │   ├─ 预热：DominateStep + 10次乱舞
    │   ├─ 循环：反复瞬移斩击（烧血）
    │   └─ 终结：重锤 + ConvertForm
    │       │
    │       ▼
    │   主手变为L + 副手变为R
    │       │
    │       └─ SA激活 → MOOD连段（心境）
    │           ├─ 突进：RippedStep(35格)
    │           ├─ 浮空：跃升斩 + 双旋斩
    │           ├─ 落地：重锤 + ConvertForm
    │           └─ 符文剑：25%生命追击
    │               │
    │               ▼
    │           主手变回R + 副手变回L
    │               │
    │               └─ 回到起点，循环继续
    │
    └─ 普通攻击 → TwinSet触发 → 双刀交叉斩
```

---

## 六、联动设计理念

### 6.1 DOOM + MOOD 的阴阳互补

| 维度           | MOOD（心境/L）          | DOOM（末日/R）           |
| -------------- | ----------------------- | ------------------------ |
| **战斗哲学**   | 柔（心境的沉静与精准）  | 刚（末日的狂暴与毁灭）   |
| **机动方式**   | RippedStep 超远距离突袭 | DominateStep 近距离支配  |
| **控制方式**   | 浮空+缓慢坠落（强控）   | 眩晕（弱控但高频）       |
| **输出模式**   | 单次高爆发（符文剑25%） | 持续循环输出（烧血机制） |
| **风险等级**   | 低（无自伤）            | 高（自伤但可循环）       |
| **形态转换后** | L→R 进入末日模式        | R→L 进入心境模式         |

**设计核心：** 两把刀不是独立武器，而是一个**完整战斗系统的两个面**。玩家需要在 MOOD（安全控制）和 DOOM（高风险爆发）之间找到节奏，通过形态转换实现无缝衔接。

### 6.2 双子圣灵的剧情/设定暗示

- "圣灵"之名暗示它们是有意识的灵魂武器
- 虚空转化机制暗示它们来自虚空深处
- L/R 双刀配对 + 形态互换暗示"双生灵魂"的设定
- 连段注释中提到的"黑兽·卯 奥义 云解显现"和"血天下鸡舞乱刀"暗示了灵感来源

### 6.3 与其他机制的交互

| 联动对象                  | 效果                              |
| ------------------------- | --------------------------------- |
| TwinSet（SE）             | 每次主手攻击时副手自动追加        |
| Resolution（伤害类型）    | 所有伤害 +50% 魔法伤害            |
| ConvertForm（形态转换）   | 连段终结时自动切换，维持 L↔R 平衡 |
| VoidTransform（虚空转化） | 隐藏获取途径，丰富世界观          |

---

## 七、DoomSlash 与 MoodSlash 技术参数对比

| 参数       | [`DoomSlash()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/TwinSlash.java:154) | [`MoodSlash()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/TwinSlash.java:104) |
| ---------- | ----------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| 调用方法   | `doAddonSlashWithEvent()`                                                                             | `doAddonSlash()`                                                                                      |
| 事件触发   | ✅ 派发 `DoSlashEvent`（触发 DamageConverter 和 TwinSet）                                             | ❌ 不派发事件                                                                                         |
| 可指定参数 | roll, ratio                                                                                           | Xrot, Yrot, roll, offset                                                                              |
| 角度控制   | roll + YRot，无 XRot                                                                                  | 完全自定义 X/Y/Roll                                                                                   |
| 伤害来源   | 事件处理                                                                                              | 直接调用                                                                                              |
| 用途       | 需要触发伤害类型转换（Resolution）的斩击                                                              | 纯视觉效果/不需要伤害类型转换的斩击                                                                   |

> **关键差异：** `DoomSlash` 使用 `doAddonSlashWithEvent`，这意味着它会触发 `SlashBladeEvent.DoSlashEvent`，进而触发 `DamageConverterEvent.OnSlash()` 将伤害转换为 `resolution` 类型。而 `MoodSlash` 使用 `doAddonSlash`（无事件），不触发伤害类型转换。

---

## 八、总结

TwinBladeR（双子圣灵·右）承载的 `TWIN_SYSTEM_R`（DOOM末日连段）代表了双子圣灵双刀流中**狂暴、持续、高风险**的一面。与 TwinBladeL 的 MOOD 心境连段形成完美互补：

- **L（心境）：** 先手控制、精确打击、安全输出
- **R（末日）：** 持续压制、血性爆发、终结收割

真正的双刀流大师会在两种形态之间自如切换——先用 MOOD 连段将敌人浮空控制，通过形态转换为 DOOM 后继续压制；或者先用 DOOM 烧血循环消耗，再转 MOOD 形态用符文剑收割。这才是双子圣灵设计的**真正精髓**。

---

> **分析文件：** [`analysis/deepseekVersion/twin_blade_r.md`](./twin_blade_r.md)
> **对应形态：** TwinBladeR（双子圣灵·右）— TWIN_SYSTEM_R
> **关联文件：** [`twin_blade_l.md`](./twin_blade_l.md)（双子圣灵·左分析）
