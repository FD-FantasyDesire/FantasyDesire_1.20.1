# FantasyDesire 拔刀剑深度分析文档：奇克芙蕾雅 (ChikeFlare / `chikeflare`)

> 本文档基于 `FantasyDesire` 模组源代码进行深度分析，覆盖注册信息、SA技能、动作连段、特殊效果及核心玩法流派。

---

## 维度一：基础属性提取

### 注册信息

| 属性             | 值                                                | 来源                                                                                                                                                      |
| ---------------- | ------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **注册名**       | `fantasydesire:chikeflare`                        | [`FantasySlashBladeBuiltInRegistry.java`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:42)       |
| **翻译键**       | `item.fantasydesire.chikeflare`                   | [`WingToTheFuture.java:21`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/WingToTheFuture.java:21)                                         |
| **特效颜色**     | `0xFFFF00`（金色）                                | [`FantasySlashBladeBuiltInRegistry.java:45`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:45)    |
| **纹理模型**     | `models/chikeflare.png` / `models/chikeflare.obj` | [`FantasySlashBladeBuiltInRegistry.java:46-49`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:46) |
| **携带方式**     | `PSO2`（背后悬浮）                                | [`FantasySlashBladeBuiltInRegistry.java:50`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:50)    |
| **基础攻击修正** | `0.2F`（极低）                                    | [`FantasySlashBladeBuiltInRegistry.java:53`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:53)    |
| **剑类型**       | `BEWITCHED`（被诅咒的/妖刀）                      | [`FantasySlashBladeBuiltInRegistry.java:54`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:54)    |
| **最大伤害**     | `40`                                              | [`FantasySlashBladeBuiltInRegistry.java:55`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:55)    |
| **附魔**         | UNBREAKING X, BINDING_CURSE I                     | [`FantasySlashBladeBuiltInRegistry.java:75-78`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:75) |

### 特殊能量系统 - `Soul`

| 属性                 | 值                          |
| -------------------- | --------------------------- |
| **能量名称**         | `Soul`（灵魂）              |
| **最大充能**         | `1000`                      |
| **特殊类型**         | `Yarimono`（枪系变体）      |
| **特殊攻击效果**     | `dimension`（次元伤害类型） |
| **特殊传说行数**     | `3`                         |
| **特殊攻击传说行数** | `6`                         |

定义于 [`FantasySlashBladeBuiltInRegistry.java:67-74`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:67)

### 绑定的SA与SE

| 类型   | 注册名                                       | 来源                                                                                                                                                   |
| ------ | -------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **SA** | `WING_TO_THE_FUTURE`（`wing_to_the_future`） | [`FantasySlashBladeBuiltInRegistry.java:64`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:64) |
| **SE** | `CheatRumble`（欺诈轰鸣）                    | [`FantasySlashBladeBuiltInRegistry.java:56`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:56) |
| **SE** | `TyrantStrike`（暴君打击）                   | [`FantasySlashBladeBuiltInRegistry.java:58`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:58) |
| **SE** | `SoulShield`（灵魂屏障）                     | [`FantasySlashBladeBuiltInRegistry.java:60`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:60) |
| **SE** | `ImmortalSoul`（不朽之魂）                   | [`FantasySlashBladeBuiltInRegistry.java:62`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:62) |

---

## 维度二：SA技能分析 - `WingToTheFuture`（通向未来的羽翼）

### 文件位置

[`WingToTheFuture.java`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/WingToTheFuture.java)

### 2.1 SA注册

在 [`FDSlashArtRegistry.java:12-13`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSlashArtRegistry.java:12) 中注册为：

```java
public static final RegistryObject<SlashArts> WING_TO_THE_FUTURE = FD_SLASH_ARTS.register("wing_to_the_future",
    () -> new FDSlashArts((e) -> FDCombo.WING_TO_THE_FUTURE.getId(), 4, true));
```

- **连段消耗**: `4` 点连段槽
- **可空中释放**: `true`

### 2.2 核心方法：`WingToTheFuture()`（第30-92行）

#### 防伪检查 - `AntiNTR()`（第23-27行）

```java
public static boolean AntiNTR(LivingEntity entity) {
    return CapabilityUtils.SEConditionMatcher.of(entity)
            .requireTranslation(CHIKEFLARE_KEY)
            .match() != null;
}
```

- 检查玩家主手物品的翻译键是否为 `item.fantasydesire.chikeflare`
- 用于确认当前武器为正品奇克芙蕾雅，防止"NTR"（被其他刀或非正品武器使用）

#### 羽翼展开机制（第39-91行）

**翼数计算**（第39行）：

```java
int wingCount = Mth.clamp((int) (Math.sqrt(Math.abs(((Player) player).experienceLevel)) - 5), 1, 3);
```

- 基于玩家经验等级计算翼数：`sqrt(|expLevel|) - 5`
- 钳制范围：`1~3` 翼
- **例如**：36级 -> `sqrt(36)-5 = 1`翼；81级 -> `sqrt(81)-5 = 4`，钳制为3翼；144级 -> `sqrt(144)-5 = 7`，钳制为3翼

**每翼羽毛数**（第43行）：

```java
int maxFeather = 32;
```

- 每翼最多32根"羽毛"（即灵魂幻影剑）
- 总数最多：`3 × 32 = 96`把幻影剑

**黄金螺旋排列**（第52-61行）：

使用斐波那契球面算法思想，将幻影剑沿对数螺旋排列：

```java
float progress = (float) j / (maxFeather - 1);
float xRotDeg = 60f - progress * 120f;  // 从60°到-60°旋转
float yRotDeg = front ? 120f : -120f;
Vec3 sec = base.yRot(Math.toRadians(yRotDeg + i * (front ? 5f : -5f)))
        .xRot(Math.toRadians(xRotDeg - i * (5f)))
        .normalize()
        .scale(baseRadius * Math.pow(1.05, j) - i * 0.25);
```

- 基础半径 `2.5f`，每把剑按 `1.05^j` 指数增长排列
- 前翼颜色：`0xFFFF00`（金色），后翼颜色：`0x00FFFF`（青色）
- 前后交替排列（`count % 2 == 0`）

**幻影剑属性**（第62-89行）：

使用 [`EntityFDSoulPhantomSword`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/entity/EntityFDSoulPhantomSword.java) 实体，造成**次元（dimension）伤害**（由父类 [`EntityFDPhantomSword`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/entity/EntityFDPhantomSword.java) 的 `onHitEntity` 方法[第562-568行]根据 `DAMAGE_TYPE` 数据参数决定伤害类型）：

| 属性         | 值                             |
| ------------ | ------------------------------ |
| **待命模式** | `PLAYER`（绑定玩家）           |
| **追踪模式** | `ADV_SEEK`（高级追踪）         |
| **速度**     | `2.5f`                         |
| **缩放**     | `1.5f`                         |
| **爆炸半径** | `3f`（命中后爆炸）             |
| **发射延迟** | `20 + countdownValue + 5` tick |
| **追踪延迟** | `20 + countdownValue` tick     |
| **最大生存** | `100 + countdownValue` tick    |
| **拖尾**     | 开启                           |
| **穿墙**     | 开启（`setNoClip(true)`）      |

**伤害计算**（第41行）：

```java
float magicDamage = 1.0f + (baseModif / 2.0f);
```

- 基础伤害 = `1.0 + 基础攻击修正 / 2.0`
- 基于奇克芙蕾雅极低的 `0.2F` 基础攻击修正，每把剑的伤害约为 `1.1`（但会随 `ImmortalSoul` 触发的永久增长而增加）

**目标锁定**（第44, 83-87行）：

- 优先使用拔刀剑状态中存储的目标
- 若无目标，则使用 [`FDTargetSelector.getTargetsInSight()`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/utils/FDTargetSelector.java) 搜索视野内35格距离、20格宽度的敌人

### 2.3 `ConvertChikeFlare()` 转化方法（第95-111行）

当非正品刀使用SA时触发：

```java
ItemStack newBlade = ItemUtils.dataBakeBlade(blade,
    FantasyDesire.getBladeAsRegistry(player.level(), FantasySlashBladeBuiltInRegistry.ChikeFlare));
```

- 将当前物品"烘焙"转换为奇克芙蕾雅的正品数据
- 播放三叉戟雷鸣音效（`SoundEvents.TRIDENT_THUNDER`）
- 从头顶召唤巨大的闪电粒子效果（`LightBoltParticles`），白色闪电柱从天而降
- 额外16道金色（`0xFFFF00`）和青色（`0x00FFFF`）闪电向四周扩散

---

## 维度三：动作连段解析

### 文件位置

[`FDCombo.java`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java)

### 3.1 连段状态机总览

奇克芙蕾雅的连段涉及以下6个ComboState注册（第37-108行）：

| 状态名                      | 行号    | 功能                             |
| --------------------------- | ------- | -------------------------------- |
| `WING_TO_THE_FUTURE`        | 37-59   | 入口：防伪检测 + 模态分流        |
| `CHIKE_FLARE_CONVERT`       | 61-68   | 转化：将非正品刀转化为奇克芙蕾雅 |
| `WING_TO_THE_FUTURE_GROUND` | 70-83   | 地面模式：挥砍+召唤羽翼          |
| `WING_TO_THE_FUTURE_ELYTRA` | 85-98   | 滑翔模式：赋予COMET_ELYTRA效果   |
| `WING_TO_THE_FUTURE_END`    | 100-108 | 收尾：归鞘+快速充能              |

### 3.2 入口状态：`WING_TO_THE_FUTURE`（第37-59行）

```java
.startAndEnd(0, 1).priority(50)
```

- **帧范围**：`0~1`（瞬间过渡状态）
- **优先级**：`50`
- **next & nextOfTimeout**：根据 `AntiNTR()` 校验结果分流
  - **正品检测通过**：
    - 滑翔中 → `wing_to_the_future_elytra`
    - 地面 → `wing_to_the_future_ground`
  - **检测失败**：→ `chike_flare_convert`

### 3.3 转化状态：`CHIKE_FLARE_CONVERT`（第61-68行）

```java
.startAndEnd(0, 1).priority(50)
.clickAction(entity -> WingToTheFuture.ConvertChikeFlare(entity, entity.getMainHandItem()))
```

- **帧范围**：`0~1`
- **优先级**：`50`
- **clickAction**：触发上文分析的 `ConvertChikeFlare()` 转化方法
- 转化后流转至 `none`（连段结束）

### 3.4 地面模式：`WING_TO_THE_FUTURE_GROUND`（第70-83行）

```java
.startAndEnd(400, 459).priority(50)
.next(TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
.nextOfTimeout(entity -> FantasyDesire.prefix("wing_to_the_future_end"))
```

- **帧范围**：`400~459`（60帧动画）
- **优先级**：`50`
- **超时流转**：→ `wing_to_the_future_end`
- **时间轴动作**（`tickAction`）：
  - **第2帧**：`AttackManager.doSlash(entityIn, -30F, Vec3.ZERO, false, false, 0.1F)` — 一次-30°角的轻挥砍，倍率`0.1F`
  - **第9帧**：`WingToTheFuture.WingToTheFuture(entityIn, entityIn.getMainHandItem())` — 正式召唤羽翼幻影剑
- **命中效果**：`StunManager::setStun` — 使目标眩晕

### 3.5 滑翔模式：`WING_TO_THE_FUTURE_ELYTRA`（第85-98行）

```java
.startAndEnd(400, 459).priority(50)
.next(entity -> FantasyDesire.prefix("wing_to_the_future_end"))
.nextOfTimeout(entity -> FantasyDesire.prefix("wing_to_the_future_end"))
```

- **帧范围**：`400~459`
- **优先级**：`50`
- **流转**：直接进入收尾状态
- **时间轴动作**（`tickAction`）：
  - **第1帧**：`entityIn.addEffect(new MobEffectInstance(FDPotionEffects.COMET_ELYTRA.get(), 1200, 0))`
  - 为玩家施加持续1200 tick（60秒）的 `COMET_ELYTRA`（彗星滑翔）效果
- **关键区别**：滑翔模式**不召唤幻影剑**，而是赋予后续坠击能力

### 3.6 收尾状态：`WING_TO_THE_FUTURE_END`（第100-108行）

```java
.startAndEnd(459, 488).priority(50)
.next(entity -> SlashBlade.prefix("none"))
.nextOfTimeout(entity -> SlashBlade.prefix("none"))
```

- **帧范围**：`459~488`（29帧收尾动画）
- **优先级**：`50`
- **第0帧**：播放快速归鞘音效
- **释放动作**（`releaseAction`）：`releaseActionQuickCharge` — 快速充能

---

## 维度四：连段衍生效果分析

### 4.1 连段与SA的联动触发

连段中通过 `tickAction` 调用 `WingToTheFuture.WingToTheFuture()` 的触发条件：

- **地面模式**：在第9帧直接召唤羽翼幻影剑，羽翼规模基于玩家经验等级
- **滑翔模式**：不直接召唤幻影剑，改为赋予 `COMET_ELYTRA` 效果

### 4.2 `COMET_ELYTRA` 彗星滑翔效果

文件位置：[`CometElytraEffect.java`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/potioneffect/CometElytraEffect.java)

注册于 [`FDPotionEffects.java:25-26`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDPotionEffects.java:25)

```java
public static final RegistryObject<MobEffect> COMET_ELYTRA = MOB_EFFECTS.register("comet_elytra",
    CometElytraEffect::new);
```

**效果机制**（第21-52行）：

- 仅在玩家处于鞘翅滑翔状态（`isFallFlying()`）时生效
- 每 tick 根据玩家视线方向施加推力：
  - `thrust = 0.06 + amplifier * 0.02`
  - 水平推力：`look.x * thrust`, `look.z * thrust`
  - 垂直推力：`look.y * thrust * 0.5`（减半，防止过度上升）
- 最大速度限制：`2.5 + amplifier * 0.3`
- 视觉效果：渐变色的 `DustColorTransitionOptions` 粒子

### 4.3 `ImmortalSoul` 不朽之魂药水效果

文件位置：[`ImmortalSoulEffect.java`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/potioneffect/ImmortalSoulEffect.java)

注册于 [`FDPotionEffects.java:21-22`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDPotionEffects.java:21)

- 这是一个纯标识性效果（`MobEffectCategory.BENEFICIAL`，颜色 `0xFFD700` 金色）
- 用于提示玩家"不朽之魂已触发"
- 实际逻辑在 `ChikeFlareEffects.java` 的 `OnDeath` 事件中处理

### 4.4 `DimensionBreakEffect` 次元崩坏效果

文件位置：[`DimensionBreakEffect.java`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/potioneffect/DimensionBreakEffect.java)

- 有害效果（`HARMFUL`，紫色 `0x8A2BE2`）
- **每级每秒削减1%最大生命值**（独立乘区 `MULTIPLY_TOTAL`），最大99%
- 效果移除时清除减益

---

## 维度五：特殊效果机制分析

### 文件位置

[`ChikeFlareEffects.java`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/chikeflare/ChikeFlareEffects.java)

### SE注册信息

来自 [`FDSpecialEffectsRegistry.java:14-21`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSpecialEffectsRegistry.java:14)：

| SE               | 注册名          | 稀有度 | 是否诅咒 | 是否隐藏 | 等级 |
| ---------------- | --------------- | ------ | -------- | -------- | ---- |
| **ImmortalSoul** | `immortal_soul` | 5      | false    | false    | 1    |
| **SoulShield**   | `soul_shield`   | 15     | false    | false    | 3    |
| **TyrantStrike** | `tyrant_strike` | 80     | false    | false    | 1    |
| **CheatRumble**  | `cheat_rumble`  | 800000 | false    | false    | 1    |

---

### 5.1 `SoulShield`（灵魂屏障）- 自动防反机制（第38-93行）

#### 攻击阶段触发 - `OnBypassAttack(LivingAttackEvent)`（第39-73行）

在玩家受到攻击时触发自动防反：

```java
float chargeRatio = (float) fdState.getSpecialCharge() / fdState.getMaxSpecialCharge();
float counterChance = Math.min(95.0f, 5.0f + chargeRatio * 90.0f);
```

**判定流程**：

1. **条件**：玩家主手持有奇克芙蕾雅且拥有 `SoulShield` SE
2. **概率计算**：`5% + 充能比 × 90%`，最大值 **95%**
   - 满充能（1000/1000）时：`5% + 1.0 × 90% = 95%`
   - 空充能时：`5%`
3. **消耗**：当前 `Soul` 的 5%（最少1点）
4. **成功反击**：
   - 调用 `AddonSlashUtils.doAddonSlash()` 释放自动寻敌拔刀剑气，颜色 `0x00FFFF`（青色）
   - 伤害倍率：`0.2 × consumeAmount`
   - 击退：`KnockBacks.cancel`（无击退）
   - 吸收伤害：`player.setAbsorptionAmount(min(current + 伤害量, 20))`
   - **取消原伤害**：`event.setCanceled(true)`

#### 伤害阶段补救 - `OnBypassAttack(LivingDamageEvent)`（第76-93行）

当防反概率判定失败时（玩家实际受到了伤害）：

```java
int chargeAmount = (int) Math.ceil(event.getAmount());
CapabilityUtils.addSpecialCharge(fdState, chargeAmount);
event.setAmount(Math.min(event.getAmount(), 5));
```

- **锁伤**：将实际受到的伤害强制降低至最多 **5点**
- **反馈充能**：受到的伤害值转化为 `Soul` 能量补充
- **设计意义**：防反失败不会导致灾难性后果，反而为下一次防反积累概率

---

### 5.2 `ImmortalSoul`（不朽之魂）- 致死保护（第96-120行）

#### 触发事件 - `OnDeath(LivingDeathEvent)`（第97-120行）

```java
if (!CapabilityUtils.tryConsumeProudSoul(state, 1000, player, null)) {
    return;
}
```

**触发条件**：

1. 玩家受到致死伤害
2. 玩家持有奇克芙蕾雅且拥有 `ImmortalSoul` SE
3. 消耗 **1000 Proud Soul（耀魂值）**

**触发效果**：
| 效果 | 详情 |
|------|------|
| **取消死亡** | `event.setCanceled(true)` |
| **永久增强** | `state.setBaseAttackModifier(baseattack + 0.67f)` — 永久增加0.67基础攻击修正 |
| **Soul充能** | `addSpecialCharge(fdState, player.getMaxHealth() * 5)` — 补充海量Soul |
| **生命恢复** | `player.setHealth(player.getMaxHealth() / 2.0F)` — 回复至50%血量 |
| **净化** | `player.removeAllEffects()` — 清除所有负面效果 |
| **增益** | 生命恢复IV（6秒）+ 抗性提升IV（6秒） |

**设计意义**：每次触发永久提升面板，意味着该刀在高压环境下越战越强。

---

### 5.3 `TyrantStrike`（暴君打击）- 处决长枪（第123-223行）

#### 命中触发 - `OnHit(SlashBladeEvent.HitEvent)`（第124-138行）

每次普通攻击命中敌人时触发。

#### 长枪召唤 - `spawnTyrantStrikePhantomSword()`（第183-223行）

```java
ss.setDamage(target.getMaxHealth() / 4);  // 目标最大生命值的25%
ss.setSpeed(5);
ss.setStandbyMode(EntityFDPhantomSword.StandbyMode.WORLD);
ss.setMovingMode(EntityFDPhantomSword.MovingMode.SEEK);
ss.setSeekAngle(36);
ss.setScale(target.getBbHeight());
```

**长枪属性**：

| 属性         | 值                                        |
| ------------ | ----------------------------------------- |
| **实体类型** | `EntityFDSpearPhantomSword`（长枪幻影剑） |
| **伤害**     | **目标最大生命值的25%**                   |
| **速度**     | `5`（高速）                               |
| **待命模式** | `WORLD`（世界绑定，在目标周围生成）       |
| **追踪模式** | `SEEK`（基础追踪）                        |
| **追踪角度** | `36°`（高机动性）                         |
| **缩放**     | 匹配目标身高                              |
| **颜色**     | 剑自身的特效色（`state.getColorCode()`）  |
| **延迟**     | 100 tick后自动消失                        |
| **拖尾**     | 开启                                      |

**生成位置**（第188-193行）：

- 在目标周围30格半径的随机球形位置生成
- 朝向目标方向

**视觉特效**（第197-201行）：

- 在生成点产生金色（`0xFFFF00`）的扩散环粒子

**设计意义**：每刀命中触发一次25%最大生命值的斩杀，配合大量幻影剑（如SA的96把剑）可以瞬间打出惊人的累积伤害。

---

### 5.4 `CheatRumble`（欺诈轰鸣）- 设定词缀

注册信息（[`FDSpecialEffectsRegistry.java:20-21`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSpecialEffectsRegistry.java:20)）：

```java
public static final RegistryObject<SpecialEffect> CheatRumble = SPECIAL_EFFECT.register("cheat_rumble",
    () -> new FDSpecialEffectBase(800000, false, false, 1));
```

- **稀有度 `800000`** — 极高的稀有度数值，标志其为"规格外"词缀
- 在 `ChikeFlareEffects.java` 中**没有直接的事件监听**
- 其实际功能体现在 `OnElytraClashBlock` 中，通过 `CheatRumble` 的存在性检查来判断是否触发彗星猛击效果（虽然该方法检查的是 `COMET_ELYTRA` 效果而非直接检查 `CheatRumble`）
- **设计意义**：剧情/传说层面的破格标识，象征该刀"作弊级"的能力

---

### 5.5 彗星猛击 - `OnElytraClashBlock(LivingHurtEvent)`（第141-181行）

这是 `CheatRumble` 的实际功能体现，与 `COMET_ELYTRA` 效果联动。

#### 触发条件（第143-148行）

```java
if (!event.getSource().is(DamageTypes.FLY_INTO_WALL) && !event.getSource().is(DamageTypes.FALL))
    return;
if (!entity.hasEffect(FDPotionEffects.COMET_ELYTRA.get()))
    return;
```

1. 玩家受到**撞击墙壁**（`FLY_INTO_WALL`）或**坠落**（`FALL`）伤害
2. 玩家拥有 `COMET_ELYTRA` 效果（即之前通过滑翔模式SA获得）

#### 伤害计算（第156-157行）

```java
weaponDamage = (ctx.state.getBaseAttackModifier() + ctx.state.getAttackAmplifier()) * 10f;
```

- 武器伤害 = `(基础攻击修正 + 攻击增幅) × 10`

#### 核爆效果（第159-181行）

| 效果         | 详情                                                              |
| ------------ | ----------------------------------------------------------------- |
| **爆炸半径** | `15` 格                                                           |
| **伤害公式** | `目标最大生命值 × 10% + 本次撞击伤害 + weaponDamage`              |
| **伤害类型** | **次元伤害**（`FDDamageSource.DIMENSION`）                        |
| **范围**     | 15格内所有敌人                                                    |
| **追加效果** | 每个被波及的敌人都触发一次 `spawnTyrantStrikePhantomSword()`      |
| **视觉**     | 30个爆炸粒子（`ParticleTypes.EXPLOSION_EMITTER`）散布在爆炸区域内 |
| **环状粒子** | 金色（`0xFFFF00`）扩散环，半径15格                                |
| **音效**     | 通用爆炸音效                                                      |
| **效果移除** | 触发后移除玩家的 `COMET_ELYTRA` 效果                              |

---

### 5.6 `DamageConverterEvent` 中 `dimension` 伤害类型处理

文件位置：[`DamageConverterEvent.java`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/globalevent/DamageConverterEvent.java)

#### 伤害替换机制 - `OnSlash(DoSlashEvent)`（第50-68行）

当奇克芙蕾雅的 `specialAttackEffect` 为 `"dimension"` 时：

```java
String fdDamageType = fdState.getSpecialAttackEffect();
if (fdDamageType != null && !fdDamageType.equals("Null")) {
    DamageSource fds = FDDamageSource.getEntityDamageSource(livingEntity.level(),
        FDDamageSource.fromString(fdDamageType), livingEntity);
    FDAttackManager.areaAttackWithSource(event.getUser(), KnockBacks.cancel.action,
        (float) event.getDamage(), true, true, false, null, fds);
    event.setDamage(0d);
}
```

- 将拔刀剑架势动作中的**原始伤害归零**
- 使用 `FDAttackManager.areaAttackWithSource()` 重新施加同等数值的区间攻击
- 伤害类型替换为 **次元伤害（`fantasydesire:dimension`）**

#### 次元伤害的定义

在 [`FDDamageSource.java:23,38`](/src/main/java/tennouboshiuzume/mods/FantasyDesire/damagesource/FDDamageSource.java:23) 中注册：

```java
public static final ResourceKey<DamageType> DIMENSION = register("dimension");
// bootstrap:
context.register(DIMENSION, new DamageType(FantasyDesire.MODID + ".dimension", 0f));
```

- 伤害类型ID：`fantasydesire:dimension`
- 无视难度系数（`0f`）

#### 次元伤害在伤害管道中的处理

在 `DamageConverterEvent` 的 `OnHurt(LivingHurtEvent)`（第91-163行）和 `OnDamage(LivingDamageEvent)`（第167-237行）中：

**次元伤害没有直接的额外修饰逻辑**——这意味着它作为"纯净"的自定义伤害类型，不受任何默认伤害修饰器影响，直接造成全额伤害。

相比之下，其他伤害类型（如 `ECHO`、`RESOLUTION`、`WRATH` 等）都有各自的修饰逻辑，而 `dimension` 作为奇克芙蕾雅的默认攻击特效，保持了伤害的纯粹性和穿透性。

---

## 核心玩法流派总结

### 1. 设计理念

奇克芙蕾雅的设计理念是 **"机制碾压 > 数值碾压"**。它以极低的基础攻击修正（0.2F）为代价，换取了四个强力特殊效果和一个高复杂度SA技能的协同。这是一把需要理解其机制循环才能发挥全部实力的"技术型"神兵。

### 2. 核心玩法循环

```
[积累Soul能量] → [触发SoulShield防反] → [消耗Soul]
       ↑                                         |
       |                                         ↓
  [ImmortalSoul免死] ← [遭受致死伤害] ← [被攻击命中]
       |                                         |
       |              [累积Soul + 永久增伤]       |
       |                                         |
       +──────────────── ← ──────────────────────+

[鞘翅滑翔] → [释放SA(滑翔模式)] → [获得COMET_ELYTRA]
                                   ↓
                       [撞击地面/墙壁触发核爆]
                                   |
                                   ↓
                [15格范围10%最大生命+武器伤害×10]
                                   |
                                   ↓
                [对每个敌人触发TyrantStrike长枪]
                                   |
                                   ↓
                   [每个长枪造成25%最大生命值]
```

### 3. 三种主要流派

#### 流派一：碰瓷防反流

- **核心**：`SoulShield` 的95%防反概率
- **打法**：主动冲入敌阵，利用高概率自动格挡触发剑气反击，同时通过吸收伤害积累护盾
- **优势**：几乎不吃操作，站撸一切
- **条件**：需要维持高 `Soul` 充能

#### 流派二：天降核弹流

- **核心**：`COMET_ELYTRA` + `CheatRumble` 核爆 + `TyrantStrike` 处决
- **打法**：滑翔至高空，释放SA获得 `COMET_ELYTRA` 加速，然后撞击地面触发核爆清场
- **优势**：瞬间爆发极高，15格半径AOE + 每个目标25%最大生命值追加
- **条件**：需要鞘翅和足够的高度

#### 流派三：越战越强流

- **核心**：`ImmortalSoul` 的永久面板增长
- **打法**：在高压环境中故意触发免死，积累基础攻击修正
- **优势**：每次触发 `ImmortalSoul` 增加 `0.67F` 基础攻击，多次触发后面板会远超常规武器
- **条件**：需要消耗1000 Proud Soul/次

### 4. 协同联动设计

| 联动组件                            | 交互方式                                    | 效果               |
| ----------------------------------- | ------------------------------------------- | ------------------ |
| `SoulShield` ←→ `Soul` 能量         | 消耗Soul触发防反，失败时受伤补充Soul        | 自平衡的能量循环   |
| `ImmortalSoul` → `Soul` 能量        | 免死时补充 `player.getMaxHealth() × 5` Soul | 紧急充能           |
| `ImmortalSoul` → 基础攻击修正       | 永久增加 `0.67F`                            | 越战越强           |
| `SoulShield` → 吸收护盾             | 成功格挡将伤害转化为吸收生命                | 滚雪球式防御       |
| `COMET_ELYTRA` → `CheatRumble`      | 滑翔撞击触发核爆                            | 滑翔→坠落→核爆     |
| `CheatRumble` → `TyrantStrike`      | 核爆波及的每个敌人触发长枪                  | AOE + 单体斩杀联动 |
| `TyrantStrike` ←→ `WingToTheFuture` | SA的96把剑 + 每刀触发长枪                   | 数百次伤害判定     |
| `dimension` 伤害类型                | 无视护甲和伤害修饰的特殊类型                | 保证伤害穿透性     |

### 5. 战斗节奏

```
Phase 1: 起手
  - 积累经验等级（提高SA翼数）
  - 积累Soul能量（提高SoulShield概率）

Phase 2: 压制
  - 使用SA（地面模式）召唤96把幻影剑压制战场
  - 每刀命中触发TyrantStrike长枪斩杀
  - SoulShield自动防御反击

Phase 3: 终结（滑翔模式）
  - 使用SA（滑翔模式）获得COMET_ELYTRA
  - 加速撞击地面触发核爆
  - 核爆后TyrantStrike处决幸存者

Phase 4: 容错
  - 死亡触发ImmortalSoul免死
  - 永久增长面板 + 满Soul充能
  - 回到Phase 1或2继续战斗
```

---

## 附：关键代码引用索引

| 内容               | 文件                                    | 行号    |
| ------------------ | --------------------------------------- | ------- |
| 注册入口           | `FantasySlashBladeBuiltInRegistry.java` | 42-78   |
| SA注册             | `FDSlashArtRegistry.java`               | 12-13   |
| SA核心逻辑         | `WingToTheFuture.java`                  | 30-92   |
| 转化逻辑           | `WingToTheFuture.java`                  | 95-111  |
| 防伪检查           | `WingToTheFuture.java`                  | 23-27   |
| 连段入口           | `FDCombo.java`                          | 37-59   |
| 连段转化           | `FDCombo.java`                          | 61-68   |
| 连段地面           | `FDCombo.java`                          | 70-83   |
| 连段滑翔           | `FDCombo.java`                          | 85-98   |
| 连段收尾           | `FDCombo.java`                          | 100-108 |
| SoulShield防反     | `ChikeFlareEffects.java`                | 39-73   |
| SoulShield锁伤     | `ChikeFlareEffects.java`                | 76-93   |
| ImmortalSoul免死   | `ChikeFlareEffects.java`                | 96-120  |
| TyrantStrike命中   | `ChikeFlareEffects.java`                | 123-138 |
| TyrantStrike长枪   | `ChikeFlareEffects.java`                | 183-223 |
| CheatRumble核爆    | `ChikeFlareEffects.java`                | 141-181 |
| 伤害类型替换       | `DamageConverterEvent.java`             | 50-68   |
| DIMENSION定义      | `FDDamageSource.java`                   | 23, 38  |
| SE注册             | `FDSpecialEffectsRegistry.java`         | 14-21   |
| COMET_ELYTRA效果   | `CometElytraEffect.java`                | 15-52   |
| COMET_ELYTRA注册   | `FDPotionEffects.java`                  | 25-26   |
| ImmortalSoul效果   | `ImmortalSoulEffect.java`               | 6-11    |
| ImmortalSoul注册   | `FDPotionEffects.java`                  | 21-22   |
| DimensionBreak效果 | `DimensionBreakEffect.java`             | 12-57   |
| 灵魂幻影剑实体     | `EntityFDSoulPhantomSword.java`         | 11-28   |
| 幻影剑基类         | `EntityFDPhantomSword.java`             | 58-1260 |
