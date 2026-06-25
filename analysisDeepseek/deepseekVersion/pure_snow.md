# 纯净虹光（PureSnow / 纯白之雪）拔刀剑深度分析

> **分析版本：** DeepSeek V4 Flash  
> **分析日期：** 2026-06-23  
> **对应刀名：** [`pure_snow`](src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:356) — "纯净虹光" / "纯白之雪"

---

## 维度一：基础属性（白虹之基）

### 1.1 注册信息

| 属性                 | 值                                                                                                                      |
| -------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| **注册名**           | [`pure_snow`](src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:356) |
| **完整 ID**          | `fantasydesire:pure_snow`                                                                                               |
| ** Translation Key** | `item.fantasydesire.pure_snow`                                                                                          |
| **特效色**           | `0xFFFFFF`（纯白）                                                                                                      |
| **模型**             | [`puresnow.obj`](src/main/resources/assets/fantasydesire/models/puresnow.obj)                                           |
| **贴图**             | [`puresnow.png`](src/main/resources/assets/fantasydesire/models/puresnow.png)                                           |
| **携带类型**         | `CarryType.RNINJA`（忍者背持）                                                                                          |
| **特殊类型**         | `"PureSnow"`（无特殊能量系统）                                                                                          |

### 1.2 战斗属性

| 属性         | 值                                                                                 |
| ------------ | ---------------------------------------------------------------------------------- |
| **基础攻击** | `2.8F`（全 FantasyDesire 刀中**最低**的基础攻击）                                  |
| **最大伤害** | `777`（中等偏上，象征"七"之含义）                                                  |
| **刀剑类型** | `SwordType.BEWITCHED`（妖刀）                                                      |
| **特殊能量** | 无（`FantasyDefinition` 中无 `specialChargeName`，仅有 `specialType: "PureSnow"`） |

### 1.3 附魔配置（7种附魔 I 级）

PureSnow 拥有全 FantasyDesire 中最**多样化**的附魔组合——7种不同附魔各 I 级：

| 附魔     | 注册名                                                                                                                           | 等级 |
| -------- | -------------------------------------------------------------------------------------------------------------------------------- | ---- |
| 力量     | [`POWER_ARROWS`](src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:379)       | I    |
| 亡灵杀手 | [`SMITE`](src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:381)              | I    |
| 节肢杀手 | [`BANE_OF_ARTHROPODS`](src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:383) | I    |
| 锋利     | [`SHARPNESS`](src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:385)          | I    |
| 穿刺     | [`PIERCING`](src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:390)           | I    |
| 时运     | [`BLOCK_FORTUNE`](src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:392)      | I    |
| 穿刺(MC) | [`IMPALING`](src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java:395)           | I    |

> **设计意图：** 7种附魔对应"七"之主题（7色彩虹、7宗罪、7刃剑），每样一点但广泛覆盖各类战斗场景。虽然等级只有 I，但为后续伤害类型转换提供了多样化的附魔基础。

### 1.4 技能配置

| 技能类型 | 注册名                                                                                                   | 对应类/效果                                                                                   |
| -------- | -------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| **SA**   | [`RAINBOW_STAR`](src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSlashArtRegistry.java:14)      | [`RainbowStar`](src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/RainbowStar.java) |
| **SE**   | [`RainbowFlux`](src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSpecialEffectsRegistry.java:30) | 彩虹通量（持续变色 + 伤害类型轮换）                                                           |
| **SE**   | [`ColorFlux`](src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSpecialEffectsRegistry.java:34)   | 色彩通量（辅助/协同 SE）                                                                      |

---

## 维度二：SA — RainbowStar（虹光星雨）

### 2.1 SA 注册信息

在 [`FDSlashArtRegistry`](src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSlashArtRegistry.java:14) 中注册：

```java
public static final RegistryObject<SlashArts> RAINBOW_STAR = FD_SLASH_ARTS.register("rainbow_star",
    () -> new FDSlashArts((e) -> FDCombo.RAINBOW_STAR.getId(), 1, true));
```

- **描述列**：1（单行描述）
- **替代名称**：`hasAltName = true`（说明有可选的替代显示名）
- **消耗**：**1 级专注**（第二个参数为 1）

### 2.2 连段路径（Combo Chain）

在 [`FDCombo`](src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java:181) 中定义的连段状态机：

```
RAINBOW_STAR (帧0-1, priority 50)
  │  ├─ AntiNTR 检测 → 不是 pure_snow 则终止
  │  └─ 通过 → RAINBOW_STAR_0
  │
  ▼
RAINBOW_STAR_0 (帧400-459, priority 50)
  │  ├─ 第2帧: doSlash(-30°, 0.1F) — 最小伤害挥砍（触发刀光架势）
  │  ├─ 第3帧: RainbowStar.RainbowStar() — 释放 21 把彩虹幻影剑
  │  └─ 超时 → RAINBOW_STAR_END
  │
  ▼
RAINBOW_STAR_END (帧459-488, priority 50)
     └─ 收刀动作 + 快速充能
```

> **AntiNTR 机制：** [`RainbowStar.AntiNTR()`](src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/RainbowStar.java:23) 通过 `SEConditionMatcher` 校验 `translationKey` 是否为 `"item.fantasydesire.pure_snow"`，防止其他刀"牛头人"这个 SA。

### 2.3 RainbowStar 核心逻辑

位置：[`RainbowStar.RainbowStar()`](src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/RainbowStar.java:29)

#### 伤害计算

```java
float baseModif = state.getDamage();      // 当前刀攻击力
float magicDamage = 1.0f + (baseModif / 2.0f);  // 魔法伤害公式
```

- 基础 2.8F 时，`magicDamage = 1.0 + 1.4 = 2.4`
- 伤害会随刀的攻击力成长而提升

#### 模式一：非潜行模式 — 天降彩虹雨

| 参数         | 值                                       |
| ------------ | ---------------------------------------- |
| **目标定位** | 玩家视线方向，**32 格射线追踪**找到落点  |
| **散布范围** | 水平面 **8 格半径**随机偏移（X/Z 各 ±8） |
| **发射高度** | **Y = +24**（从玩家位置向上 24 格）      |
| **角度随机** | 偏航 ±3° 高斯噪声，俯仰 ±3° 高斯噪声     |
| **位置随机** | 生成点 ±3 格高斯偏移（X/Y/Z）            |

> 每把剑瞄准落点方向，形成从天而降的彩虹剑雨，覆盖约 16×16 格的椭圆形区域。

#### 模式二：潜行模式 — 全方位彩虹喷泉

| 参数         | 值                                     |
| ------------ | -------------------------------------- |
| **发射高度** | **Y = +32**                            |
| **水平散布** | **360° 全向**（`random.nextInt(360)`） |
| **俯仰**     | 90°（正上方）+ 3° 高斯噪声             |
| **位置随机** | 生成点 ±3 格高斯偏移                   |

> 潜行时剑向正上方 360° 全方位散射，形成彩虹喷泉般的壮观效果。

#### 幻影剑属性

每把 [`EntityFDRainbowPhantomSword`](src/main/java/tennouboshiuzume/mods/FantasyDesire/entity/EntityFDRainbowPhantomSword.java) 的属性：

| 属性         | 值                    | 说明                                                             |
| ------------ | --------------------- | ---------------------------------------------------------------- |
| **数量**     | 21                    | 7 的倍数，对应七色 × 3                                           |
| **颜色**     | 彩虹渐变              | `ColorUtils.getSmoothTransitionColor(i, 21, true)` — 21 步色相环 |
| **速度**     | `5f`                  | 高速飞行                                                         |
| **缩放**     | `2f`                  | 大尺寸幻影剑                                                     |
| **拖尾**     | `hasTail = true`      | 带有彩虹粒子拖尾                                                 |
| **延迟**     | `200` ticks           | 10 秒后自动消失                                                  |
| **发射间隔** | `5 + i` ticks         | 逐把延时空投                                                     |
| **旋转**     | `random.nextInt(360)` | 每把剑随机旋转角度                                               |
| **待机模式** | `StandbyMode.WORLD`   | 世界坐标待机                                                     |

### 2.4 命中效果 — RainbowShockwave（彩虹冲击波）

当每把彩虹幻影剑命中实体或方块时，触发 [`RainbowShockwave()`](src/main/java/tennouboshiuzume/mods/FantasyDesire/entity/EntityFDRainbowPhantomSword.java:107)：

```
命中
  ├─ 生成 7 道彩虹斩击效果 (EntityFDSlashEffect) 环绕命中点
  │    ├─ 颜色: 7 色渐变 (i=0~6)
  │    ├─ 伤害: 0.5
  │    ├─ 击退: 取消
  │    └─ 静默: true
  │
  ├─ 范围近战攻击 (5 格半径)
  │    ├─ 伤害: 3.5
  │    └─ 排除施法者自身
  │
  └─ 调用 burst() 令幻影剑爆散消失
```

> 21 把剑如果全部命中，总爆炸伤害 = 21 × 3.5 = **73.5 范围伤害**（理论最大值），外加 21 × 0.5 = **10.5 斩击特效伤害**。

### 2.5 穿透保护 — makeSurePierce()

位置：[`EntityFDRainbowPhantomSword.makeSurePierce()`](src/main/java/tennouboshiuzume/mods/FantasyDesire/entity/EntityFDRainbowPhantomSword.java:78)

```java
if (getShooter().position().y + this.getDeltaMovement().length() <= this.position().y) {
    this.setNoClip(true);
}
```

- **天降保护：** 当剑的 Y 坐标高于或等于施法者 Y + 速度时，启用 `NoClip`
- 防止彩虹剑雨被洞穴天花板/树叶/建筑挡住
- 确保从天而降的剑能穿透地形命中目标

### 2.6 彩虹粒子拖尾

位置：[`playRainbowParticle()`](src/main/java/tennouboshiuzume/mods/FantasyDesire/entity/EntityFDRainbowPhantomSword.java:40)

- 在幻影剑飞行路径上插值生成 `DustParticleOptions` 粒子
- 粒子颜色 = 剑的当前彩虹色
- 粒子缩放 = 剑的缩放（2f）
- 轨迹分段 = 飞行速度（5 段/ tick）

> **注意：** 该方法的调用已被注释掉（第 36-37 行），可能暂时禁用或改用其他粒子系统。

### 2.7 后续效果 — RAINBOW_SEVEN_EDGE

释放技能后，玩家获得 **14 秒** 的 [`RAINBOW_SEVEN_EDGE`](src/main/java/tennouboshiuzume/mods/FantasyDesire/potioneffect/RainbowSevenEdgeEffect.java) 效果：

```java
player.addEffect(new MobEffectInstance(FDPotionEffects.RAINBOW_SEVEN_EDGE.get(), 20 * 14, 0));
```

详见维度四的分析。

---

## 维度三：连段系统 — RainBowStar Combo Chain 深度解析

### 3.1 完整的连段状态机

```
[玩家按下SA键]
     │
     ▼
RAINBOW_STAR (comboState)
  ├── 帧: 0-1
  ├── 优先级: 50
  ├── motionLoc: ExMotionLocation
  ├── next() → AntiNTR检查
  │     ├── 通过 → RAINBOW_STAR_0
  │     └── 失败 → none (终止)
  └── nextOfTimeout() → 同上
     │
     ▼
RAINBOW_STAR_0 (comboState)
  ├── 帧: 400-459 (60帧动画)
  ├── 优先级: 50
  ├── motionLoc: ExMotionLocation
  ├── next() → 15帧超时后 → none
  ├── nextOfTimeout() → RAINBOW_STAR_END
  ├── TickAction:
  │     ├── 第2帧: doSlash(-30°, Vec3.ZERO, false, false, 0.1F)
  │     │     └─ 最小伤害挥砍 → 触发刀光架势
  │     │        └─ 如果处于 RainbowSevenEdge 效果下 → 触发七刃剑追击
  │     └── 第3帧: RainbowStar.RainbowStar(player, blade)
  │           └─ 实际释放 21 把彩虹幻影剑
  └── HitEffect: StunManager::setStun
     │
     ▼
RAINBOW_STAR_END (comboState)
  ├── 帧: 459-488 (29帧收刀)
  ├── 优先级: 50
  ├── motionLoc: ExMotionLocation
  ├── next() → none
  ├── nextOfTimeout() → none
  └── TickAction:
        └── 第0帧: playQuickSheathSoundAction (收刀声效)
      releaseAction: releaseActionQuickCharge (快速充能)
```

### 3.2 关键时序分析

| 事件     | 帧  | 时间（@60fps） | 说明              |
| -------- | --- | -------------- | ----------------- |
| 起手     | 0-1 | ~0.017s        | 瞬间进入          |
| 挥砍触发 | 402 | ~6.7s          | 第2帧 doSlash     |
| 剑雨释放 | 403 | ~6.72s         | 第3帧 RainbowStar |
| 超时结束 | 459 | ~7.65s         | 进入收刀          |
| 收刀完成 | 488 | ~8.13s         | 收刀 + 快速充能   |

> **注意：** 挥砍在第2帧就触发，而幻影剑在第3帧释放，意味着 SA 的释放动画非常短，几乎瞬间完成动作后即可自由移动，幻影剑会自行空投攻击。

---

## 维度四：衍生效果 — RainbowSevenEdge（虹羽七刃剑）

### 4.1 效果基础信息

| 属性         | 值                                                                                                                                                                          |
| ------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **注册名**   | [`rainbow_seven_edge`](src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDPotionEffects.java:17)                                                                      |
| **效果类**   | [`RainbowSevenEdgeEffect`](src/main/java/tennouboshiuzume/mods/FantasyDesire/potioneffect/RainbowSevenEdgeEffect.java)                                                      |
| **类别**     | `MobEffectCategory.BENEFICIAL`（纯增益）                                                                                                                                    |
| **颜色**     | `0xFFFFFF`（纯白）                                                                                                                                                          |
| **图标**     | `textures/mob_effect/void_strike.png`                                                                                                                                       |
| **持续时间** | **14 秒**（20 ticks × 14）                                                                                                                                                  |
| **等级**     | 0（I 级）                                                                                                                                                                   |
| **免疫**     | 强制应用（[`DamageConverterEvent.onMobEffectApplicable`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/globalevent/DamageConverterEvent.java:316) 允许） |

### 4.2 七刃剑追击机制（核心玩法）

位置：[`PureSnowEffects.onSlash()`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/puresnow/PureSnowEffects.java:59)

**触发条件：**

1. 玩家持有 `pure_snow`
2. 玩家拥有 `RainbowFlux` SE
3. 玩家拥有 `RAINBOW_SEVEN_EDGE` 药水效果
4. 执行普通挥砍（`DoSlashEvent`）

**执行效果：**

```
触发挥砍
  │
  ├─ 1. 获取当前 RainbowFlux 的伤害类型 (fdDamageType)
  │
  ├─ 2. 构造自定义 DamageSource
  │     └─ FDDamageSource.getEntityDamageSource(lvl, fdDamageType, player)
  │
  ├─ 3. 释放 7倍伤害 AOE 攻击
  │     └─ FDAttackManager.areaAttackWithSource(
  │            user, KnockBacks.cancel, damage * 7, true, true, false, null, fds)
  │
  ├─ 4. 召唤 7 道彩虹斩击效果 (环绕玩家)
  │     ├─ 颜色: 七色渐变
  │     ├─ 伤害: 0 (纯视觉)
  │     ├─ 击退: 取消
  │     └─ 360° 环绕分布
  │
  └─ 5. 取消原始挥砍 (event.setCanceled(true))
```

> **核心机制：** 七刃剑将普通的拔刀剑挥砍**替换**为一次高额 AOE 伤害攻击，伤害类型随颜色同步轮换。这意味着在 14 秒内，每次左键挥砍都是带有七宗罪伤害类型的强力范围攻击。

### 4.3 伤害类型与效果联动

当七刃剑触发时，伤害类型由 [`RainbowFlux.updateEvent`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/puresnow/PureSnowEffects.java:33) 当前轮换到的 `fdDamageType` 决定。

每种伤害类型会触发 [`DamageConverterEvent.OnHurt`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/globalevent/DamageConverterEvent.java:91) 中的对应效果：

| 伤害类型             | 颜色阶段 | 特殊效果                                  |
| -------------------- | -------- | ----------------------------------------- |
| **Wrath（暴怒）**    | 红       | 目标每损失 1% 生命，伤害 +2%              |
| **Lust（色欲）**     | 橙       | **每次命中治疗攻击者 5% 伤害量**          |
| **Sloth（怠惰）**    | 黄       | 50% 概率施加缓慢 II + 虚弱 I（1 秒）      |
| **Gluttony（暴食）** | 绿       | 转化为饱食度（10%），溢出转伤害吸收       |
| **Gloom（忧郁）**    | 青       | 根据目标氧气值增伤（最高 2x），清空氧气   |
| **Pride（傲慢）**    | 蓝       | 根据攻击者生命值缩放（0x ~ 3x，满血最高） |
| **Envy（嫉妒）**     | 紫       | 护甲差每 1 点 +5% 伤害                    |

> **实战意义：** 七刃剑的 7 倍 AOE 配合伤害类型特效，使得每刀都带有双重视觉 + 机制效果——高额范围伤害 + 特定状态/回复/增伤。

---

## 维度五：特殊效果 — RainbowFlux & ColorFlux

### 5.1 RainbowFlux（彩虹通量）

注册信息：[`FDSpecialEffectsRegistry.RainbowFlux`](src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSpecialEffectsRegistry.java:30)

```java
public static final RegistryObject<SpecialEffect> RainbowFlux = SPECIAL_EFFECT.register("rainbow_flux",
    () -> new FDSpecialEffectBase(1, false, false, 2, true));
```

| 参数         | 值      | 含义                   |
| ------------ | ------- | ---------------------- |
| 专注消耗     | `1`     | 极低                   |
| 是否触发即死 | `false` | 否                     |
| 是否触发即杀 | `false` | 否                     |
| SE 槽占用    | `2`     | **占用 2 个 SE 槽**    |
| 隐藏效果     | `true`  | **不显示在 SE 列表中** |

#### 核心逻辑：双轮换系统

位置：[`PureSnowEffects.updateEvent()`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/puresnow/PureSnowEffects.java:33)

此事件**每 tick 执行**（每秒 20 次），实现颜色和伤害类型的同步轮换。

##### A. 颜色轮换

```java
int eachColorZone = 15;          // 每色停留 15 tick
int totalSteps = eachColorZone * 7;  // 总步数 = 105 tick (5.25 秒/周期)
int timeStep = (int)(tickCount % totalSteps);
int color = ColorUtils.getSmoothTransitionColor(timeStep, totalSteps, true);
state.setColorCode(color);
```

- **周期：** 5.25 秒完成一次完整色相环
- **算法：** HSV 色相环（饱和度 100%，亮度 100%）
- **效果：** 刀身特效颜色平滑遍历红→橙→黄→绿→青→蓝→紫

##### B. 伤害类型轮换

```java
int stepsPerType = totalSteps / 7;      // 15 步/类型
int damageTypeIndex = ((timeStep + stepsPerType / 2) * 7 / totalSteps) % 7;
fdState.setSpecialAttackEffect(damageTypes[damageTypeIndex]);
```

- **7 种伤害类型**（对应七宗罪）：`wrath, lust, sloth, gluttony, gloom, pride, envy`
- 每种伤害类型持续 **15 tick（0.75 秒）**
- 每 **0.75 秒**切换一次伤害类型
- 颜色和伤害类型同步循环

#### 视觉-伤害联动时间轴

```
时间(tick)  0    15    30    45    60    75    90    105
颜色       红    橙    黄    绿    青    蓝    紫    → 循环
伤害类型   wrath lust sloth gltn gloom pride envy  → 循环
```

> **设计理念：** 颜色即伤害——玩家通过刀身的颜色就能直观判断当前的伤害类型。

### 5.2 ColorFlux（色彩通量）

注册信息：[`FDSpecialEffectsRegistry.ColorFlux`](src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSpecialEffectsRegistry.java:34)

```java
public static final RegistryObject<SpecialEffect> ColorFlux = SPECIAL_EFFECT.register("color_flux",
    () -> new FDSpecialEffectBase(40, false, false, 1));
```

| 参数         | 值      | 含义     |
| ------------ | ------- | -------- |
| 专注消耗     | `40`    | 中等消耗 |
| 是否触发即死 | `false` | 否       |
| 是否触发即杀 | `false` | 否       |
| SE 槽占用    | `1`     | 1 槽     |

> **说明：** `ColorFlux` 目前没有对应的事件处理逻辑。它作为 PureSnow 的第二个 SE，可能：
>
> 1. 作为**被动加成**（如增加颜色轮换速度或额外效果）
> 2. 与未来更新中 RainbowFlux 产生**协同联动**
> 3. 为 PureSnow 提供额外的**专注消耗机制**（40 专注/次）

### 5.3 SE 配置总结

| SE          | 专注消耗 | 槽位 | 隐藏  | 功能                            |
| ----------- | -------- | ---- | ----- | ------------------------------- |
| RainbowFlux | 1        | 2    | ✅ 是 | 颜色+伤害类型双轮换，七刃剑核心 |
| ColorFlux   | 40       | 1    | ❌ 否 | 未知（协同/被动）               |

> **SE 占位分析：** RainbowFlux 占 2 槽 + ColorFlux 占 1 槽 = 共 3 槽。对于基础 SE 槽有限的拔刀剑来说，这意味着 PureSnow 几乎无法再安装其他 SE。

---

## 维度六：玩法流派总结

### 6.1 核心玩法循环

```
[持刀状态]
    │
    ├─ RainbowFlux 自动轮换颜色+伤害类型 (每 0.75s)
    │
    ├─ 使用 SA [释放 RainbowStar]
    │     ├─ 非潜行: 天降彩虹剑雨 (远程压制)
    │     ├─ 潜行: 彩虹喷泉 (近距离全向攻击)
    │     └─ 获得 14 秒 RainbowSevenEdge 效果
    │
    └─ RainbowSevenEdge [持续 14 秒]
          ├─ 每次挥砍 → 7 倍 AOE 伤害
          ├─ 伤害类型随颜色变化
          └─ 召唤 7 道彩虹斩击视觉特效
```

### 6.2 战斗定位

| 定位           | 评分       | 说明                                     |
| -------------- | ---------- | ---------------------------------------- |
| **远程压制**   | ⭐⭐⭐⭐⭐ | 21 把彩虹剑的广范围覆盖 + 穿透地形       |
| **爆发输出**   | ⭐⭐⭐⭐   | 七刃剑 7 倍 AOE + 彩虹冲击波叠加         |
| **持续输出**   | ⭐⭐⭐⭐⭐ | RainbowFlux 常驻，七刃剑 14 秒持续       |
| **生存能力**   | ⭐⭐       | 无防御类 SE，仅靠 Lust/Gluttony 伤害回血 |
| **操作复杂度** | ⭐⭐⭐⭐   | 需要管理 SA 冷却和七刃剑的 14 秒窗口     |
| **AOE 清场**   | ⭐⭐⭐⭐⭐ | 彩虹剑雨 + 七刃剑 AOE，群战利器          |

### 6.3 推荐战斗策略

1. **起手爆发：** 非潜行 SA → 21 把剑从天而降轰炸 → 获取 14 秒七刃剑
2. **七刃剑窗口：** 每 0.75 秒伤害类型变化，针对不同敌人选择最佳输出时机
   - **对高血量 BOSS：** Wrath 阶段（生命越低伤害越高）
   - **PVP：** Envy 阶段（护甲差增伤）、Pride 阶段（满血最大伤害）
   - **续航战：** Lust（吸血）、Gluttony（回饱食度）
3. **近身防身：** 潜行 SA → 360° 彩虹剑雨 → 全方位覆盖
4. **循环节奏：** 七刃剑结束 → 再次 SA → 重复爆发循环

### 6.4 优缺点分析

**优势：**

- 全 FantasyDesire 中**最华丽的视觉特效**（彩虹渐变 + 粒子拖尾 + 七色斩击）
- 伤害类型多样性极高，**几乎无免疫目标**
- 21 把幻影剑 + 穿透地形，**无视障碍物打击**
- 七刃剑期间**每秒 20 次颜色渐变**，视觉反馈极强

**劣势：**

- 基础攻击 **全 FD 最低**（2.8F），白板时近战刮痧
- 高度依赖 SA 启动七刃剑，**技能真空期输出乏力**
- SE 槽占用高（3 槽），**几乎没有自定义空间**
- `ColorFlux` 功能不明确，**存在感薄弱**
- 纯伤害型拔刀剑，**缺乏控制/防御/位移能力**

---

## 维度七：联动设计分析

### 7.1 内部联动

| 联动链条                | 说明                                       |
| ----------------------- | ------------------------------------------ |
| RainbowFlux → 七刃剑    | 伤害类型由 RainbowFlux 提供，二者绑定      |
| 颜色 → 伤害类型         | 视觉（刀身颜色）直接映射到机制（伤害类型） |
| SA → 七刃剑             | SA 是唯一获取七刃剑效果的途径              |
| 幻影剑命中 → 彩虹冲击波 | 单目标可叠加多次爆炸                       |
| AntiNTR 检测            | 防止其他刀使用 RainbowStar SA              |

### 7.2 与 FantasyDesire 全局系统的联动

| 系统                                                                                                                                 | 联动方式                                 |
| ------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------- |
| **[`DamageConverterEvent`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/globalevent/DamageConverterEvent.java)** | 七宗罪伤害类型的全局处理逻辑             |
| **[`FDDamageSource`](src/main/java/tennouboshiuzume/mods/FantasyDesire/damagesource/FDDamageSource.java)**                           | 自定义伤害来源系统                       |
| **[`ColorUtils`](src/main/java/tennouboshiuzume/mods/FantasyDesire/utils/ColorUtils.java)**                                          | HSV 色相环渐变工具，也用于其他彩虹色场景 |
| **[`FDAttackManager`](src/main/java/tennouboshiuzume/mods/FantasyDesire/utils/FDAttackManager.java)**                                | 自定义 AOE 攻击管理器                    |
| **[`FDTargetSelector`](src/main/java/tennouboshiuzume/mods/FantasyDesire/utils/FDTargetSelector.java)**                              | 范围目标选择（5 格半径）                 |
| **[`FDPotionEffects`](src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDPotionEffects.java)**                                 | 药水效果强制应用                         |

### 7.3 与其他拔刀剑对比

| 刀名          | 基础攻击 | 特色                    | 复杂度 | 视觉效果   |
| ------------- | -------- | ----------------------- | ------ | ---------- |
| **PureSnow**  | 2.8F     | 彩虹色 + 七宗罪伤害轮换 | 高     | ⭐⭐⭐⭐⭐ |
| CrimsonScythe | 4.5F     | 吸血 + 猩红之击         | 中     | ⭐⭐⭐     |
| OverCold_3    | 13.0F    | 冰霜进化 + 特殊能量     | 极高   | ⭐⭐⭐⭐   |
| TwinBlade     | 6.0F     | 双刀切换 + 双 SA        | 高     | ⭐⭐⭐⭐   |
| StarlessNight | 9.0F     | 虚空回响 + 永劫伤害     | 高     | ⭐⭐⭐⭐   |

### 7.4 可能的扩展方向

1. **ColorFlux 功能补全：** 当前 ColorFlux 没有具体实现，未来可添加：
   - 增加颜色轮换速度（双倍频率）
   - 释放 SA 时额外召唤彩虹爆炸
   - 增加七刃剑的持续时间或伤害倍率

2. **七宗罪伤害类型的深度联动：**
   - PureSnow 的攻击附带随颜色变化的附加效果
   - 特定颜色组合解锁隐藏效果

3. **进阶变形机制：**
   - 类似 OverCold 的进化系统
   - 纯白之雪 → 极光之虹（形态转变）

---

## 附录：关键源码索引

| 功能                     | 文件                                                                                                                                            | 行号    |
| ------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| 注册定义                 | [`FantasySlashBladeBuiltInRegistry.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/data/builtin/FantasySlashBladeBuiltInRegistry.java) | 355-396 |
| SA 核心逻辑              | [`RainbowStar.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/RainbowStar.java)                                              | 全文件  |
| SA 注册                  | [`FDSlashArtRegistry.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSlashArtRegistry.java)                                     | 14-15   |
| SA 连段                  | [`FDCombo.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDCombo.java)                                                           | 181-213 |
| 彩虹幻影剑               | [`EntityFDRainbowPhantomSword.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/entity/EntityFDRainbowPhantomSword.java)                 | 全文件  |
| 彩虹七刃剑效果           | [`RainbowSevenEdgeEffect.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/potioneffect/RainbowSevenEdgeEffect.java)                     | 全文件  |
| RainbowFlux + 七刃剑追击 | [`PureSnowEffects.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/effects/puresnow/PureSnowEffects.java)                | 全文件  |
| SE 注册                  | [`FDSpecialEffectsRegistry.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/init/FDSpecialEffectsRegistry.java)                         | 29-35   |
| 颜色工具                 | [`ColorUtils.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/utils/ColorUtils.java)                                                    | 全文件  |
| 伤害类型处理             | [`DamageConverterEvent.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/specialeffects/globalevent/DamageConverterEvent.java)           | 全文件  |
| 连段基类                 | [`ComboState.java`](deplib/SlashBlade_Resharped-master/src/main/java/mods/flammpfeil/slashblade/registry/combo/ComboState.java)                 | 28      |
| SA 基类                  | [`FDSlashArts.java`](src/main/java/tennouboshiuzume/mods/FantasyDesire/slasharts/FDSlashArts.java)                                              | 全文件  |
| 生成数据                 | [`pure_snow.json`](src/generated/resources/data/fantasydesire/fantasydesire/fantasyslashblade/pure_snow.json)                                   | 全文件  |

---

> **总结：** PureSnow（纯净虹光/纯白之雪）是一把以"彩虹"和"七"为主题的**纯伤害型法术拔刀剑**。其核心机制围绕 RainbowFlux 的颜色-伤害双轮换系统展开，通过 SA 启动七刃剑状态后将普通挥砍替换为 7 倍 AOE 攻击。21 把彩虹幻影剑的天降剑雨提供了强大的远程压制能力，而潜行模式的全方位散射则保证了近身防身能力。虽然基础攻击力是 FantasyDesire 中最低的，但通过七刃剑 7 倍伤害倍率和七宗罪伤害类型的多样性，弥补了面板上的不足。ColorFlux 的功能尚未完全实现，存在后续扩展的空间。
