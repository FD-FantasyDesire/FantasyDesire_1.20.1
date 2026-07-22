# 纯 GLSL Shader 开发脚手架

此目录用于快速制作 Minecraft 魔法、能量和粒子视觉效果的**纯程序化原型**。预览入口为 `main.frag`，目标运行环境是 VSCode 的 glsl-canvas，而不是 Minecraft 正式渲染管线。

## 开始使用

1. 在 VSCode 扩展市场安装 **GLSL Canvas**（扩展 ID：`circledev.glsl-canvas`）。工作区也会显示推荐安装提示。
2. 用 VSCode 打开 `shaderDev/main.frag`。
3. 打开命令面板（`Ctrl+Shift+P`），运行 **Show GLSL Canvas**。
4. 保存文件后观察预览；移动鼠标可扰动符文中心、亮度和旋转参数。

## Uniform 约定

入口使用 glsl-canvas 提供的以下 uniform：

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

## `lib` 函数库

`lib/common.glsl`、`lib/color.glsl` 和 `lib/noise.glsl` 是带 include guard 的备用函数库，分别提供常用数学、颜色与噪声函数。`main.frag` **默认没有 include 它们且完全自包含**，确保 glsl-canvas 无需预处理器即可直接运行。

使用库函数时，推荐将需要的函数复制到实验 shader。若使用支持 `#include` 的额外预处理器，需自行确认相对路径语法、include guard 支持情况以及函数名是否与入口重复；原生 WebGL 1 和原版 GLSL ES 1.00 不提供标准 `#include`。

## 迁移到 Minecraft

原型迁移不是直接复制即用，需要按目标 Minecraft shader 类型适配：

1. 将 `u_time`、`u_resolution`、`u_mouse` 替换为目标管线实际可用的数据；Minecraft 通常不会直接提供鼠标位置。
2. 根据正式 shader 的版本声明、输入输出变量、矩阵、混合模式和深度状态调整语法。
3. 将全屏 `gl_FragCoord` 坐标改成模型 UV、屏幕坐标或顶点阶段传入的局部坐标。
4. 检查颜色空间、透明度与预乘 Alpha；此示例输出不透明颜色并在 shader 内做简易色调映射。
5. 只迁移确认需要的函数，避免与 Minecraft 的 include 文件、宏和 uniform 命名冲突。

本目录仅用于原型开发，不修改或自动接入项目的正式 Minecraft shader 资源。
