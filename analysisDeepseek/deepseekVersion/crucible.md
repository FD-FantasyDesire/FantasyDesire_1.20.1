# Crucible（裁决剑）深度分析

> **分析版本：** DeepSeek v4
> **分析日期：** 2026-06-23
> **模组版本：** FantasyDesire 1.20.1

---

## 一、总览概述

**Crucible（裁决剑）** 是一把定位极其纯粹的 **面板怪**——它拥有全模组最高的基础攻击力（14.0F），但既没有绑定任何自定义 SA（Slash Arts），也没有绑定任何自定义 SE（Special Effects）。它的一切战斗力都建立在 **简单粗暴的平砍** 之上，配合其独有的 **"永劫"（eternity）** 伤害类型，每一次攻击都在 **永久削减敌人的生命值上限**。

| 项目         | 值                       |
| ------------ | ------------------------ |
| 注册名       | `fantasydesire:crucible` |
| 中文名       | §4§l裁决剑               |
| 特效色       | `0xff0000`（血红）       |
| 模型         | `models/crucible.obj`    |
| 纹理         | `models/crucible.png`    |
| 携带方式     | `RNINJA`（忍者背持）     |
| 剑类型       | `BEWITCHED`              |
| 特殊类型     | `crucible`               |
| 特殊攻击效果 | `eternity`（永劫）       |
| 自定义 SA    | ❌ 无                    |
| 自定义 SE    | ❌ 无                    |

---

## 二、维度一：基础属性分析

### 1. 攻击力 —— 全模组之冠

```java
.baseAttackModifier(14.0F)
```

**14.0F** 的攻击力加成使得 Crucible 成为整个 FantasyDesire 模组中基础攻击最高的刀，**没有任何其他武器能在纯面板上超越它**。

| 对比对象                   | 基础攻击        | 备注              |
| -------------------------- | --------------- | ----------------- |
| **Crucible（裁决剑）**     | **14.0**        | 🏆 **全模组最高** |
| Crimson Scythe（深红恶魔） | 10.0            | -4.0              |
| Chikeflare（奇克芙蕾雅）   | 8.0             | -6.0              |
| Twin Blade（双子圣灵）     | 8.0             | -6.0              |
| Smart Pistol（智能枪刃）   | 6.0             | -8.0              |
| Starless Night（无星之夜） | 10.0            | -4.0              |
| 原版下界合金剑             | 3.0（+7.0基础） | 仅+3攻击伤害      |

> 注：SlashBlade 的 `baseAttackModifier` 是直接加在玩家基础攻击力上的额外数值，因此 14.0F 意味着手持时玩家面板攻击力约为 `1（空手）+ 14.0 = 15.0`。

### 2. 耐久度

```java
.maxDamage(1561)
```

1561 点耐久 + **UNBREAKING V** 附魔，实际等效耐久约为 `1561 × (5+1) ≈ 9366` 次使用，极其耐用。

### 3. 附魔配置

| 附魔                    | 等级          | 效果                                      |
| ----------------------- | ------------- | ----------------------------------------- |
| **亡灵杀手（SMITE）**   | **X（10级）** | 对亡灵生物额外造成 `10 × 2.5 = 25` 点伤害 |
| **耐久（UNBREAKING）**  | V（5级）      | 80%概率不消耗耐久                         |
| **经验修补（MENDING）** | I（1级）      | 吸收经验值修复耐久                        |

**关键分析：** SMITE X 是 Crucible 的核心附魔。由于亡灵杀手对亡灵生物（僵尸、骷髅、凋灵、凋灵骷髅、僵尸猪灵等）每级增加 2.5 点伤害，SMITE X 意味着 **对亡灵生物额外造成 25 点伤害**。配合 14.0 基础攻击，一刀对亡灵生物可造成约 **40 点伤害（20颗心）**，即 **一刀秒杀绝大多数亡灵怪物**。

### 4. 携带方式

`RNINJA`（忍者背持）—— 刀背在背后，兼具美观与实用性。

---

## 三、维度二：SA 技能分析

### ❌ 无自定义 SA

Crucible 的注册代码中 **没有调用 `.slashArtsType()` 方法**，这意味着它没有绑定任何自定义 Slash Arts。

```java
// Crucible 注册代码（关键部分）
bootstrap.register(Crucible,
    new FantasySlashBladeDefinition(
        FantasyDesire.prefix("crucible"),
        RenderDefinition.Builder.newInstance()/*...*/.build(),
        PropertiesDefinition.Builder.newInstance()
            .baseAttackModifier(14.0F)
            .defaultSwordType(List.of(SwordType.BEWITCHED))
            .maxDamage(1561)
            .build(),
        FantasyDefinition.Builder.newInstance()
            .specialType("crucible")
            .specialAttackEffect("eternity")
            .build(),
        List.of(/*附魔*/)));
// 注意：没有任何 .slashArtsType() 调用！
```

### 默认 SA 行为

由于没有绑定自定义 SA，Crucible 使用 SlashBlade 的 **默认 SA 系统**：

- **默认左键连段**：SlashBlade 基础三连斩（平砍）
- **默认右键 SA**：无（因为 SA 槽位为空）
- **默认 SB（Slash Blade）技能**：无特殊技能

这意味着 Crucible 的玩家必须依赖 **纯平砍输出**，没有任何花哨的技能释放。

### FDCombo 中是否有关联？

在 [`FDCombo.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java) 中搜索 `crucible` 关键词——**结果为空**。所有已注册的连段状态（ComboState）均分配给以下武器：

| 武器           | 连段状态                                         |
| -------------- | ------------------------------------------------ |
| Chikeflare     | `WING_TO_THE_FUTURE`, `CHIKE_FLARE_CONVERT`, ... |
| Crimson Scythe | `CRIMSON_STRIKE`, `CRIMSON_STRIKE_0~2`, ...      |
| Twin Blade     | `TWIN_MODE`, `MOOD_SLASH`, `DOOM_SLASH`, ...     |
| Starless Night | `ECHOING_VOID`, ...                              |
| Over Cold      | `FREEZE_ZERO`, ...                               |
| Smart Pistol   | `CHARGE_SHOT`, `OVER_CHARGE`, ...                |
| **Crucible**   | **（无任何连段注册）**                           |

**结论：** Crucible 完全使用 SlashBlade 原版的默认连段系统，没有任何自定义的攻击动画或连击派生。

---

## 四、维度三~四：连段与衍生效果

由于 Crucible 没有绑定任何自定义 ComboState，其连段行为完全取决于 **SlashBlade 原版系统**：

### 默认连段

1. **地面平砍**：标准的三连斩击动画
2. **空中攻击**：标准空中斩击
3. **疾跑攻击**：标准冲刺斩
4. **格挡**：标准 SlashBlade 格挡（右键）

没有任何特殊派生、特殊按下、或终结技。

### 衍生效果

没有自定义连段意味着也没有任何连段触发的衍生效果（如召唤幻影剑、发射弹射物、范围爆炸等）。Crucible 的所有输出都依赖于 **每次普通攻击的伤害数值本身**。

---

## 五、维度五：特殊效果分析

### ❌ 无自定义 SE

Crucible 的注册代码中 **没有调用 `.addSpecialEffect()` 方法**，意味着没有任何自定义 Special Effect 绑定到这把刀上。

### ⚠️ 核心机制：「永劫」（Eternity）伤害类型

虽然 Crucible 没有传统意义上的"特殊效果"，但它拥有整个模组中最具特色的伤害类型——**eternity**。这不是 SE，而是通过 [`FantasyDefinition.specialAttackEffect("eternity")`](src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:450) 绑定的 **特殊攻击伤害类型**。

#### 伤害处理流程

整个处理流程在 [`DamageConverterEvent.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/globalevent/DamageConverterEvent.java) 中实现，分三个阶段：

##### 阶段一：伤害替换（DoSlashEvent）

```java
// DamageConverterEvent.java:49-69
@SubscribeEvent(priority = EventPriority.LOWEST)
public static void OnSlash(SlashBladeEvent.DoSlashEvent event) {
    // 检测到 FantasySlashBlade 并且有特殊攻击效果
    String fdDamageType = fdState.getSpecialAttackEffect();
    if (fdDamageType != null && !fdDamageType.equals("Null")) {
        // 创建 eternity 类型的 DamageSource
        DamageSource fds = FDDamageSource.getEntityDamageSource(
            livingEntity.level(),
            FDDamageSource.fromString(fdDamageType),  // -> "eternity"
            livingEntity);
        // 用 eternity 伤害源重新执行范围攻击
        FDAttackManager.areaAttackWithSource(..., fds);
        // 将原始伤害置为 0（因为 SlashEffect 不兼容自定义伤害类型）
        event.setDamage(0d);
    }
}
```

**关键点：** SlashBlade 原版的斩击效果（SlashEffect）不兼容自定义伤害类型。因此 Crucible 的伤害流程是：

1. 原版 SlashBlade 触发斩击事件
2. 将原始伤害归零
3. 用 `eternity` 伤害类型 **重新计算并施加伤害**

##### 阶段二：伤害造成时（LivingHurtEvent）

Crucible 的 **eternity** 在 `LivingHurtEvent` 阶段没有特殊处理（和其他伤害类型如 resolution、wrath 等不同，eternity 的额外效果在 `LivingDamageEvent` 阶段处理）。

##### 阶段三：伤害结算后（LivingDamageEvent）—— 生命上限削减

```java
// DamageConverterEvent.java:184-202
if (source.is(FDDamageSource.ETERNITY)) {
    float reduce = amount * 0.1f;  // 伤害的10%
    AttributeInstance maxHealth = target.getAttribute(Attributes.MAX_HEALTH);
    if (maxHealth != null) {
        // 获取旧的 modifier（可叠加）
        AttributeModifier old = maxHealth.getModifier(ETERNITY_HEALTH_MODIFIER);
        double totalReduce = -reduce;
        if (old != null) {
            totalReduce += old.getAmount();  // 累加效果
            maxHealth.removeModifier(old);
        }
        // 应用永久生命上限削减
        AttributeModifier mod = new AttributeModifier(
            ETERNITY_HEALTH_MODIFIER,
            "eternity_reduce",
            totalReduce,
            AttributeModifier.Operation.ADDITION);
        maxHealth.addPermanentModifier(mod);
    }
}
```

**核心效果：** 每次造成 eternity 伤害时，**目标的生命值上限永久降低该次伤害的 10%**。

| 攻击次数 | 单次伤害  | 累计削减上限 | 目标剩余上限（初始40❤️） |
| -------- | --------- | ------------ | ------------------------ |
| 1        | 15.0      | -1.5         | 38.5 ❤️                  |
| 5        | 15.0 × 5  | -7.5         | 32.5 ❤️                  |
| 10       | 15.0 × 10 | -15.0        | 25.0 ❤️                  |
| 20       | 15.0 × 20 | -30.0        | 10.0 ❤️                  |
| 27       | 15.0 × 27 | -40.5        | ❌ 死亡                  |

对于拥有 40 点生命值（20颗心）的玩家：

- 需要约 27 次有效攻击才能将对方上限削减至 0 以下

对于拥有 100 点生命值（50颗心）的 BOSS：

- 需要约 67 次有效攻击

#### 伤害类型标签

根据 [`bypasses_armor.json`](src/generated/resources/data/minecraft/tags/damage_type/bypasses_armor.json) 和 [`bypasses_invulnerability.json`](src/generated/resources/data/minecraft/tags/damage_type/bypasses_invulnerability.json)：

```json
{
  "values": ["fantasydesire:omega", "fantasydesire:eternity"]
}
```

```json
{
  "values": ["fantasydesire:eternity"]
}
```

eternity 伤害类型拥有以下特性：

| 标签                       | 含义             | 生效                |
| -------------------------- | ---------------- | ------------------- |
| `bypasses_armor`           | **无视护甲**     | ✅ 护甲值不减少伤害 |
| `bypasses_invulnerability` | **无视伤害免疫** | ✅ 可穿透伤害免疫帧 |

> ⚠️ **注意：** eternity **没有** `bypasses_resistance` 标签，因此抗性提升（Resistance）效果仍然可以减少伤害。

#### 语言键

```json
"tooltip.fantasydesire.AttackEffect.eternity": "§4永劫",
"tooltip.fantasydesire.AttackEffect.eternity.desc": "无视护甲 伤害的10%将削减生命值上限"
```

#### eternity 伤害类型定义

```json
{
  "exhaustion": 0.0,
  "message_id": "fantasydesire.eternity",
  "scaling": "when_caused_by_living_non_player"
}
```

- ** exhaustion（ exhaustion）**：0.0，不消耗玩家的饱和度
- **scaling**：`when_caused_by_living_non_player`，伤害随难度缩放（仅在非玩家生物造成时）

#### 清除机制

每次攻击叠加的 `eternity_reduce` modifier 在以下情况下会被清除：

| 事件         | 处理方式                                    |
| ------------ | ------------------------------------------- |
| 目标死亡     | `LivingDeathEvent` → `clearEternity()`      |
| 实体进入世界 | `EntityJoinLevelEvent` → `clearEternity()`  |
| 玩家上床睡觉 | `PlayerSleepInBedEvent` → `clearEternity()` |

这意味着 **eternity 的生命上限削减是战斗内永久性的，但会在目标死亡或重生后重置**。

---

## 六、合成配方分析

根据 [`crucible.json`](src/generated/resources/data/fantasydesire/recipes/crucible.json)：

```
配方：
  [ ][M][L]
  [M][N][M]
  [B][M][ ]

材料：
  B = 拔刀剑（需求：下界岩台座、666击杀、6666耀魂）
  M = 岩浆块
  N = 下界之星
  L = 金锭
```

### 材料成本分析

| 材料                                 | 数量 | 获取难度                        |
| ------------------------------------ | ---- | ------------------------------- |
| 下界岩台座拔刀剑（666击杀/6666耀魂） | 1    | ⭐⭐⭐ 极高（需要大量刷怪积累） |
| 岩浆块                               | 5    | ⭐ 低（下界大量生成）           |
| 下界之星                             | 1    | ⭐⭐⭐（需要击杀凋灵）          |
| 金锭                                 | 1    | ⭐ 低                           |

**合成门槛评价：** 主要门槛在于需要一把已经积累了一定击杀和耀魂的下界岩台座拔刀剑（666击杀、6666耀魂）。这意味着玩家需要先使用一把下界岩台座刀进行大量战斗积累，才能制作 Crucible。

---

## 七、综合玩法流派分析

### 核心定位：纯物理面板怪 · 平砍压制流

```
┌─────────────────────────────────────────────────┐
│                 裁决剑 · Crucible                  │
│                                                   │
│  15.0攻击力 + SMITE X + 永恒生命削减                │
│                                                   │
│  ┌───────────┐    ┌───────────────────┐           │
│  │ 高额基础   │    │  永恒伤害叠加机制   │           │
│  │  攻击力    │    │  ❤️→❤️→❤️→🖤→💀      │           │
│  └─────┬─────┘    └─────────┬─────────┘           │
│        ↓                    ↓                      │
│  ┌─────────────────────────────────────┐           │
│  │     无视护甲的永久生命上限削减          │           │
│  │  每刀削减10%伤害量的最大生命值          │           │
│  └─────────────────────────────────────┘           │
│                                                   │
│  ⚠️ 没有任何SA技能                               │
│  ⚠️ 没有任何SE效果                                │
│  ⚠️ 没有任何自定义连段                             │
└─────────────────────────────────────────────────┘
```

### 优势分析

1. **单发伤害极高**（14.0基础 + SMITE X对亡灵额外25）
   - 对亡灵生物：约 40 点伤害/刀，秒杀一切亡灵杂兵
   - 对非亡灵生物：约 15 点伤害/刀，3-4刀击杀大多数怪物

2. **永劫（eternity）效果的压制力**
   - 无视护甲 → 对高护甲目标（铁傀儡、穿着装备的玩家）仍然造成全额伤害
   - 生命上限削减 → 战斗拖得越久，敌人越弱
   - 特别适合对付高血量 BOSS（如凋灵、末影龙 MOD 的 BOSS）

3. **极高的续航能力**
   - UNBREAKING V + MENDING I → 几乎不会损坏
   - 不需要消耗任何特殊资源（如 SA 的 SE 点数、弹药等）

4. **上手简单**
   - 没有复杂的连段系统
   - 没有需要记忆的技能组合
   - "拿起就砍"——纯粹的战斗体验

### 劣势分析

1. **缺乏AOE能力**：默认平砍一次只能攻击一个目标
2. **缺乏机动性**：没有突进技能或位移
3. **缺乏远程手段**：无法攻击远距离目标
4. **被 Resistance 克制**：eternity 没有 bypasses_resistance 标签
5. **对亡灵生物特化**：SMITE X 对非亡灵生物无效

### 与其他武器的对比

| 武器           | 单发伤害   | AOE        | 机动性     | 远程       | 操作难度   | 持续作战   |
| -------------- | ---------- | ---------- | ---------- | ---------- | ---------- | ---------- |
| **Crucible**   | ⭐⭐⭐⭐⭐ | ⭐         | ⭐⭐       | ⭐         | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Crimson Scythe | ⭐⭐⭐⭐   | ⭐⭐⭐⭐⭐ | ⭐⭐⭐     | ⭐⭐⭐     | ⭐⭐⭐     | ⭐⭐⭐     |
| Smart Pistol   | ⭐⭐⭐     | ⭐⭐⭐     | ⭐⭐⭐⭐   | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐   | ⭐⭐       |
| Twin Blade     | ⭐⭐⭐     | ⭐⭐⭐⭐   | ⭐⭐⭐⭐⭐ | ⭐⭐       | ⭐⭐       | ⭐⭐⭐     |
| Starless Night | ⭐⭐⭐⭐   | ⭐⭐⭐⭐   | ⭐⭐⭐     | ⭐⭐⭐⭐   | ⭐⭐⭐     | ⭐⭐⭐⭐   |
| Chikeflare     | ⭐⭐⭐⭐   | ⭐⭐⭐⭐   | ⭐⭐⭐⭐⭐ | ⭐⭐⭐     | ⭐⭐⭐⭐   | ⭐⭐⭐     |

### 推荐使用场景

| 场景                  | 评价          | 理由                                                         |
| --------------------- | ------------- | ------------------------------------------------------------ |
| **亡灵生物集群清理**  | 🏆 **最优**   | SMITE X 一刀一个，配合 eternity 削减上限                     |
| **BOSS 战（单体）**   | ✅ **推荐**   | eternity 的叠加削减效果在长线战斗中极为恐怖                  |
| **PvP**               | ✅ **强力**   | 无视护甲 + 生命上限削减，对穿戴钻石/下界合金甲的玩家极为致命 |
| **亡灵 BOSS（凋灵）** | 🏆 **最优解** | SMITE X 对凋灵有效 + eternity 叠加削减凋灵血量上限           |
| **下界探索**          | ✅ **推荐**   | 下界绝大多数怪物为亡灵（僵尸猪灵、凋灵骷髅等）               |
| **远程战斗需求**      | ❌ **不推荐** | 无任何远程手段                                               |
| **大规模混战**        | ⚠️ **一般**   | 缺乏 AOE，需要逐个击破                                       |

---

## 八、总结评价

```
┌────────────────────────────────────────────────────────────┐
│                   裁决剑 Crucible 评分                        │
├──────────────┬─────────────────────────────────────────────┤
│  基础攻击     │ ⭐⭐⭐⭐⭐ (14.0F - 全模组最高)                  │
│  附魔配置     │ ⭐⭐⭐⭐⭐ (SMITE X + UNB V + Mending)          │
│  SA 技能      │ ⭐ (无自定义SA)                              │
│  自定义连段   │ ⭐ (无自定义连段)                              │
│  特殊效果     │ ⭐⭐⭐⭐ (eternity - 独特机制但需叠层)            │
│  合成门槛     │ ⭐⭐⭐ (需要大量积累)                           │
│  上手难度     │ ⭐⭐⭐⭐⭐ (极低 - 纯粹的平砍)                   │
│  上限潜力     │ ⭐⭐⭐⭐ (对亡灵/高甲目标表现出色)                │
│  泛用性       │ ⭐⭐⭐ (亡灵场景优秀，其他场景一般)               │
├──────────────┴─────────────────────────────────────────────┤
│  一句话总结：全模组最暴力的平砍面板，无任何花哨机制，             │
│  但每刀都在削减敌人的生命上限，是纯粹力量的化身。                │
└────────────────────────────────────────────────────────────┘
```

**Crucible 是一把"极端纯粹"的武器**——它放弃了所有复杂的技能系统（SA、SE、自定义连段），将一切资源全部投入到基础面板和独特的伤害类型上。每一次攻击都朴实无华，但每一次攻击都在 **永久性削弱敌人**。它不需要花哨的连招，不需要华丽的特效，只需要你一刀一刀地砍下去，敌人就会越来越弱，直到死亡。

**适合玩家类型：** 喜欢简单粗暴、不喜欢复杂操作的玩家；偏好亡灵生物场景的玩家；追求极致单发伤害的玩家。

**不适合玩家类型：** 喜欢华丽技能特效的玩家；偏好远程或AOE战斗风格的玩家；喜欢复杂连段系统的玩家。
