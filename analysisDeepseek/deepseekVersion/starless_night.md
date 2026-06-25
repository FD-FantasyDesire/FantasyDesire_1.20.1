# StarlessNight（无星之夜 / 回荡虚空）深度分析

> **分析版本：** DeepSeek v4  
> **最后更新：** 2025-06-23  
> **所属Mod：** FantasyDesire（SlashBlade Resharped 附属）

---

## 目录

1. [概述与定位](#1-概述与定位)
2. [维度一：基础属性](#2-维度一基础属性)
3. [维度二：SA - EchoingVoid（回荡虚空）](#3-维度二sa---echoingvoid回荡虚空)
4. [维度三：连段系统详解](#4-维度三连段系统详解)
5. [维度四：特殊效果系统](#5-维度四特殊效果系统)
6. [维度五：完整机制链与数据流](#6-维度五完整机制链与数据流)
7. [玩法流派总结](#7-玩法流派总结)
8. [联动设计分析](#8-联动设计分析)

---

## 1. 概述与定位

StarlessNight（无星之夜）是一柄以 **"虚空印记叠加 → 回响伤害存储 → 连锁引爆"** 为循环核心的拔刀剑。它不追求单次爆发的极致数值，而是通过 **延迟伤害（ECHO Damage）** 与 **群体系数分摊** 的机制，在持续战斗中累积优势，最终以连锁反应形式对群体目标造成毁灭性打击。

**设计哲学：** 伏击型、延迟引爆型、群体压制型  
**上手难度：** ⭐⭐⭐⭐（需要对延迟伤害机制与连段时机有深刻理解）  
**核心关键词：** `ECHO`（回响）、`VoidStrike`（虚空印记）、`EchoingStrike`（回响连锁）、`Detonate`（引爆）

---

## 2. 维度一：基础属性

### 2.1 注册信息

| 属性         | 值                                  | 来源                                                                                                                                                          |
| ------------ | ----------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **注册名**   | `starless_night`                    | [`FantasySlashBladeBuiltInRegistry.java:398`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:398) |
| **本地化键** | `item.fantasydesire.starless_night` | [`EchoingVoid.java:16`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/EchoingVoid.java:16)                                                |

### 2.2 渲染与模型

| 属性               | 值                             | 来源                                                                                                                                                          |
| ------------------ | ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **特效色**         | `0x8000ff`（暗紫色）           | [`FantasySlashBladeBuiltInRegistry.java:401`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:401) |
| **模型文件**       | `models/sn.obj`                | [`FantasySlashBladeBuiltInRegistry.java:403`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:403) |
| **贴图文件**       | `models/sn.png`                | [`FantasySlashBladeBuiltInRegistry.java:402`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:402) |
| **携带方式**       | `RNINJA`（忍者背持）           | [`FantasySlashBladeBuiltInRegistry.java:404`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:404) |
| **连段中切换模型** | `models/sn_huge.obj`（巨大化） | [`FDCombo.java:533`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:533)                                                           |

> **🔍 注意：** 连段进入 [`ECHOING_VOID_0`](FDCombo.java:524) 阶段时，模型会从 `sn.obj` 切换为 `sn_huge.obj`（巨大化）。连段结束时由 [`StarlessNightEffects.onBladeMotion()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/starlessnight/StarlessNightEffects.java:86) 自动恢复为 `sn.obj`。

### 2.3 战斗属性

| 属性             | 值              | 来源                                                                                                                                                          |
| ---------------- | --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **基础攻击**     | `9.0F`          | [`FantasySlashBladeBuiltInRegistry.java:407`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:407) |
| **MaxDamage**    | `1561`          | [`FantasySlashBladeBuiltInRegistry.java:410`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:410) |
| **剑类型**       | `BEWITCHED`     | [`FantasySlashBladeBuiltInRegistry.java:408`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:408) |
| **特殊类型**     | `StarlessNight` | [`FantasySlashBladeBuiltInRegistry.java:419`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:419) |
| **特殊攻击效果** | `echo`          | [`FantasySlashBladeBuiltInRegistry.java:420`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:420) |
| **SpecialLore**  | `1`             | [`FantasySlashBladeBuiltInRegistry.java:418`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:418) |

### 2.4 附魔

| 附魔                      | 等级 |
| ------------------------- | ---- |
| `UNBREAKING`（耐久）      | X    |
| `SHARPNESS`（锋利）       | V    |
| `POWER_ARROWS`（力量/弓） | III  |
| `MENDING`（经验修复）     | I    |

> **来源：** [`FantasySlashBladeBuiltInRegistry.java:422-431`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:422-431)

### 2.5 注册的SA与SE

| 类型   | 注册名                      | 类引用                                                                                                                                    | SP消耗 |
| ------ | --------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- | ------ |
| **SA** | `ECHOING_VOID`              | [`FDSlashArtRegistry.java:26-27`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSlashArtRegistry.java:26-27)             | 4      |
| **SE** | `VoidStrike`（虚空打击）    | [`FDSpecialEffectsRegistry.java:57-58`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSpecialEffectsRegistry.java:57-58) | 100    |
| **SE** | `EchoingStrike`（回响打击） | [`FDSpecialEffectsRegistry.java:59-60`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSpecialEffectsRegistry.java:59-60) | 30     |

---

## 3. 维度二：SA - EchoingVoid（回荡虚空）

### 3.1 注册结构

```
FDSlashArtRegistry.ECHOING_VOID
  └─ new FDSlashArts((e) -> FDCombo.ECHOING_VOID.getId(), 4)
       ├─ 消耗SP: 4
       └─ 连段ID: fantasydesire:echoing_void
```

**来源：** [`FDSlashArtRegistry.java:26-27`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSlashArtRegistry.java:26-27)

### 3.2 `EchoingVoid` 工具类

**文件：** [`EchoingVoid.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/EchoingVoid.java)

#### `AntiNTR()` 方法

```java
public static boolean AntiNTR(LivingEntity entity) {
    return CapabilityUtils.SEConditionMatcher.of(entity)
            .requireTranslation("item.fantasydesire.starless_night")
            .match() != null;
}
```

- 检测手持物品是否包含 StarlessNight 的 translation key
- 用于连段触发前的 **归属检测**，防止非 StarlessNight 持有者错误触发连段
- **重要：** 当检测失败时，连段会回退到 `SlashBlade.prefix("none")`（即普通攻击动作）

#### `doEnderSlash()` 方法

```java
public static EntitySlashEffect doEnderSlash(
    LivingEntity playerIn,       // 使用者
    float roll,                  // 旋转角
    float YRot, float XRot,      // 方向角
    int colorCode,               // 特效色
    float rotationOffset,        // 旋转偏移
    Vec3 centerOffset,           // 中心偏移
    boolean mute,                // 静音
    boolean critical,            // 暴击
    double damage,               // 伤害基数
    KnockBacks knockback,        // 击退类型
    float scale,                 // 大小缩放
    int lifetime                 // 存在时间（tick）
)
```

**核心逻辑：**

1. **位置计算：** 以玩家眼部高度 75% 处为起点，沿视线方向偏移 0.3 格
2. **创建实体：** 生成 [`EntityEnderSlashEffect`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/entity/EntityEnderSlashEffect.java) 实体
3. **Rank继承：** 从玩家集中等级（ConcentrationRank）Capability 获取当前 Rank 并注入到实体
4. **伤害类型：** 固定为 [`FDDamageSource.ECHO`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/damagesource/FDDamageSource.java:35)（即回响伤害）

### 3.3 `EntityEnderSlashEffect` 实体

**文件：** [`EntityEnderSlashEffect.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/entity/EntityEnderSlashEffect.java)

```
EntityFDSlashEffect (extends) ──> EntitySlashEffect (SlashBlade核心)
      ↑
EntityEnderSlashEffect
```

**攻击逻辑 (`handleAttack()`):**

```java
@Override
protected void handleAttack() {
    if (this.tickCount % 2 == 0) {
        // 每2 tick攻击一次
        List<Entity> hits;
        if (!getIndirect() && getShooter() instanceof LivingEntity shooter) {
            float ratio = (float) getDamage() * (getIsCritical() ? 1.1f : 1.0f);
            hits = FDAttackManager.areaAttack(
                shooter, this.getAction().action, this.position(),
                4.0 * this.getScale(), ratio,
                forceHit, false, true,
                getAlreadyHits(),
                FDDamageSource.getEntityDamageSource(
                    shooter.level(), FDDamageSource.ECHO, shooter
                )
            );
        }
    }
}
```

**关键特性：**
| 特性 | 说明 |
|------|------|
| **攻击频率** | 每 2 tick 一次（即每秒 10 次判定） |
| **范围** | `4.0 * scale` 半径 |
| **伤害类型** | 固定 `FDDamageSource.ECHO`（回响） |
| **暴击倍率** | 暴击时 `* 1.1` |
| **击中冷却** | `!doCycleHit()` 时加入已命中列表防止重复击中 |
| **穿透实体** | 可以穿透多个目标（`true`），不可穿透方块 |

### 3.4 SA的触发方式（DamageConverterEvent 回调）

StarlessNight 的 `specialAttackEffect` 被设为 `"echo"`。

在 [`DamageConverterEvent.OnSlash()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/globalevent/DamageConverterEvent.java:50) 中：

```java
String fdDamageType = fdState.getSpecialAttackEffect();
if (fdDamageType != null && !fdDamageType.equals("Null")) {
    DamageSource fds = FDDamageSource.getEntityDamageSource(
        livingEntity.level(),
        FDDamageSource.fromString(fdDamageType),  // -> "echo" -> ECHO
        livingEntity
    );
    FDAttackManager.areaAttackWithSource(..., fds);
    event.setDamage(0d);  // 原始伤害归零
}
```

> **🔍 这意味着：** 所有连段中通过 `doSlash` 造成的伤害都会被替换为 ECHO 伤害类型（原始的 SlashBlade 硬编码物理伤害被置零）。

---

## 4. 维度三：连段系统详解

### 4.1 连段状态机总览

```
ECHOING_VOID (触发检测)
    │ AntiNTR成功
    ▼
ECHOING_VOID_0 (蓄力·虚空凝聚)  ← 模型切换 sn_huge.obj
    │ 超时/完成
    ▼
ECHOING_VOID_1 (终结一击·虚空印记)  ← 10层VoidStrike
    │ 超时/完成
    ▼
ECHOING_VOID_2 (虚空横斩x4)  ← 4次EnderSlash
    │ 超时/完成
    ▼
ECHOING_VOID_END (收刀)
    │ 超时
    ▼
ECHOING_VOID_END2 (范围引爆)  ← 40m内全员引爆
    │
    ▼
none (结束)
```

**来源：** [`FDCombo.java:513-631`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:513-631)

### 4.2 各阶段详细分析

#### 阶段0：`ECHOING_VOID` — 触发检测

| 属性         | 值                            |
| ------------ | ----------------------------- |
| **帧范围**   | 0 ~ 1                         |
| **优先级**   | 50                            |
| **动作定位** | `ExMotionLocation`            |
| **安全检测** | `EchoingVoid.AntiNTR(entity)` |
| **成功转跳** | `echoing_void_0`              |
| **失败转跳** | `none`（普通攻击）            |

#### 阶段1：`ECHOING_VOID_0` — 蓄力·虚空凝聚

| 属性         | 值               |
| ------------ | ---------------- |
| **帧范围**   | 1 ~ 33           |
| **优先级**   | 50               |
| **动作定位** | `testLocation`   |
| **超时转跳** | `echoing_void_1` |

**第30帧执行：**

1. **模型切换：** `ItemUtils.ConvertModel(blade, "models/sn_huge.obj")` — 剑体瞬间巨大化
2. **音效：** `SoundEvents.TRIDENT_THUNDER`（三叉戟雷声）
3. **粒子特效：** 16 道紫色（`0x8000FF`）闪电粒子向全方位随机方向射出，长度 16 格

> **设计意图：** 蓄力阶段通过视觉上剑体巨大化与漫天紫色闪电，营造出虚空能量在剑身凝聚的压迫感。

#### 阶段2：`ECHOING_VOID_1` — 终结一击·虚空印记

| 属性         | 值               |
| ------------ | ---------------- |
| **帧范围**   | 200 ~ 218        |
| **优先级**   | 50               |
| **超时转跳** | `echoing_void_2` |

**第6帧执行 — `doAddonFDSlash`：**

```java
AddonSlashUtils.doAddonFDSlash(
    entityIn,
    180 - 42,          // roll
    entityIn.getYRot(), // YRot
    0,                 // XRot
    0x8000FF,          // colorCode (紫色)
    0,                 // rotationOffset
    Vec3.ZERO,         // centerOffset
    false, false,      // mute, critical
    0.1f,              // damage (0.1倍! 极小)
    KnockBacks.cancel, // knockback
    10f,               // scale (巨大)
    10,                // lifetime
    FDDamageSource.ECHO.location().toString()  // 伤害类型: "fantasydesire:echo"
)
```

> **🔍 关键：** 此阶段伤害只有 0.1f（极小），但通过设置 `damageType` 为 `ECHO`，触发 [`DamageConverterEvent.OnHurt()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/globalevent/DamageConverterEvent.java:150)，将伤害存储到目标身上（具体机理见 §6.2）。

**命中效果 — 叠加10层虚空印记：**

```java
.addHitEffect((target, attacker) -> {
    StarlessNightEffects.stackVoidStrike(target, 10);
})
```

> 这是 VoidStrike 的主要叠加方式，一次命中直接叠 **10 层**。

#### 阶段3：`ECHOING_VOID_2` — 虚空横斩x4

| 属性         | 值                 |
| ------------ | ------------------ |
| **帧范围**   | 725 ~ 743          |
| **优先级**   | 50                 |
| **超时转跳** | `echoing_void_end` |

**4次EnderSlash依次发射：**

| 帧  | YRot偏移    | 伤害 | scale | lifetime | 说明   |
| --- | ----------- | ---- | ----- | -------- | ------ |
| 4   | `+180`      | 3f   | 10f   | 10       | 身后斩 |
| 5   | `+270`      | 5f   | 10f   | 10       | 左侧斩 |
| 6   | `+360` (0)  | 7f   | 10f   | 10       | 前方斩 |
| 7   | `+450` (90) | 9f   | 10f   | 10       | 右侧斩 |

> **🔍 伤害递增：** 3f → 5f → 7f → 9f，叠加虚空印记层数后，ECHO 伤害将显著放大。
> **攻击范围：** 每刀 `4.0 * 10 = 40` 格半径，覆盖极广。

#### 阶段4：`ECHOING_VOID_END` — 收刀

| 属性         | 值                  |
| ------------ | ------------------- |
| **帧范围**   | 743 ~ 764           |
| **优先级**   | 50                  |
| **动作定位** | `ExMotionLocation`  |
| **超时转跳** | `echoing_void_end2` |

#### 阶段5：`ECHOING_VOID_END2` — 范围引爆

| 属性         | 值                 |
| ------------ | ------------------ |
| **帧范围**   | 764 ~ 787          |
| **优先级**   | 50                 |
| **速度**     | 0.5f（慢放）       |
| **动作定位** | `ExMotionLocation` |
| **超时转跳** | `none`             |

**第1帧执行 — 40m内全员范围引爆：**

```java
.put(1, (entityIn) -> {
    List<LivingEntity> targets = FDTargetSelector.getLivingEntitiesInRadius(
        entityIn, entityIn.position(), 40.0, false, null
    );
    for (LivingEntity target : targets) {
        EchoDamageHelper.detonateArea(target, 10.0);
    }
})
```

> **这是整个连段的爆发终点。** 对 40m 内的每个目标（不分敌我），以 10m 半径执行范围引爆（`detonateArea`），将存储的回响伤害均分给范围内的所有敌人。

---

## 5. 维度四：特殊效果系统

### 5.1 StarlessNightEffects — 事件监听中枢

**文件：** [`StarlessNightEffects.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/starlessnight/StarlessNightEffects.java)

该类监听三个事件：

| 事件                                                                                                                                            | 优先级    | 触发时机     | 功能                          |
| ----------------------------------------------------------------------------------------------------------------------------------------------- | --------- | ------------ | ----------------------------- |
| [`HitEvent`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/starlessnight/StarlessNightEffects.java:37)         | 正常      | 普通攻击命中 | 叠加1层 VoidStrike            |
| [`LivingHurtEvent`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/starlessnight/StarlessNightEffects.java:59)  | `LOWEST`  | ECHO伤害造成 | 触发 EchoingStrike 连锁       |
| [`BladeMotionEvent`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/starlessnight/StarlessNightEffects.java:86) | `HIGHEST` | 连段状态切换 | 非 EchoingVoid 连段时恢复模型 |

### 5.2 VoidStrike（虚空打击）

#### 注册信息

| 属性               | 值                                                                                                               |
| ------------------ | ---------------------------------------------------------------------------------------------------------------- |
| **SE注册名**       | `void_strike`                                                                                                    |
| **SP消耗**         | 100                                                                                                              |
| **药水效果注册名** | `void_strike`                                                                                                    |
| **效果类**         | [`VoidStrikeEffect`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/potioneffect/VoidStrikeEffect.java) |
| **效果颜色**       | `0x5500AA`（紫黑色）                                                                                             |
| **HUD图标**        | `textures/mob_effect/void_strike.png`                                                                            |

**来源：** [`FDSpecialEffectsRegistry.java:57-58`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSpecialEffectsRegistry.java:57-58), [`FDPotionEffects.java:15-16`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDPotionEffects.java:15-16)

#### 层数叠加机制

```java
public static void stackVoidStrike(LivingEntity entity, int stacks) {
    MobEffect voidStrike = FDPotionEffects.VOID_STRIKE.get();
    MobEffectInstance current = entity.getEffect(voidStrike);
    int duration = 200;  // 10秒
    int amplifier = stacks - 1;
    if (current != null) {
        amplifier = current.getAmplifier() + stacks;  // 累加
    }
    amplifier = Math.min(amplifier, 49);  // 上限50层 (amplifier 0-49)
    entity.forceAddEffect(new MobEffectInstance(voidStrike, duration, amplifier), null);
}
```

| 叠加方式            | 层数 | 触发条件        |
| ------------------- | ---- | --------------- |
| 普通攻击命中        | +1   | 每次 HitEvent   |
| ECHOING_VOID_1 命中 | +10  | SA阶段2命中效果 |

> **🔍 上限 50 层**（amplifier 49，层数 = amplifier + 1）。

#### 层数读取

```java
public static int getVoidStrikeLayers(LivingEntity entity) {
    MobEffectInstance current = entity.getEffect(VOID_STRIKE.get());
    if (current != null) {
        return Math.min(50, current.getAmplifier() + 1);
    }
    return 0;
}
```

#### 粒子效果（`applyEffectTick`）

每 20 tick 触发一次，根据层数（amplifier）生成 **螺旋星云状粒子**：

- 3条旋臂（arms）
- 每臂粒子数 = `min(15 + amplifier, 30)`
- 粒子类型：`DustColorTransitionOptions`（紫→黑渐变色）
- 旋转半径：`0.2f + t * 1.5f`（从内向外扩散）
- 随机轴向倾角（`tiltX/Y/Z`），产生三维立体效果

#### 增伤效果

在 [`DamageConverterEvent.OnDamage()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/globalevent/DamageConverterEvent.java:167) 中：

```java
int voidStrikeLayers = VoidStrikeEffect.getVoidStrikeLayers(target);
if (voidStrikeLayers > 0) {
    amount *= 1.0f + voidStrikeLayers * 0.1f;  // 每层+10%
}
```

> **示例：** 50层 → `1.0 + 50 * 0.1 = 6.0` 倍伤害！

### 5.3 EchoingStrike（回响打击）

#### 注册信息

| 属性         | 值               |
| ------------ | ---------------- |
| **SE注册名** | `echoing_strike` |
| **SP消耗**   | 30               |
| **触发概率** | 25%              |

**来源：** [`FDSpecialEffectsRegistry.java:59-60`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSpecialEffectsRegistry.java:59-60)

#### 执行流程

1. **触发条件：** ECHO伤害命中目标时，25%概率激活
2. **搜索范围：** 主目标 16m 半径内的所有活体生物
3. **排序策略：** 按 VoidStrike 层数 **升序**排列（优先选择印记层数低的目标）
4. **连锁次数：** 最多 **3次** 连锁

```
主目标 ──→ 目标A（VoidStrike最低）──→ 目标B ──→ 目标C
    [0.1倍ECHO伤害]   [0.1倍]      [0.1倍]
```

**每次连锁的伤害计算：**

```java
DamageSource damageSource = player.damageSources().magic();
nextTarget.hurt(damageSource, baseDamage * 0.1f);  // 主伤害的10%
```

**视觉特效：**

- 紫色发光线条（`GlowingLineParticleOptions`）连接前一个目标和当前目标
- 沿连线方向每 2 格生成一个扩散环粒子（`SpreadingRingParticleOptions`）
- 环的 lifetime 随距离递增（`baseLifetime + i * 5`）

**额外效果：** 每次连锁对目标叠加 1 层 VoidStrike。

### 5.4 ECHO伤害处理系统

#### ECHO伤害类型

**注册：** [`FDDamageSource.java:35`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/damagesource/FDDamageSource.java:35)

```java
public static final ResourceKey<DamageType> ECHO = register("echo");
```

**JSON定义：** [`echo.json`](../../src/generated/resources/data/fantasydesire/damage_type/echo.json)

```json
{
  "exhaustion": 0.0,
  "message_id": "fantasydesire.echo",
  "scaling": "when_caused_by_living_non_player"
}
```

- 无疲劳消耗（`exhaustion: 0.0`）
- 仅当由非玩家生物造成时缩放伤害

#### ECHO伤害的存储与延迟机制

**位置：** [`DamageConverterEvent.OnHurt()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/globalevent/DamageConverterEvent.java:150-161)

```java
if (source.is(FDDamageSource.ECHO)) {
    // 1. 赋予回响计时器（3秒后自动引爆）
    target.forceAddEffect(
        new MobEffectInstance(FDPotionEffects.ECHO_TIMER.get(), 60, 0, false, false, false),
        attacker
    );

    // 2. 存储伤害（扣除0.1f保留为即时伤害）
    if (amount > 0.1f) {
        float storeAmount = amount - 0.1f;
        target.getCapability(EchoDamageProvider.ECHO_DAMAGE)
            .ifPresent(cap -> cap.addDamage(attacker.getUUID(), storeAmount));
        amount = 0.1f;  // 只造成0.1f即时伤害
    }
}
```

> **核心机制：** ECHO伤害的 99%+ 被"存储"到目标身上（按攻击者UUID分类），只造成 0.1f 的即时伤害（用于触发命中效果）。真正的伤害在引爆时释放。

#### 伤害存储体系架构

```
每个LivingEntity
    ├── EchoDamageCap (Capability)
    │     └── HashMap<UUID, Float>
    │           ├── attackerA_UUID -> 15.3f
    │           ├── attackerB_UUID -> 42.0f
    │           └── ...
    │
    └── PersistentData (NBT持久化)
          └── "FDEchoDamageData"
                ├── attackerA_UUID: 15.3
                ├── attackerB_UUID: 42.0
                └── ...
```

| 组件                    | 文件                                                                                                                          | 功能                             |
| ----------------------- | ----------------------------------------------------------------------------------------------------------------------------- | -------------------------------- |
| `IEchoDamageCap` 接口   | [`IEchoDamageCap.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/capability/IEchoDamageCap.java)               | UUID→Float映射                   |
| `EchoDamageCap` 实现    | [`EchoDamageCap.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/capability/EchoDamageCap.java)                 | HashMap实现                      |
| `EchoDamageProvider`    | [`EchoDamageProvider.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/capability/EchoDamageProvider.java)       | Capability提供者+NBT序列化       |
| `EchoCapabilityHandler` | [`EchoCapabilityHandler.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/capability/EchoCapabilityHandler.java) | 为所有LivingEntity附加Capability |

### 5.5 EchoTimerEffect（回响计时器）

**文件：** [`EchoTimerEffect.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/potioneffect/EchoTimerEffect.java)

| 属性         | 值                                          |
| ------------ | ------------------------------------------- |
| **注册名**   | `echo_timer`                                |
| **效果类型** | `HARMFUL`                                   |
| **效果颜色** | `0x000000`（黑色，作为隐藏效果）            |
| **tick效果** | 无（`isDurationEffectTick` 始终返回 false） |

**移除时触发：**

```java
@Override
public void removeAttributeModifiers(LivingEntity entity, AttributeMap map, int amplifier) {
    super.removeAttributeModifiers(entity, map, amplifier);
    if (entity.level().isClientSide || !entity.isAlive()) return;
    if (entity.hasEffect(this)) return;  // 刷新效果时不触发
    EchoDamageHelper.detonateSingle(entity);  // 引爆
}
```

> **🔍 注意：** 60 tick = 3秒。在效果自然到期或被清除时触发单目标引爆。如果效果被刷新（重新叠加），`hasEffect()` 返回 true 因此不会触发。

### 5.6 EchoDamageHelper — 引爆工具类

**文件：** [`EchoDamageHelper.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/utils/EchoDamageHelper.java)

#### `detonateSingle()` — 单目标引爆

```
对所有存储了伤害的攻击者：
    对目标造成：存储伤害值（通用伤害类型）
    重置无敌帧
    播放音效：TRIDENT_RETURN
    粒子效果：PORTAL（传送门环）
```

#### `detonateArea()` — 范围引爆

```
对所有存储了伤害的攻击者：
    获取 radius 内的所有活体目标（包括自身）
    伤害均分：damagePerTarget = totalOverflow / count
    对每个目标造成 damagePerTarget（魔法伤害）
    重置无敌帧
    播放音效：TRIDENT_RETURN
    粒子效果：
        └─ FlatSpreadingRingParticle（紫色扩散环，半径=radius）
        └─ 5个 ENDER_SHARD（末影碎片粒子）
```

> **🔍 均分策略：** 范围引爆会将总伤害均分给范围内所有目标，而不是每个目标都承受全额伤害。这意味着范围越大/目标越多，单个目标承受的伤害越低，但总伤害输出更高。

### 5.7 EchoDeathEventHandler — 死亡引爆

**文件：** [`EchoDeathEventHandler.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/globalevent/EchoDeathEventHandler.java)

```java
@SubscribeEvent(priority = EventPriority.LOWEST)
public static void onDeath(LivingDeathEvent event) {
    LivingEntity entity = event.getEntity();
    if (entity.level().isClientSide) return;
    EchoDamageHelper.detonateArea(entity, 5.0D);  // 5m范围引爆
}
```

> 目标死亡时，其身上存储的所有 ECHO 伤害会以 5m 半径范围引爆，形成 **死亡连锁**。

### 5.8 模型管理 — 连段切换安全机制

**位置：** [`StarlessNightEffects.onBladeMotion()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/starlessnight/StarlessNightEffects.java:86-105)

监听 `BladeMotionEvent`，当连段状态切换到非 EchoingVoid 系列时，自动将模型恢复为 `models/sn.obj`。

**检测的连段ID列表：**

- `echoing_void`
- `echoing_void_0`
- `echoing_void_1`
- `echoing_void_2`
- `echoing_void_end`

> **设计目的：** 防止连段被意外取消或超时后，模型仍然保持 `sn_huge.obj` 的巨大化状态。

---

## 6. 维度五：完整机制链与数据流

### 6.1 循环流程图

```
                        ┌──────────────────────────────────────────┐
                        │          循环核心链路                      │
                        │                                          │
  ┌──────────┐          │  ┌───────────┐    ┌──────────────┐       │
  │  普攻命中  │ ──+1层─→  │  VoidStrike │ ──→│ ECHO伤害增伤  │       │
  └──────────┘          │  │ (层数叠加) │    │ (每层+10%)   │       │
                        │  └───────────┘    └──────┬───────┘       │
  ┌──────────┐          │         ↑                │               │
  │ SA命中10层│ ───+10层─→         │                │               │
  └──────────┘          │         │                ▼               │
                        │  ┌───────────┐    ┌──────────────┐       │
                        │  │  ECHO伤害  │ ──→│ 伤害存储99%+  │       │
                        │  │ (SA输出)   │    │ (Capability) │       │
                        │  └───────────┘    └──────┬───────┘       │
                        │                          │               │
                        │                          ▼               │
                        │  ┌──────────────────────────────┐        │
                        │  │      三途径引爆               │        │
                        │  │                              │        │
                        │  │ 1. EchoTimer到期 → detonateSingle │    │
                        │  │ 2. 目标死亡 → detonateArea(5m) │      │
                        │  │ 3. SA收尾 → detonateArea(10m)  │      │
                        │  │     （对40m内全员触发）         │      │
                        │  └──────────────────────────────┘        │
                        └──────────────────────────────────────────┘

         ┌───────────────────────────────────────┐
         │         EchoingStrike（25%概率）        │
         │  主目标→3次连锁→0.1倍魔法伤害+叠1层     │
         └───────────────────────────────────────┘
```

### 6.2 数据流：从伤害造成到引爆

```
[玩家造成ECHO伤害]
    │
    ▼
[DamageConverterEvent.OnHurt() - LOWEST]
    ├─ 赋予目标 EchoTimerEffect (60tick / 3秒)
    ├─ 存储伤害: cap.addDamage(attackerUUID, amount - 0.1f)
    └─ 即时伤害: event.setAmount(0.1f)
    │
    ▼
[DamageConverterEvent.OnDamage() - LOWEST]
    └─ VoidStrike层数检测: amount *= (1 + layers * 0.1)
    │
    ▼
[StarlessNightEffects.onLivingHurt() - LOWEST]
    └─ 25%概率 → EchoingStrike 连锁
         ├─ 搜索16m内敌人
         ├─ 按VoidStrike层数升序排列
         └─ 最多3次连锁 (0.1倍魔法伤害)
    │
    ▼
[引爆触发] ─── 三种途径
    │
    ├─ EchoTimer到期: detonateSingle()
    │   └─ 对目标造成存储的全额伤害（通用伤害类型）
    │
    ├─ 目标死亡: detonateArea(5m)
    │   └─ 均分给5m内所有敌人（魔法伤害）
    │
    └─ SA收尾ECHOING_VOID_END2: 对所有40m内目标detonateArea(10m)
        └─ 每个目标10m范围内均分
```

### 6.3 伤害计算示例

**场景：** 50层 VoidStrike + ECHOING_VOID_2 第4刀（9f伤害）

```
原始ECHO伤害:   9.0
暴击倍率(×1.1): 9.9
VoidStrike增伤: 9.9 × (1 + 50 × 0.1) = 9.9 × 6.0 = 59.4
即时伤害:        0.1
存储伤害:        59.4 - 0.1 = 59.3

引爆时(若单体):  59.3（通用伤害，无视护甲？取决于DamageTypes.GENERIC）
引爆时(范围5m/5目标): 59.3 / 5 = 11.86 每目标（魔法伤害）
```

---

## 7. 玩法流派总结

### 7.1 核心循环

**虚空印记叠层 → 回响伤害存储 → 连锁引爆**

| 阶段        | 操作                              | 目的                             |
| ----------- | --------------------------------- | -------------------------------- |
| **1. 叠层** | 普攻 / SA(ECHOING_VOID_1)         | 积累 VoidStrike 层数（增伤倍率） |
| **2. 输出** | SA (ECHOING_VOID_2) 4刀横斩       | 造成高额ECHO伤害并存储到目标     |
| **3. 连锁** | 等待EchoingStrike概率触发         | 扩散伤害+叠加新印记              |
| **4. 引爆** | SA收尾 / 等目标死亡 / 等Timer到期 | 释放存储伤害造成爆发             |

### 7.2 推荐操作流程

1. **起手：** 对目标进行若干次普攻（叠加 VoidStrike 层数）
2. **SA释放：** 使用 `ECHOING_VOID`（消耗 4SP）
   - 蓄力阶段（ECHOING_VOID_0）等待剑体巨大化
   - 终结一击（ECHOING_VOID_1）叠 10 层 VoidStrike
   - 4次横斩（ECHOING_VOID_2）造成大量 ECHO 存储伤害
3. **连锁：** EchoingStrike 25%概率触发，扩散到其他目标
4. **引爆：** SA收尾阶段（ECHOING_VOID_END2）对 40m 全员范围引爆
5. **收尾（可选）：** 等待目标自然死亡时触发 5m 范围引爆

### 7.3 优势与劣势

| 优势                            | 劣势                      |
| ------------------------------- | ------------------------- |
| ✅ 群体压制能力极强             | ❌ 单体爆发需要长时间预热 |
| ✅ 延迟伤害机制可规避无敌帧     | ❌ 依赖连段完整释放       |
| ✅ VoidStrike叠层后增伤倍率极高 | ❌ ECHO伤害对免疫生物无效 |
| ✅ 死亡连锁可形成链式反应       | ❌ 操作复杂度高           |
| ✅ 范围引爆无视目标数量         | ❌ SP消耗较大（4SP/次）   |

### 7.4 最佳使用场景

- **群体怪物聚集地：** SA收尾的 40m 范围引爆可以一次性清理大量敌人
- **Boss战（有复数目标）：** EchoingStrike 的连锁机制可以在Boss+小怪间来回弹射
- **持久战：** VoidStrike 层数随时间累积，战斗时间越长伤害倍率越高

---

## 8. 联动设计分析

### 8.1 VoidStrike × ECHO伤害

```
VoidStrike层数 → ECHO伤害倍率放大 → 更多存储伤害 → 引爆时伤害更高
```

这是StarlessNight最核心的内部联动。VoidStrike 层数越高，后续 ECHO 伤害的 DPS 呈线性增长（每层+10%）。而 ECHO 伤害本身又会通过 EchoingStrike 连锁叠加 VoidStrike，形成 **正反馈循环**。

### 8.2 ECHO伤害存储 × 范围引爆

```
单目标存储 → 范围引爆 → 伤害均分 → 多目标同时承伤
```

存储机制的巧妙之处在于：**伤害只存储一份，但引爆时可以作用于多个目标**。虽然均分降低了单体伤害，但总输出大幅提升。配合 SA 收尾的 40m 搜索 + 10m 范围引爆，可以实现恐怖的总伤害量。

### 8.3 EchoTimer × 死亡连锁

```
EchoTimer(3s) ─→ detonateSingle
目标中途死亡 ─→ detonateArea(5m)
              └→ 连锁：5m内的其他存储目标也被引爆？
```

这是一个潜在的 **多米诺骨牌效应**：目标A死亡→5m范围引爆→范围内的目标B如果也存储了ECHO伤害，会被引爆→目标B死亡→再次5m范围引爆……在密集群体中可能形成连续死亡连锁。

### 8.4 EchoingStrike × VoidStrike 排序

```
EchoingStrike按VoidStrike层数升序选择目标
    └→ 优先攻击印记层数低的目标
         └→ 对低层目标叠1层
              └→ 保持所有目标的印记层数相对均衡
```

> **设计意图：** 防止某些目标印记层数过高而其他目标层数过低，保持群体印记层数的一致性，为后续引爆做铺垫。

### 8.5 与RANK系统的联动

[`EchoingVoid.doEnderSlash()`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/EchoingVoid.java:48-51) 中：

```java
playerIn.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT).ifPresent((rank) -> {
    jc.setRank(rank.getRankLevel(playerIn.level().getGameTime()));
});
```

> SA实体继承玩家的集中等级（Rank），Rank越高 → 基础伤害倍率越高 → 存储的ECHO伤害越高。建议在 Rank S 以上使用 SA 以达到最大效率。

### 8.6 协同Mod/装备建议

| 协同项                | 理由                               |
| --------------------- | ---------------------------------- |
| **高Rank集中技能**    | Rank影响SA伤害，间接影响ECHO存储量 |
| **SP回复装备**        | SA消耗4SP，需高效回复以持续输出    |
| **范围聚怪能力**      | StarlessNight的群体优势需目标密集  |
| **BEWITCHED剑型相关** | 同类型剑型可能有额外加成           |

---

## 附录：关键文件索引

| 文件                                                                                                                                                                  | 说明                         |
| --------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------- |
| [`FantasySlashBladeBuiltInRegistry.java:397-432`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:397-432) | StarlessNight 注册定义       |
| [`EchoingVoid.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/EchoingVoid.java)                                                              | SA工具类                     |
| [`EntityEnderSlashEffect.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/entity/EntityEnderSlashEffect.java)                                           | SA实体攻击逻辑               |
| [`EntityFDSlashEffect.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/entity/EntityFDSlashEffect.java)                                                 | 基础实体（含自定义伤害类型） |
| [`FDCombo.java:513-631`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:513-631)                                                           | ECHOING_VOID 连段状态机      |
| [`FDSlashArtRegistry.java:26-27`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSlashArtRegistry.java:26-27)                                         | SA注册                       |
| [`StarlessNightEffects.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/starlessnight/StarlessNightEffects.java)                 | 事件监听中枢                 |
| [`VoidStrikeEffect.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/potioneffect/VoidStrikeEffect.java)                                                 | 虚空印记药水效果             |
| [`EchoTimerEffect.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/potioneffect/EchoTimerEffect.java)                                                   | 回响计时器                   |
| [`EchoDamageCap.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/capability/EchoDamageCap.java)                                                         | ECHO伤害存储实现             |
| [`EchoDamageProvider.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/capability/EchoDamageProvider.java)                                               | Capability提供者             |
| [`EchoCapabilityHandler.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/capability/EchoCapabilityHandler.java)                                         | Capability附加               |
| [`IEchoDamageCap.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/capability/IEchoDamageCap.java)                                                       | Capability接口               |
| [`EchoDamageHelper.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/utils/EchoDamageHelper.java)                                                        | 引爆工具类                   |
| [`DamageConverterEvent.java:149-161`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/globalevent/DamageConverterEvent.java:149-161)           | ECHO伤害存储逻辑             |
| [`DamageConverterEvent.java:177-182`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/globalevent/DamageConverterEvent.java:177-182)           | VoidStrike增伤逻辑           |
| [`EchoDeathEventHandler.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/globalevent/EchoDeathEventHandler.java)                         | 死亡引爆                     |
| [`FDSpecialEffectsRegistry.java:57-60`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSpecialEffectsRegistry.java:57-60)                             | SE注册                       |
| [`FDPotionEffects.java:15-16,33-34`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDPotionEffects.java:15-16)                                         | 药水效果注册                 |
| [`FDDamageSource.java:35`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/damagesource/FDDamageSource.java:35)                                               | ECHO伤害类型注册             |
| [`echo.json`](../../src/generated/resources/data/fantasydesire/damage_type/echo.json)                                                                                 | ECHO伤害类型数据             |
| [`starless_night.json`](../../src/generated/resources/data/fantasydesire/fantasydesire/fantasyslashblade/starless_night.json)                                         | 生成数据                     |
| [`FDSlashArts.java`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/FDSlashArts.java)                                                              | SA封装类                     |
| [`AddonSlashUtils.java:56-91`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/utils/AddonSlashUtils.java:56-91)                                              | doAddonFDSlash 工具方法      |
