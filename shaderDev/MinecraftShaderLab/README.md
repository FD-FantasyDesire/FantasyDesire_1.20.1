# Minecraft Shader Lab

面向 **Minecraft Java 1.20.1 / Forge core shader** 的独立桌面预览器。使用 Java 17 + LWJGL、OpenGL 3.2 Core，直接编译 `.vsh` / `.fsh` 的桌面 GLSL 150，并读取原有 shader JSON。无需启动 Minecraft、Forge、Gradle 或浏览器，也不做 GLSL ES 转译。

此工具位于主项目的 `shaderDev/MinecraftShaderLab/`，作为 shaderDev 的统一验证器独立运行，不加入 mod 的 Gradle 源码集或发布包。正式 shader 可直接从 mod 资源目录读取；预览工具不改写它们。主项目工作流与作品约定见 [shaderDev 说明](../README.md)。

## 启动

Windows x64，需要 JDK 17 和支持 OpenGL 3.2 的显卡驱动。本机已具备这些条件。

双击本目录的 **`run.cmd`**，或执行以下命令。本页命令均以 **`shaderDev/MinecraftShaderLab` 为工作目录**；从主项目根目录调用的方法见 [统一入口](../README.md#统一入口)。

```powershell
.\run.cmd

# 打开配套配置、shader JSON，或有唯一配套 JSON 的 fsh / vsh。
.\run.cmd -Shader .\examples\fantasy-frost.preview.json
.\run.cmd -Shader ..\..\src\main\resources\assets\fantasydesire\shaders\core\fd_void_flame.fsh
```

首次启动自动编译独立 Java 源码。依赖固定为 LWJGL 3.3.1、Gson 2.10.1 和 JOML 1.10.5，先复用本机 Gradle 缓存，缺失时下载到 `.deps/`；总计约 3.2 MB。之后无网络运行，源码未修改时跳过编译。工具默认限制 60 FPS，避免编辑时持续占满 GPU。

启动日志写入 `build/logs/launcher-*.log`；双击失败时错误窗口会保留，不会闪退。启动脚本已兼容 Windows PowerShell 5.1 的中文编码要求。

自动尝试读取本机 ForgeGradle 缓存或官方启动器的 **1.20.1 client.jar**，用于原版 `#moj_import` 与纹理。也可明确指定：

```powershell
.\run.cmd -MinecraftJar "D:\Minecraft\versions\1.20.1\1.20.1.jar"
```

工具不包含或分发 Minecraft 游戏文件。没有游戏资源时，内置示例使用生成的棋盘纹理；主动配置的纹理缺失会报错，不以替代图掩盖资源错误。

## 操作

启动后有两个独立窗口：OpenGL 画面和参数控制台。

- **打开文件 / 加载路径**：加载 `.json`、`.fsh`、`.vsh` 或 `.preview.json`。单独的阶段文件必须有唯一引用它的 JSON；多个程序共用阶段时请直接选择 JSON。
- **保存即重载**：每 0.4 秒检查实际用到的 JSON、shader、include 和显式纹理；编译或链接失败保留上一个有效程序并显示 GPU 日志，修复后自动恢复。
- **示例**：`surface` 方块、`entity` 实体、`sky` 天空盒、`depth` 双深度合成。`fantasy-*` 使用相对路径引用主项目 `src/main/resources` 中的正式三件套。
- 左键拖动画面环绕，滚轮缩放；空格暂停，`R` 重新加载，`Esc` 关闭。
- 时间支持暂停、按 1/20 秒步进、定位与速度调整。双击 uniform 数值编辑；向量和矩阵输入 JSON 数组。
- **保存预览配置**：保存实验对象、相机、深度状态和参数覆盖，不修改正式 shader JSON。GUI 覆盖的参数优先于时间绑定。
- **截图 PNG**：将实际离屏画面写入 `captures/`，不包含控制台。

## 新工作流

1. 直接编写 Minecraft core 三件套，使用 `#version 150`、`in/out` 与正式资源名称。
2. 打开 shader JSON；根据目标选择方块、天空盒、实体、平面或全屏 pass。
3. 写一份 `.preview.json` 描述 Java 渲染端本来需要提供的输入：顶点 UV 域、纹理、动态 uniform、混合以外的状态。
4. 保持预览器运行，在编辑器保存 shader/include 后直接看结果。
5. 接入游戏时沿用这些 shader 文件，在 Java 渲染器中对齐同一组输入和状态，完成实际世界中的最终验收。

已有 `main.frag` 不自动迁移。它们的时间、坐标和采样输入需要明确转换一次；之后可以只维护正式格式。

## 试验场景

| target | 绘制位置 | 默认用途 |
| --- | --- | --- |
| `blocks` | 地面、台阶、立柱的材质 | 表面着色、法线、纹理、顶点动画 |
| `sky` | 相机中心的六面立方体，去除视图平移 | 方向空间天空、程序化星空；可自由环视 |
| `entity` | 僵尸或苦力怕盒模型，有独立肢体动画及 UV 岛 | 表面侵蚀、能量纹路、实体附着效果 |
| `quad` | 世界空间平面，可朝向相机，UV 域可配置 | 粒子、裂痕、光束等承载网格 |
| `screen` | 全屏平面，读取场景快照 | 深度重建、地形覆盖、屏幕合成 |
| `volume` | 世界空间内向单位盒，读取场景快照 | 有界体积射线积分、实体与地形深度裁剪 |

渲染顺序：**天空 + 地形 → 地形颜色/深度 → 复制后绘制实体 → 完整场景颜色/深度 → 复制到输出并执行效果**。三个 framebuffer 独立，均为 RGBA8 + DEPTH24；缩放窗口时同步重建。双深度输入保留实体遮挡差异，避免读写同一附件。

## 预览配置

示例：同一份实体效果 shader，绑定秒制时间并叠加到原材质。

```json
{
  "shader": "assets/example/shaders/core/energy.json",
  "target": "entity",
  "entity": "zombie",
  "overlay": true,
  "resourceRoots": ["."],
  "state": {"depthTest": true, "depthWrite": false, "cull": true},
  "uniforms": {"Strength": 0.7},
  "bindings": {"EffectTime": "seconds"},
  "textures": {"Sampler0": "minecraft:textures/entity/zombie/zombie.png"},
  "camera": {"yaw": 35, "pitch": 25, "distance": 9, "fov": 60},
  "scene": {"terrain": true, "entity": true}
}
```

所有文件路径相对于配置文件。`resourceRoots` 指向包含 `assets/` 的目录；标准资源布局会自动推断根目录。资源按显式根目录、推断目录、游戏 JAR 的顺序查找。散装三件套支持同目录阶段及 include，正式集成时推荐标准 `assets/<namespace>/shaders/{core,include}` 结构。

`quad` 额外支持：

```json
"mesh": {"width": 6.4, "height": 2.8, "uv": [-1.6, -0.7, 1.6, 0.7], "billboard": true},
"scale": 1
```

`uv` 顺序为左下 U/V、右上 U/V；默认 `[0,0,1,1]`。这对把 UV 用作局部 SDF 坐标的效果尤其必要：正式 Java 上传的是 `[-1,1]` 或更大的域时，不能用默认纹理 UV 代替。

`quad` 的 `mesh.copies` 可设置 1–4096 个相同面片（默认 1），以一个 `glDrawArrays` 绘制，适合技能粒子批量压力测试。每片连续六顶点，可由 vertex shader 的 `gl_VertexID / 6` 解码槽号，自行计算位置、年龄和尺寸。公共工具不自动提供战斗实例数据或出生队列。多片批次首次绘制缓存 VAO/VBO，重载时重新创建；时间与 uniform 仍逐帧上传。copies 必须是整数，其他 target 不接受多片配置；GLSL 自行生成的位置仍需对齐 quad 的模型平移与 scale。

`mesh.billboard: true` 时可指定 `mesh.billboardMode: "vertical"`，只绕世界 Y 轴转向相机，保持竖直；默认 `"spherical"` 同时跟随俯仰。`quad` 默认在实体之后绘制；顶层 `quadStage: "before_entities"` 可验证实体之前的背景气场，此时效果遵守地形深度，而实体随后覆盖效果。`scene-color` / `scene-depth` 附件仍保存不含效果的完整场景，便于逐像素验证实体未被覆盖。该选项不提供额外场景纹理采样，也不改变其他 target 的顺序。

## Uniform 与纹理契约

`volume` 使用 `[-1,1]³` 局部坐标、内向三角形绕序，默认开启背面剔除，只提交射线出口面。`mesh.width / height / depth` 指定世界尺寸，均默认 2、范围 `(0,100]`；中心固定为 `(0,1.5,0)`，始终世界轴对齐，不受 `scale`、`billboard` 或 `quadStage` 影响。默认关闭硬件深度测试与写入；shader 自行用独立的 `@scene_depth` 截断积分，因此实体前方的体积可见、后方体积被遮挡，也可用 `@block_depth` 做对照。关闭剔除会重复累计前后面，不适合普通体积积分。近裁面、相机在盒内、采样步数与包围盒外归零均由效果 shader 处理；参考 [Shin](../Shin/README.md)。

`volumeScale` 默认 1，范围 `[0.25,1]`，支持 `volume` 和 `screen`。小于 1 时启用两个阶段：先在向上取整的缩小目标中绘制透明层，再在原分辨率绘制同一个出口盒或全屏平面。shader 必须声明 `RenderSize`（vec2，当前目标的像素尺寸）、`ResolvePass`（int，首次 0、合成 1），并绑定 `AuraSampler: "@volume_color"`。宿主在自定义 uniform 后上传这两个值；缩小阶段该 sampler 绑定独立白纹理，合成阶段绑定前次颜色，禁止 framebuffer 反馈。shader 负责深度感知重建和必要时的精确重算，使用预乘颜色及 `one / 1-srcalpha` 混合；低分辨率颜色初始为透明黑。`ScreenSize` 仍为最终输出尺寸，场景深度始终为原分辨率。`customGpuMs` 包含两个 draw 的总时间。此能力是同一 shader 的双阶段执行，不是通用 post chain。

`screen` 在 `volumeScale: 1` 时上传 `ResolvePass: -1`，表示直接绘制全部层；`volume` 保留原来的 0。全屏效果可在 pass 0 只绘制云团，pass 1 重建云团并绘制原分辨率粒子/细线，参考 [超新星汇聚](../Shaders/SuperNovaConvergence/README.md)。未使用该接口的 screen shader 不受影响。颜色调制应只在最终阶段应用一次；尺寸、深度边缘、相机进入体积以及完整与缩小分辨率切换均需实际验证。

文件纹理读取可选 `.png.mcmeta` 的 `texture.blur` / `texture.clamp`：分别选择线性/最近邻过滤与钳位/循环寻址。缺省保持最近邻和循环寻址，元数据同样参与热重载；指定线性过滤的效果应在采样中正确处理 LOD 和非一致控制流。

从 shader JSON 读取 `int` / `float`（1–4 分量）、`matrix2x2` / `matrix3x3` / `matrix4x4` 默认值。一个默认值按照 Minecraft 规则广播；矩阵按列主序上传。检查 JSON 声明与 GPU 活动 uniform 类型，优化掉的值只提示。

自动提供 `ModelViewMat`、`ProjMat`、`IViewRotMat`、`TextureMat`、`ScreenSize`、`ColorModulator`、`Light0_Direction`、`Light1_Direction`、`FogStart/End/Color/Shape`、`GameTime`、`ChunkOffset`、`LineWidth`、`GlintAlpha`。`EffectLocalMat` 在默认实体局部坐标契约下为单位矩阵。

声明 `InvProjMat` 时自动上传当前 `ProjMat` 的逆矩阵，用于逐像素重建视图坐标。

默认两方向光是固定试验光；光照贴图是全亮 16×16，实体 overlay 使用原版通道约定。需要其他光照时可显式绑定自己的纹理、覆盖 uniform。

| binding source | 输入 / 单位 |
| --- | --- |
| `seconds` | 自预览开始的秒数 |
| `ticks` | 秒数 × 20 |
| `game_time` | `(ticks % 24000) / 24000`，与原版 GameTime 一致 |
| `camera_position` | 相机世界坐标，格 |
| `camera_relative` | 配置 `value: [x,y,z]` 减去相机世界坐标，格 |
| `inverse_view_projection` | `(投影 × 完整视图矩阵)` 的逆，重建世界坐标 |
| `inverse_view_projection_rotation` | `(投影 × 视图旋转)` 的逆，重建相机相对坐标 |

时间绑定可用对象形式：`{"source":"seconds","scale":2,"offset":0.5}`。运行时优先级：**JSON 默认值 → 内建值 → 配置 uniforms → 配置 bindings → 界面覆盖**。修改 shader 文件后，界面临时覆盖会清除；长期参数请保存到配置。

| texture 输入 | 内容 |
| --- | --- |
| `@surface` | 当前网格原材质；Sampler0 的默认输入 |
| `@overlay` | 16×16 实体叠加纹理；Sampler1 默认值 |
| `@lightmap` | 16×16 全亮光照纹理；Sampler2 默认值 |
| `@scene_depth` | 地形 + 实体深度；DepthSampler 默认值 |
| `@block_depth` | 仅地形深度；BlockDepthSampler 默认值 |
| `@scene_color` | 地形 + 实体颜色快照 |
| `@white` / `@checker` | 全白 / 棋盘测试纹理 |
| 资源位置或相对 PNG 路径 | 例如 `minecraft:textures/block/stone.png` 或 `textures/noise.png` |

场景颜色、深度输入只在 `screen` / `volume` pass 中可用；两者写入的 output 与采样的 scene / blocks 附件独立，防止反馈或读取尚未绘制的目标。当前支持 `sampler2D`。PNG 保持 Minecraft 的像素行与 UV 约定；使用最近邻放大，PNG 自动生成 mipmap。

## 与游戏一致的部分与边界

- 桌面 OpenGL 直接编译原文件；支持命名空间、递归 `#moj_import`、include 去重、版本合并和带源文件索引的错误信息。未使用 WebGL 或改写着色器算法。
- 属性按 JSON 顺序绑定。Position/UV0 为 float，Color 为归一化无符号字节，UV1/UV2 为整数 short，Normal 为归一化有符号字节，保留法线后 padding。
- shader JSON 的混合公式、RGB/Alpha 分离因子直接应用；深度测试、写入和剔除由预览配置补充。实际游戏 Java 可能另外改变状态，必须核对绘制时最终生效的状态。
- 模型是工具生成的 Minecraft 尺寸盒模型与动画试件；**不执行 Minecraft 世界、实体 AI、Forge 事件或任意模组 renderer**。不自动导入方块状态/BakedModel、OBJ、复杂实体骨骼。
- 光照贴图、AO、方块遮挡剔除、透明物体排序、天气和游戏材质图集未完整复刻。方块试件使用独立方块纹理，而非 Minecraft block atlas；依赖 atlas UV/动画的效果需另加对应资源和网格生成器。
- 原版 core JSON 与 OptiFine/Iris shaderpack、Minecraft 后处理链 JSON 是不同格式。本版不加载后两者，不支持任意 pass 图、cubemap sampler、HDR/Bloom 或 shaderpack 特殊缓冲附件。天空盒试件使用立方体方向向量，可用 sampler2D 制作六面图集/全景采样。
- 目标是让正式文件和输入约定可直接复用。实际世界的渲染阶段、坐标、光照及其他模组兼容性仍需最后在游戏内验证。

## 验证与扩展

开发与 agent 验收默认使用 [无桌面操作的调试接口](DEBUG_API.md)，通过隐藏 GPU 进程加载 shader、设置参数、捕获画面和深度，不需要控制用户键鼠。`run.cmd -SmokeTest` 还能导出隐藏控制台的布局图片与控件 JSON。

```powershell
.\run.cmd -Verify
```

创建隐藏 OpenGL 窗口进行实际 GPU 编译和绘制，输出 `build/verification/report.json` 与 PNG。本机 RTX 3060 Laptop / OpenGL 3.2 已通过示例场景、现有 FantasyDesire 9 组 shader、错误恢复、深度遮挡和窗口尺寸变化检查。

关键源码：

- `Project.java`：资源位置、配置、依赖检测和 include。
- `ShaderProgram.java`：GLSL 编译/链接、类型检查、uniform 与混合。
- `Mesh.java`：顶点打包、方块和实体试件。新增真实网格导入器可从这里扩展。
- `Renderer.java` / `Target.java`：场景、pass 顺序、输入绑定与目标生命周期。
- `Controls.java`：编辑、时间、预览配置导出。
- `Verification.java`：可重复的 GPU 集成检查；验证产物不进入发布内容。

mod 的 Gradle 配置、运行源码与正式 shader 不依赖此工具。三个 `fantasy-*` 预设与自动验证按 `shaderDev/MinecraftShaderLab/` 布局查找主项目资源，整个项目移动或改名不需要修改绝对路径。
