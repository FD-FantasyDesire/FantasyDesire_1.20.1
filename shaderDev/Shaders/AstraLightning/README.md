# AstraLightning 星座闪电

正式粒子为 `fantasydesire:astra_lightning`。一份参数、一个粒子实例和一条固定的三维折线完成整个效果；起点、终点及每个折点都绘制面向相机的十字星光。线条由近白细核、彩色包覆与柔和辉光组成，星芒具有收尖的四条主光刺与较弱的对角闪光。节点不随时间移动，生命期内只改变亮度与星芒尺寸。

## Java 调用

```java
ParticleUtils.spawnAstraLightning(level,
        new AstraLightningParticleOptions(start, end, 0x8000FF, 9, 42L));
```

起终点是绝对世界坐标。生成坐标始终是 `start`；不使用起终点的中点，也不读取速度作为终点。上述入口可在服务端发送或客户端本地生成，调用方应选择一侧，避免重复出生。

完整构造器：

```java
new AstraLightningParticleOptions(start, end, color, thickness, lifetime,
        alpha, fade, randomness, maxEndpoints, starRadius, seed);
```

| 参数 | 含义与范围 | 简化构造器默认值 |
| --- | --- | --- |
| `start` / `end` | 世界坐标，分量必须有限且绝对值不超过 30000000 | 必填 |
| `color` | Java 整数 `0xRRGGBB`；命令与 Codec 使用十六进制字符串 | 必填 |
| `maxEndpoints` | 总节点上限 **2–33，包含起点和终点** | 必填 |
| `seed` | 有符号 64 位整数；网络完整传递，同参数同种子同路径 | 必填 |
| `thickness` | 线条核心尺度，单位格，0.001–4 | 0.05 |
| `lifetime` | 生命周期，单位 tick，1–1200；20 tick 为一秒 | 20 |
| `alpha` | 整体亮度系数，0–1 | 1 |
| `fade` | `true` 在生命期后段渐隐；`false` 保持后在末尾两 tick 收光 | true |
| `randomness` | 折点横向扰动尺度，单位格，0–64；0 为直线 | 0.7 |
| `starRadius` | 节点星芒尺度，单位格，0.01–8；粗线会适当放大星芒 | 0.22 |

Java／网络构造对有限但超出范围的参数钳制，对非有限参数和非法坐标拒绝构造。Codec 对范围错误返回解析错误。`color` 的高位不作为透明度；透明度由 `alpha` 决定，`0x000000` 不发光。

节点数量随长度与星芒尺寸自动减少，避免短线挤满星光；最大端点数为 2 时是一条直线和两颗端点星。起终点距离小于 `0.000001` 格时合并为起点的一颗星。折线由 Java 使用固定种子生成一次，相机旋转只改变承载光带与星芒的朝向。

每个折点在起终点连线的法平面内独立采样方向，绕轴角度均匀分布，偏移幅度向首尾收拢；第一折和后续折点都不再按序号固定左右交替。二维预览对应为各折点独立随机选侧。同参数同种子仍可复现，但修正方向采样后，既有种子生成的形状会改变。

原 `ParticleUtils.AstraLightningParticles(...)` 保留服务端调用方式。旧签名把 `maxSegments + 1` 转为总节点数，旧 `ringRadius` 的绝对值作为星芒半径；增加的重载以 `maxEndpoints, starRadius, seed` 结尾。无星之夜连锁攻击与虚空转化已改用显式种子重载，不再递归拼接 GlowingLine 和 SpreadingRing。

## 命令与 Codec

粒子参数顺序与完整 Java 构造器不同：常用参数排在前面。

```text
/particle fantasydesire:astra_lightning sx sy sz ex ey ez 0xRRGGBB maxEndpoints seed thickness lifetime alpha fade randomness starRadius [原版粒子位置、偏移、速度、数量、模式]
```

在 `(0, 65, 0)` 至 `(8, 68, 0)` 生成紫色星座闪电：

```mcfunction
particle fantasydesire:astra_lightning 0 65 0 8 68 0 0x8000FF 9 42 0.05 20 1 true 0.7 0.22 0 65 0 0 0 0 0 1 force
```

选项内部坐标不支持 `~`／`^`；命令末尾的原版生成位置也应设为起点，以匹配原版距离筛选。数量填 1，一颗粒子就是整条效果。工具方法的发送位置也是起点，发送半径为 `64 + 起终点距离`；原版客户端对强制粒子的距离限制仍然以生成位置为中心。

直接使用本选项 Codec 的最小 JSON：

```json
{
  "start": [0.0, 65.0, 0.0],
  "end": [8.0, 68.0, 0.0],
  "color": "0x8000FF",
  "maxEndpoints": 9,
  "seed": 42
}
```

## 基础预览

在 VSCode GLSL Canvas 打开 `main.frag`，运行 **Show GLSL Canvas**。修改顶部 `START`、`END`、`COLOR = 0xRRGGBB`、`MAX_ENDPOINTS`、`SEED` 等常量即可预览；`u_time` 单位为秒，效果按完整生命周期循环，中间留出空白间隔。

这个入口是自包含的 WebGL 1 / GLSL ES 1.00 二维预览，无纹理。短边归一化为 `[-1,1]`，循环硬上限 33。预览用浮点哈希生成二维路径，`SEED` 建议为绝对值小于 16777216 的整数；不承诺与正式 Java 64 位种子的三维节点逐点对应。颜色解包与较细的光刺需要片元 `highp` 才能保持完整精度。

## 正式管线与验证

- `AstraLightningParticleOptions` 处理命令、网络与 Codec；`AstraLightningPath` 生成相对起点的稳定节点。
- `AstraLightningParticle` 提交有限线段光带与节点星芒，所有实例共用一个渲染类型。最多 32 个线段方片和 33 个星芒方片，无每片元节点遍历，也不为每实例创建渲染类型或上传整组节点 uniform。
- 正式资源为 `src/main/resources/assets/fantasydesire/shaders/core/fd_astra_lightning.{json,vsh,fsh}`，GLSL 150，顶点格式 `POSITION_COLOR_TEX`。UV 区分线段与星芒；RGB 与生命期亮度通过顶点颜色传递。时间只在 Java 按 `age + partialTick` 的 tick 值计算，不使用 `GameTime`。
- RGB 使用 `ONE / ONE` 加法，目标 alpha 保持原值；开启深度测试、关闭深度写入。星芒和线条辉光在方片边界前衰减，裁剪盒覆盖完整三维路径与最大星芒尺寸。没有场景纹理、隐式 Bloom 或全屏后处理。
- 通过 `FDShaderHandler` 注册、getter 和加载状态检查接入，每帧重新获取 shader，兼容资源重载。此目录的 `main.frag` 不由 Minecraft 加载。

本次检查：Java 17 的 `compileJava processResources`；命令／网络／Codec 往返、十六进制颜色、64 位种子、2–33 节点上限、短线／重合／竖直路径和非法浮点输入；通过 Minecraft `ShaderInstance` 加载正式 JSON 与 GLSL 150，并使用实际粒子输出的网格在 NVIDIA RTX 3060 Laptop GPU 的 OpenGL 3.2 上离屏渲染，检查配色、首尾星芒和完整生命期。未进行游戏场景内的地形遮挡、多人收包、F3+T 操作或大量并发性能测量。

方向采样修正的回归检查：按 `astraTails` 的 4 节点、扰动 2、星芒半径 0.5 参数，分别使用 10000 个连续种子和 10000 个随机 long 种子。连续种子下第一折正侧比例从 100% 变为 50.02%，相邻折点同侧比例从 0% 变为 49.89%；随机 long 种子下对应为 50.47% 和 49.69%。固定首尾、同种子复现、零扰动、2–33 节点上限及短线／重合／竖直路径检查通过，Java 17 `compileJava` 与基础预览 GLSL ES 1.00 编译通过；本次未在游戏内复测飞行尾迹。
