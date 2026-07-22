# Liquid Energy

一个面向 GLSL Canvas 的纯程序化液态能量效果：中心能量球向外黏连约六根长度、角度和粗细略不规则的枝条。球体、胶囊枝条、连接液滴与末端液滴通过平滑并集融合，流动亮斑沿枝条持续外移；分层本体色、边缘高亮、核心脉冲和远近辉光共同形成自发光液体观感。

## 运行环境

- WebGL 1 / GLSL ES 1.00。
- 自包含且不使用纹理、动态数组、导数或现代 GLSL 输出语法。
- 所有循环边界均为编译期常量，适合 GLSL Canvas 直接预览。

## Uniform

```glsl
uniform float u_time;       // 自预览开始后的秒数
uniform vec2 u_resolution;  // 画布像素尺寸
```

坐标按画布短边归一化，因此不同宽高比下能量球和枝条能保持比例。输出使用不透明 `gl_FragColor`，并在片元着色器内进行简易色调映射。

## 主要调参点

参数集中在 `main.frag` 顶部：

- `BACKGROUND_COLOR`、`DEEP_COLOR`、`LIQUID_COLOR`、`CORE_COLOR`：背景、液体暗部、主体和高亮颜色。
- `BALL_RADIUS`：中心液态能量球半径。
- `BRANCH_WIDTH`：六根枝条的基础粗细；每根枝条另有固定倍率以制造不规则感。
- `UNION_SOFTNESS`：球体、枝条和液滴之间的平滑融合范围；增大后更圆润黏稠。
- `GLOW_STRENGTH`：外部近辉光和远辉光的总体强度。
- `FLOW_SPEED`：呼吸、形变和亮斑移动的整体动画速度。
- `WOBBLE_AMOUNT`：球体轮廓起伏及枝条漂移幅度。

每根枝条的折点、末端和独立宽度倍率集中在 `branchData` 中，可直接修改六组固定数据。为维持 WebGL 1 兼容性，建议继续使用显式分支，不要改为动态索引数组。
