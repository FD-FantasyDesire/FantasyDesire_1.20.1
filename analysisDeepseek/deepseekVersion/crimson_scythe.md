# CrimsonScythe（深红恶魔镰刀）— 深度分析文档

> **注册名**: `crimson_scythe`  
> **翻译键**: `item.fantasydesire.crimson_scythe`  
> **特效颜色**: `0xFF0000`（深红）  
> **作者备注**: 吸血续航型拔刀剑，具备聚怪、群攻、自愈三位一体的作战体系

---

## 目录

1. [维度一：基础属性提取](#维度一基础属性提取)
2. [维度二：SA技能分析 — CrimsonStrike](#维度二sa技能分析--crimsonstrike)
3. [维度三：动作连段解析](#维度三动作连段解析)
4. [维度四：连段衍生效果分析](#维度四连段衍生效果分析)
5. [维度五：特殊效果机制分析](#维度五特殊效果机制分析)
6. [核心玩法流派总结](#核心玩法流派总结)
7. [联动设计理念](#联动设计理念)

---

## 维度一：基础属性提取

以下数据均提取自 [`FantasySlashBladeBuiltInRegistry.java:143-176`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:143)。

### 注册信息

| 属性             | 值                           |
| ---------------- | ---------------------------- |
| **注册名**       | `crimson_scythe`             |
| **特效颜色**     | `0xFF0000`（深红）           |
| **纹理路径**     | `models/crimsonscythe.png`   |
| **模型路径**     | `models/crimsonscythe.obj`   |
| **携带方式**     | `CarryType.PSO2`（背后悬浮） |
| **基础攻击修正** | `4.5F`（较高）               |
| **剑类型**       | `[SwordType.BEWITCHED]`      |
| **最大伤害**     | `150`                        |

### 附魔

| 附魔                             | 等级 |
| -------------------------------- | ---- |
| `SMITE`（亡灵杀手）              | VI   |
| `MOB_LOOTING`（抢夺）            | VI   |
| `BANE_OF_ARTHROPODS`（节肢杀手） | VI   |

### 特殊能量系统 — BloodiedHook

| 属性             | 值                         |
| ---------------- | -------------------------- |
| **最大能量**     | 12 点                      |
| **能量名称**     | `"BloodiedHook"`           |
| **特殊类型**     | `"CrimsonScythe"`          |
| **特殊攻击效果** | `"absorb"`（吸血伤害类型） |

### 绑定的技能与特殊效果

- **SA（剑技）**: [`FDSlashArtRegistry.CRIMSON_STRIKE`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSlashArtRegistry.java:16)
- **SE（特殊效果）**:
  - [`FDSpecialEffectsRegistry.BloodDrain`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSpecialEffectsRegistry.java:43)（鲜血引流）
  - [`FDSpecialEffectsRegistry.CrimsonStrike`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSpecialEffectsRegistry.java:45)（深红打击）

### SE注册参数

- **BloodDrain**: 等级 60, 稀有度 2 [`FDSpecialEffectsRegistry.java:43-44`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSpecialEffectsRegistry.java:43)
- **CrimsonStrike**: 等级 10, 稀有度 1 [`FDSpecialEffectsRegistry.java:45-46`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSpecialEffectsRegistry.java:45)

---

## 维度二：SA技能分析 — CrimsonStrike

SA注册于 [`FDSlashArtRegistry.java:16-17`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSlashArtRegistry.java:16)：

```java
CRIMSON_STRIKE = FD_SLASH_ARTS.register("crimson_strike",
    () -> new FDSlashArts((e) -> FDCombo.CRIMSON_STRIKE.getId(), 1));
```

- **连段ID**: `FDCombo.CRIMSON_STRIKE`
- **所需能量**: 1 点

### 2.1 `ShootHunterSword()` — 猎杀之剑齐射

位于 [`CrimsonStrike.java:26-80`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/CrimsonStrike.java:26)。

#### 核心参数

| 参数         | 值                 | 说明             |
| ------------ | ------------------ | ---------------- |
| **剑数量**   | 24 把              | 满编齐射         |
| **分布半径** | 1.5 格             | 围绕玩家均匀分布 |
| **索敌范围** | 40 格              | 大范围自动索敌   |
| **追踪模式** | `SEEK`             | 主动追踪目标     |
| **追踪角度** | `36°`              | 转向灵活度       |
| **追踪延迟** | 10 tick            | 追踪启动延迟     |
| **总体延迟** | 100 tick           | 剑存在总时间     |
| **缩放比例** | 0.5                | 缩小尺寸         |
| **拖尾效果** | `setHasTail(true)` | 有粒子拖尾       |

#### 斐波那契球面分布算法

使用黄金角算法实现24把猎杀之剑在球面上的均匀分布 [`CrimsonStrike.java:38-49`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/CrimsonStrike.java:38)：

```java
double phi = Math.PI * (3.0 - Math.sqrt(5.0)); // 黄金角 ≈ 137.5°
for (int i = 0; i < count; i++) {
    double y = 1 - (i / (double)(count - 1)) * 2;
    double radiusAtY = Math.sqrt(1 - y * y);
    double theta = phi * i;
    double x = Math.cos(theta) * radiusAtY;
    double z = Math.sin(theta) * radiusAtY;
    Vec3 dir = new Vec3(x, y, z).normalize();
    // ...
}
```

这种算法确保了剑在三维空间中的均匀分布，没有极区聚集效应。

#### 目标分配机制

```java
// CrimsonStrike.java:72-75
if (!targets.isEmpty()) {
    LivingEntity target = targets.get(i % targets.size());
    sword.setTargetId(target.getId());
}
```

24把剑循环均匀分配给所有目标（`i % targets.size()`），保证多目标环境下每个目标分配到大致等量的剑。

#### 音效

`SoundEvents.TRIDENT_THROW`（三叉戟投掷音效） [`CrimsonStrike.java:79`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/CrimsonStrike.java:79)

### 2.2 `doTripleAddonFDSlash()` — 三段斩

位于 [`CrimsonStrike.java:82-104`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/CrimsonStrike.java:82)。

**组成结构**：

1. **主斩击** — 中心位置，使用 `AddonSlashUtils.doAddonFDSlash()` 生成
2. **左偏移斩击** — `centerOffset + (0, +dy, +dz)`，距离 1.5 格
3. **右偏移斩击** — `centerOffset + (0, -dy, -dz)`，距离 1.5 格

**伤害特性**：

- 伤害类型: `FDDamageSource.ABSORB`（吸血） [`CrimsonStrike.java:90`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/CrimsonStrike.java:90)
- 缩放: `3.0f`
- 击退: `KnockBacks.cancel`（无击退）
- 伤害值: 由调用者传入（连段中为 `4f`）

### 2.3 `AntiNTR()` — 防伪检查

位于 [`CrimsonStrike.java:20-24`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/CrimsonStrike.java:20)：

```java
public static boolean AntiNTR(LivingEntity entity) {
    return CapabilityUtils.SEConditionMatcher.of(entity)
            .requireTranslation("item.fantasydesire.crimson_scythe")
            .match() != null;
}
```

通过验证手中物品的翻译键是否为 `item.fantasydesire.crimson_scythe` 来防止非本刀的实体触发技能。

---

## 维度三：动作连段解析

连段状态注册于 [`FDCombo.java:110-180`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:110)。

### 连段状态机流转图

```
                   ┌──────────────────────────────────────────┐
                   │           CRIMSON_STRIKE (入口)           │
                   │       startAndEnd(0,1)  priority=50      │
                   │       AntiNTR 检查                       │
                   └─────────────┬────────────────────────────┘
                                 │ 通过
                                 ▼
                   ┌──────────────────────────────────────────┐
                   │          CRIMSON_STRIKE_0 (剑阵展开)     │
                   │       startAndEnd(2200,2251)             │
                   │       Tick 16: ShootHunterSword()        │
                   │       ← 发射24把猎杀之剑                 │
                   │       超时: 16帧 → crimson_strike_1      │
                   └─────────────┬────────────────────────────┘
                                 │
                                 ▼
                   ┌──────────────────────────────────────────┐
                   │        CRIMSON_STRIKE_1 (腥红风暴)       │
                   │       startAndEnd(1816,1859)  speed=6F   │
                   │       clickAction: doTripleAddonFDSlash() │
                   │       ← 三段斩(22.5° roll, 4f dmg, 20t) │
                   │       超时: 18帧 → crimson_strike_2      │
                   └─────────────┬────────────────────────────┘
                                 │
                                 ▼
                   ┌──────────────────────────────────────────┐
                   │        CRIMSON_STRIKE_2 (深红裁决)       │
                   │       startAndEnd(204,218)  speed=1.1F   │
                   │       clickAction: doTripleAddonFDSlash() │
                   │       ← 三段斩(157.5° roll, 4f dmg, 15t)│
                   │       超时: 20帧 → crimson_strike_end    │
                   └─────────────┬────────────────────────────┘
                                 │
                                 ▼
                   ┌──────────────────────────────────────────┐
                   │       CRIMSON_STRIKE_END (收尾)          │
                   │       startAndEnd(218,281)  aerial()     │
                   │       Tick 0: playQuickSheathSoundAction │
                   │       releaseAction: 快速蓄力检测         │
                   │       超时 → crimson_strike_end2          │
                   └─────────────┬────────────────────────────┘
                                 │
                                 ▼
                   ┌──────────────────────────────────────────┐
                   │      CRIMSON_STRIKE_END2 (完全结束)      │
                   │       startAndEnd(281,314)  aerial()     │
                   │       releaseAction: 快速蓄力检测          │
                   │       超时 → none                        │
                   └──────────────────────────────────────────┘
```

### 连段状态详细参数表

| 状态                  | 帧范围    | 速度    | 优先级 | 空中 | 前进(超时)                  | 关键动作                          |
| --------------------- | --------- | ------- | ------ | ---- | --------------------------- | --------------------------------- |
| `CRIMSON_STRIKE`      | 0-1       | 1.0     | 50     | 否   | `CRIMSON_STRIKE_0`          | AntiNTR检查                       |
| `CRIMSON_STRIKE_0`    | 2200-2251 | 1.0     | 50     | 否   | `CRIMSON_STRIKE_1` (16帧)   | **Tick 16: `ShootHunterSword()`** |
| `CRIMSON_STRIKE_1`    | 1816-1859 | **6.0** | 50     | 否   | `CRIMSON_STRIKE_2` (18帧)   | **clickAction: 三段斩(22.5°)**    |
| `CRIMSON_STRIKE_2`    | 204-218   | 1.1     | 50     | 否   | `CRIMSON_STRIKE_END` (20帧) | **clickAction: 三段斩(157.5°)**   |
| `CRIMSON_STRIKE_END`  | 218-281   | 1.0     | 50     | ✅是 | `CRIMSON_STRIKE_END2`       | 收刀音效 + 快速蓄力               |
| `CRIMSON_STRIKE_END2` | 281-314   | 1.0     | 50     | ✅是 | `none`                      | 收刀音效 + 快速蓄力               |

### 关键说明

1. **CRIMSON_STRIKE_1** 的 `speed=6F` 说明这个阶段的动画以 6 倍速播放，表现快节奏的乱舞斩击
2. **CRIMSON_STRIKE_END** 和 **CRIMSON_STRIKE_END2** 均为 `aerial()` 空中状态，允许在空中执行收尾动作
3. 三段斩(157.5°) 的计算方式为 `180° - 22.5°`，与一段的 22.5° 形成镜像对称
4. 所有连段状态的 `addHitEffect` 均调用 `StunManager::setStun`，确保每次命中都造成硬直

---

## 维度四：连段衍生效果分析

### 4.1 `ShootHunterSword()` 触发时机

在 [`CRIMSON_STRIKE_0`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:120-128) 的 **第16 tick** 触发：

```java
.addTickAction(ComboState.TimeLineTickAction.getBuilder()
    .put(16, entityIn -> CrimsonStrike.ShootHunterSword(entityIn))
    .build())
```

- 此阶段帧范围为 2200-2251，触发点为动画的第 16 帧
- 此时玩家周围绽放 24 把猎杀之剑围绕自身呈球面分布
- 剑会自动索敌并追踪，将目标拉近

### 4.2 `doTripleAddonFDSlash()` 触发条件

**CRIMSON_STRIKE_1**（腥红风暴）— `clickAction` 触发 [`FDCombo.java:136-142`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:136)：

```java
.clickAction((entityIn) -> CrimsonStrike.doTripleAddonFDSlash(
    entityIn, 22.5F, entityIn.getYRot(), 0, 0xFF0000, 0,
    Vec3.ZERO, false, false, 4f, KnockBacks.cancel, 20))
```

- 玩家每次点击（攻击输入）都会触发
- roll = 22.5°，生命期 20 tick，伤害 4f
- 速度 6F 让玩家可以快速连续点击触发多次

**CRIMSON_STRIKE_2**（深红裁决）— `clickAction` 触发 [`FDCombo.java:153-159`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:153)：

```java
.clickAction((entityIn) -> CrimsonStrike.doTripleAddonFDSlash(
    entityIn, 180F - 22.5F, entityIn.getYRot(), 0, 0xFF0000, 0,
    Vec3.ZERO, false, false, 4f, KnockBacks.cancel, 15))
```

- roll = 157.5°（与一段反向），生命期 15 tick（更短、更快）
- 作为终结式爆发

### 4.3 击中效果

所有连段状态均通过 `addHitEffect(StunManager::setStun)` 在每次命中时对目标施加 **眩晕** 效果，确保连段的连续性，防止目标逃脱。

---

## 维度五：特殊效果机制分析

### 5.1 BloodDrain（鲜血引流）SE

注册于 [`FDSpecialEffectsRegistry.java:43-44`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSpecialEffectsRegistry.java:43)，实现在 [`CrimsonScytheEffects.java:65-126`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/crimsonscythe/CrimsonScytheEffects.java:65)。

#### 触发条件

- 事件: `SlashBladeEvent.DoSlashEvent`（每次挥刀时触发）
- 条件: 手持物品翻译键匹配 + 刀上拥有 `BloodDrain` SE

#### 核心机制 — 发射钩爪幻影剑拉拽

**参数计算（受横扫附魔加成）** [`CrimsonScytheEffects.java:72-74`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/crimsonscythe/CrimsonScytheEffects.java:72)：

| 参数         | 基础值 | 每级横扫加成 |
| ------------ | ------ | ------------ |
| **索敌距离** | 15 格  | +10 格       |
| **最大剑数** | 3 把   | +1 把        |
| **索敌角度** | 30°    | +10°         |

**目标选择**：

1. 使用 `FDTargetSelector.getTargetsInSight()` 获取视野锥体内的敌人
2. 按 **距离从远到近排序**（优先拉拽远处的敌人）[`CrimsonScytheEffects.java:77`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/crimsonscythe/CrimsonScytheEffects.java:77)
3. 如果无索敌结果但有锁定目标，则至少发射 1 把剑

**能量消耗**：

- 每次发射消耗 1 点 `BloodiedHook` 能量
- 如果能量不足则停止发射 (`tryConsumeSpecialCharge`) [`CrimsonScytheEffects.java:87`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/crimsonscythe/CrimsonScytheEffects.java:87)

**幻影剑参数** [`CrimsonScytheEffects.java:90-124`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/crimsonscythe/CrimsonScytheEffects.java:90)：

| 参数       | 值                        | 说明                   |
| ---------- | ------------------------- | ---------------------- |
| 伤害       | `0.001`                   | 几乎无伤害，纯拉拽功能 |
| 速度       | `1.0f`                    | 标准飞行速度           |
| 追踪模式   | `SEEK`                    | 主动追踪目标           |
| 追踪延迟   | 5 tick                    | 快速响应               |
| 追踪角度   | 36°                       | 灵活转向               |
| 可多次命中 | `true`                    | 可穿透多个目标         |
| 音效       | `SoundEvents.CHAIN_BREAK` | 锁链音效               |

#### BloodDrain 命中恢复能量

在 `onHit` 事件中 [`CrimsonScytheEffects.java:130-139`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/crimsonscythe/CrimsonScytheEffects.java:130)：

```java
if (ctx != null) {
    IFantasySlashBladeState fdState = ctx.fantasyState;
    CapabilityUtils.addSpecialCharge(fdState, 1);
}
```

**每次命中恢复 1 点特殊能量**，形成"发射→命中→回能→再发射"的能量循环。

#### 猎杀之剑命中拉拽逻辑

位于 [`EntityFDHuntSword.java:38-45`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/entity/EntityFDHuntSword.java:38)：

```java
if (shooter.distanceTo(targetEntity) > 5) {
    Vec3 motion = shooter.position().subtract(targetEntity.position()).normalize().scale(0.8);
    targetEntity.setDeltaMovement(motion);
    targetEntity.hurtMarked = true;
}
```

- 距离 > 5 格时才触发拉拽
- 拉拽方向：敌人 → 玩家
- 力度：0.8（强力拉拽）

---

### 5.2 CrimsonStrike（深红打击）SE

注册于 [`FDSpecialEffectsRegistry.java:45-46`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSpecialEffectsRegistry.java:45)，实现在 [`CrimsonScytheEffects.java:41-63`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/crimsonscythe/CrimsonScytheEffects.java:41)。

#### 触发条件

- 事件: `SlashBladeEvent.DoSlashEvent`
- 条件: 手持物品翻译键匹配 + 刀上拥有 `CrimsonStrike` SE

#### 核心机制 — 吸血爪刃斩击

每次普通攻击时，额外生成 **两道爪刃斩击** [`CrimsonScytheEffects.java:55-62`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/crimsonscythe/CrimsonScytheEffects.java:55)：

```java
AddonSlashUtils.doAddonFDSlash(entity, roll, entity.getYRot(), 0.0f, color, 0,
    offset1, true, false, ratio, KnockBacks.cancel, 1.0f, 10,
    FDDamageSource.ABSORB.location().toString());
AddonSlashUtils.doAddonFDSlash(entity, roll, entity.getYRot(), 0.0f, color, 0,
    offset2, true, false, ratio, KnockBacks.cancel, 1.0f, 10,
    FDDamageSource.ABSORB.location().toString());
```

**关键参数**：

| 参数     | 值                      | 说明               |
| -------- | ----------------------- | ------------------ |
| 偏移距离 | 0.5 格                  | 产生双爪刃视觉效果 |
| 伤害     | `原伤害比例(ratio)`     | 继承原攻击伤害     |
| 伤害类型 | `FDDamageSource.ABSORB` | 吸血伤害           |
| 击退     | `KnockBacks.cancel`     | 无击退             |
| 缩放     | `1.0f`                  | 标准大小           |
| 生命期   | 10 tick                 | 快速生效           |
| 静音     | `true`                  | 不播额外音效       |

#### 与 SA `CRIMSON_STRIKE` 的区别与联系

| 维度         | SA CrimsonStrike                  | SE CrimsonStrike           |
| ------------ | --------------------------------- | -------------------------- |
| **触发方式** | 主动释放 SA 技能（消耗 1 点能量） | 被动，每次普通攻击自动触发 |
| **效果**     | 发射24剑 + 三段斩                 | 双爪刃吸血斩击             |
| **伤害类型** | ABSORB                            | ABSORB                     |
| **能量消耗** | 1 点                              | 无                         |
| **冷却**     | 连段驱动                          | 每次挥刀                   |
| **定位**     | 爆发技能                          | 常驻吸血                   |

**两者协同**：SE 提供持续的吸血续航，SA 提供爆发性的聚怪和群攻，互补形成完整的战斗体系。

---

### 5.3 ABSORB 伤害类型处理

位于 [`DamageConverterEvent.java:203-213`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/globalevent/DamageConverterEvent.java:203)。

#### 处理流程（`LivingDamageEvent` 中执行）

```java
if (source.is(FDDamageSource.ABSORB)) {
    float heal = amount;                              // ① 获取伤害值
    float missing = attackerLiving.getMaxHealth()      // ② 计算缺口血量
        - attackerLiving.getHealth();
    float overflow = Math.max(0, heal - missing);      // ③ 计算溢出量
    attackerLiving.heal(heal);                         // ④ 治疗全额伤害
    if (overflow > 0) {
        float bonus = overflow * 0.1f;                 // ⑤ 溢出部分10%→护盾
        float newAbsorb = Math.min(20f,
            attackerLiving.getAbsorptionAmount() + bonus);
        attackerLiving.setAbsorptionAmount(newAbsorb);  // ⑥ 设置护盾值
    }
}
```

#### 吸血比例说明

| 条件                 | 效果                                                           |
| -------------------- | -------------------------------------------------------------- |
| **治疗量**           | = 全额伤害值（100% 吸血）                                      |
| **血量未满**         | 优先治疗，回复量 = 伤害值                                      |
| **血量回满后的溢出** | 溢出部分 × 10% 转换为 **吸收护盾**                             |
| **护盾上限**         | 20 点（10 颗心）                                               |
| **示例**             | 造成 100 伤害，缺 50 血 → 治疗 50，溢出 50 → 额外获得 5 点护盾 |

#### 前置处理：伤害类型替换

在 [`DamageConverterEvent.java:50-68`](../../src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/globalevent/DamageConverterEvent.java:50) 的 `OnSlash` 方法中：

1. 检测刀上 `fdState.getSpecialAttackEffect()` 是否为 `"absorb"`
2. 若是，使用 `FDAttackManager.areaAttackWithSource()` 替换伤害源为 `FDDamageSource.ABSORB`
3. 原始伤害置 0，由新伤害源重新计算

这是为了解决 SlashEffect 不兼容自定义伤害类型且硬编码于架势动作中的问题。

---

## 核心玩法流派总结

### 吸血续航流

利用 **SE CrimsonStrike** + **ABSORB伤害类型** 形成持续的生命回复。

```
普通攻击 → 触发CrimsonStrike SE → 双爪刃斩击(ABSORB伤害)
  → DamageConverterEvent处理 → 全额治疗 → 溢出变护盾
```

- 基础攻击修正 4.5F 保证基础伤害充足
- 每次攻击额外附带 100% 吸血效果
- 能量过剩时溢出转化为护盾，提供额外生存能力

### 聚怪爆发流

利用 **BloodDrain SE** + **SA CRIMSON_STRIKE** 形成控制+爆发的连招：

```
挥刀触发BloodDrain → 发射钩爪剑拉拽敌人 → 命中回能
  → 释放SA CRIMSON_STRIKE → 24剑球面齐射群攻
    → 三段斩追加 → ABSORB吸血大量回血
```

- BloodDrain 拉拽聚怪（距离可达 15-45 格）
- 24 把猎杀之剑均匀分配所有目标
- SA三段斩提供额外伤害和吸血

### 持续作战流

能量循环体系保证了长时间战斗的续航能力：

```
BloodDrain发射(消耗1能量) → 猎杀剑命中(回复1能量)
  → 能量收支平衡 → 可无限循环
SE CrimsonStrike不消耗能量 → 常驻吸血
SA释放(消耗1能量) → 爆发输出 → 期间普攻回复能量
```

---

## 联动设计理念

CrimsonScythe 的整个技能体系围绕 **"吸血续航"** 这一核心主题设计，各组件形成闭合的联动循环：

```
                    ┌──────────────────────────────────────┐
                    │          BloodDrain (SE)              │
                    │    挥刀发射钩爪剑 → 拉拽敌人           │
                    │    命中回能 → 能量循环                 │
                    └───────────┬──────────────────────────┘
                                │ ① 拉拽聚怪
                                ▼
                    ┌──────────────────────────────────────┐
                    │       SA CrimsonStrike                │
                    │    24剑齐射 → 自动索敌群攻             │
                    │    三段斩追加 → ABSORB伤害             │
                    └───────────┬──────────────────────────┘
                                │ ② 群攻+吸血
                                ▼
                    ┌──────────────────────────────────────┐
                    │   SE CrimsonStrike (常驻)              │
                    │    每次普攻 → 双爪刃吸血斩击            │
                    │    不耗能 → 常驻续航                   │
                    └───────────┬──────────────────────────┘
                                │ ③ 吸血
                                ▼
                    ┌──────────────────────────────────────┐
                    │   DamageConverterEvent (ABSORB处理)    │
                    │    全额治疗 → 溢出 → 护盾转化          │
                    │    护盾上限 20 点                      │
                    └───────────┬──────────────────────────┘
                                │ ④ 回馈
                                ▼
                    ┌──────────────────────────────────────┐
                    │     能量循环(BloodiedHook)             │
                    │     BloodDrain发射消耗1 → 命中回复1    │
                    │     SA消耗1 → 普攻回能                 │
                    │     维持战斗续航                       │
                    └──────────────────────────────────────┘
```

### 设计哲学总结

> **"以血还血"** — 将每一次造成的伤害转化为自身的生命力。

1. **BloodDrain** 作为起手技，承担"聚怪"和"能量获取"双重角色，其横扫附魔加成机制鼓励玩家提升附魔等级
2. **SA CrimsonStrike** 作为核心爆发，24剑球面齐射 + 三段斩的组合提供优秀的 AOE 能力
3. **SE CrimsonStrike** 作为常驻被动，将每一次普通攻击都转化为治疗效果
4. **ABSORB 伤害类型** 将整个体系串联起来，100% 吸血 + 溢出护盾保证了极高的生存能力
5. **特殊能量系统** 作为资源管理核心，平衡了 BloodDrain 的消耗与回复，防止无限消耗

---

> 本文档基于游戏版本 `FantasyDesire 1.20.1` 源代码分析生成  
> 最后更新: 2026-06-23
