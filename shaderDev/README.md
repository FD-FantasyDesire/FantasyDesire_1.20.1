# 纯 GLSL Shader 开发脚手架

此目录用于快速制作 Minecraft 魔法、能量和粒子视觉效果的**纯程序化原型**。每个效果目录中的 `main.frag` 都是独立入口，目标运行环境是 VSCode 的 glsl-canvas，而不是 Minecraft 正式渲染管线。

## 开始使用

1. 在 VSCode 扩展市场安装 **GLSL Canvas**（扩展 ID：`circledev.glsl-canvas`）。工作区也会显示推荐安装提示。
2. 用 VSCode 打开某个效果目录中的 `main.frag`，例如 `shaderDev/BladeRift/main.frag`。
3. 打开命令面板（`Ctrl+Shift+P`），运行 **Show GLSL Canvas**。
4. 保存文件后观察预览；只有显式声明并使用 `u_mouse` 的作品支持鼠标交互。

## Uniform 约定

入口按需要使用 glsl-canvas 提供的以下 uniform：

```glsl
uniform float u_time;       // 自预览开始后的秒数
uniform vec2 u_resolution;  // 画布像素尺寸
uniform vec2 u_mouse;       // 鼠标像素坐标
```

不要在这个脚手架中假定 Minecraft 的 uniform 名称与其一致。迁移时应通过目标 shader JSON、顶点阶段或 Minecraft 提供的 uniform 替换它们。

## 坐标约定

- `gl_FragCoord.xy` 和 `u_mouse` 均按像素处理。
- glsl-canvas 尚未收到鼠标输入而提供 `u_mouse == vec2(0.0)` 时，示例将其视为中性输入，不偏移符文中心。
- 示例将画面中心映射到原点，并除以分辨率的较短边，因此图形在不同宽高比下保持比例。
- 归一化后，短边从一侧到另一侧约为 `-1.0` 到 `1.0`。
- 运算中对分辨率、除数和发光距离设置了最小值，避免除零和中心奇点产生不稳定结果。

## 无贴图约束

当前阶段禁止依赖贴图：入口和函数库均不声明 `sampler`，也不调用任何 `texture*` 函数。图像完全由 SDF、value noise、FBM、轨道粒子和程序化调色板生成。请勿在 VSCode 设置中添加纹理通道。

## 兼容边界

- 入口按 **WebGL 1 / GLSL ES 1.00** 编写：不声明 `#version`，使用 `precision highp float` 与 `gl_FragColor`。
- 循环次数均为编译期常量，以符合 WebGL 1 对循环展开的常见限制。
- 避免整数位运算、动态数组、导数和较新 GLSL 内建函数。
- 某些移动端 WebGL 1 实现可能不支持片元阶段 `highp`；如目标设备编译失败，可在仅接受精度下降时改为 `mediump`。

## 几何与动画约束

以下约束是新建和修改效果时的默认规则。具体作品可以有意偏离，但必须在自身 README 和关键公式旁说明原因。

- 线段、射线和贝塞尔路径必须显式声明起点与终点。即使目标是画面中心，也应传入 `target = vec2(0.0)`，不能通过省略贝塞尔终点项、默认节点或未命中分支隐式回落到原点。
- 从球体、传送门或其它实体表面发出的光束必须同时裁剪根部和头部。计算域应满足 `rootDistance <= distance <= headDistance`，不能从局部原点开始采样后依赖前景图形遮挡错误部分。
- 线状几何必须具有有限轴向范围。辉光、噪声和色差只能调制有限形状，不能把 `abs(y)`、角距离或半平面遮罩单独当作完整线段。
- 生命周期遮罩与空间形状分开计算。默认使用有限的头尾窗口、中心向两端展开或整体淡入；`p.x <= front` 一类单侧累计 reveal 只有在设计明确要求时才使用。
- 所有阶段都要独立验收开始、中点、结束前一帧和切换后一帧。重点检查默认端点、尚未出生的路径、已经经过的尾迹以及宽屏画布边缘，不能只检查完整展开后的静态画面。
- 发光尾迹必须在几何或网格边缘前降到丢弃阈值以下。扩大绘制区域不能替代根部、头部和生命周期裁剪。

`BladeRift` 的 Spawn 明确采用从局部左端向右端推进的单侧累计 reveal，因此中途会有一段从起点延伸到中心的裂痕。这是该作品的特例，不应作为其它线状效果的默认生成方式。

## 自包含约定

当前没有公共 `main.frag` 或 `lib/*.glsl`。每个入口保持自包含，确保 glsl-canvas 无需预处理器即可直接运行。复用函数时应连同输入、输出和空间约束一起复制，不能只复制距离公式而遗漏端点或生命周期遮罩；原生 WebGL 1 和原版 GLSL ES 1.00 不提供标准 `#include`。

## 迁移到 Minecraft

原型迁移不是直接复制即用，需要按目标 Minecraft shader 类型适配：

1. 将 `u_time`、`u_resolution`、`u_mouse` 替换为目标管线实际可用的数据；Minecraft 通常不会直接提供鼠标位置。
2. 根据正式 shader 的版本声明、输入输出变量、矩阵、混合模式和深度状态调整语法。
3. 将全屏 `gl_FragCoord` 坐标改成模型 UV、屏幕坐标或顶点阶段传入的局部坐标。
4. 检查颜色空间、透明度与预乘 Alpha；此示例输出不透明颜色并在 shader 内做简易色调映射。
5. 只迁移确认需要的函数，避免与 Minecraft 的 include 文件、宏和 uniform 命名冲突。

本目录仅用于原型开发，不修改或自动接入项目的正式 Minecraft shader 资源。

## 独立作品

- [`AstraLightning`](AstraLightning/main.frag)：固定节点构成的闪电折线原型。
- [`BladeRift`](BladeRift/README.md)：单侧生成的有限空间裂痕；该 Spawn 方式是有意特例。
- [`EchoTimer`](EchoTimer/README.md)：附着于实体轮廓的虚空侵蚀效果。
- [`SuperNova`](SuperNova/main.frag)：多色粒子流汇聚后形成体积超新星。
- [`SuperNovaConvergence`](SuperNovaConvergence/README.md)：同色粒子对称汇入旋转星核，按可设倒计时爆发的三维星云预制。
- [`VoidRifter`](VoidRifter/main.frag)：交错菱形十字裂痕与中央传送门。

`SuperNovaConvergence` 另提供共用着色器源文件的本地审查页，支持暂停、逐帧、切换随机种子和环绕视角；启动方式及实际渲染动画见其 README。
