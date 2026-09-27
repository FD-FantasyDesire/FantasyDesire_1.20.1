# SuperNova 3D 原型

`main.frag` 是面向 VSCode GLSL Canvas 的 WebGL 1 / GLSL ES 1.00 单 pass 三维场景原型。它不依赖贴图，通过透视相机、射线与平面/球体求交，在一个片元入口中模拟未来迁移到 Minecraft 后的多层叠加渲染。

## 运行

1. 用 VSCode 打开 `shaderDev/SuperNova3D/main.frag`。
2. 运行 **Show GLSL Canvas**。
3. 在预览画布中移动鼠标，水平控制相机方位角，垂直控制俯仰角。
4. 鼠标输入不可用时使用固定的默认观察角度。

完整循环为 8 秒。开始阶段显示十字爆闪，随后出现世界空间星云冲击球壳；球壳快速扩张并在约 3.1 秒前消失，冲击波接触测试平面后留下具有三层星点、量化星云和曲折分叉裂痕的宇宙流体，残留物在周期末衰减。

## 三维场景

- `buildCamera` 构建透视相机射线，爆炸不再直接使用二维画布坐标。
- `raySphere` 计算视线与世界空间球体的交段，`integrateBurst` 在被测试平面遮挡前执行固定 14 步体积积分。
- `rayPlane` 计算测试平面交点，地面网格用于观察透视比例和遮挡关系。
- `crossFlash` 使用经过爆心且面向相机的世界空间平面，只让星芒朝向相机，不会把整个爆炸退化为二维图案。

测试平面由文件顶部的常量控制：

```glsl
const vec3 TEST_PLANE_POINT = vec3(0.0, 0.0, 0.0);
const vec3 TEST_PLANE_NORMAL = vec3(0.0, 1.0, 0.0);
```

将法线改为其它非零向量可检查斜坡或墙面附着；Shader 会在运行时将其归一化。爆心始终沿平面法线偏移 `EXPLOSION_HEIGHT`。

## 模拟渲染层

单 pass 中的函数边界对应正式实现中的预期渲染层：

- `crossFlash`：面向相机的十字爆闪层，迁移为 Billboard 核心 Shader。
- `burstSample` / `integrateBurst`：世界空间星云体积层，迁移为深度感知全屏 Shader 或球体代理几何。
- `shellContour`：破碎球壳描边层，迁移时使用加法混合。
- `planeMaterial`：测试表面、宇宙残留、星空视差和接触冲击环。
- `crackField`：每条主裂痕由 7 段固定 seed 折线组成，并附带两段式折线分支；正式版本建议改为 Java 生成的地形贴合折线网格。

原型在一个片元中手动合成所有层，仅用于确认形状、节奏、透视和遮挡。迁移到 Minecraft 时应将深色残留层与加法发光层拆开，分别使用预乘 Alpha/普通 Alpha 和加法混合。

## 兼容边界

以下是当前[基础预览入口](../README.md)的实现选择。后续可按总规范扩展为带纹理、顶点阶段或多 pass 的 Minecraft 原型。

- 不声明 `#version`，使用 `gl_FragColor`。
- 只使用 `u_time`、`u_resolution` 和可选的 `u_mouse`。
- 不使用纹理、导数、动态数组或整数位运算。
- 体积积分和裂痕循环次数均为编译期常量。
