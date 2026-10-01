# 下载的 Unity 特效参考与参数映射

2026-10-02 下载并读取源码、Unity YAML、Shader Graph 节点及 FBX；没有运行 Unity 工程。完整参考文件保存在本机忽略目录 `captures/reference-source/`，作品仅携带 MIT 噪声纹理及许可。

## 球形爆炸：MIT 工程

项目：[the-other-mariana/explosion-shader-unity](https://github.com/the-other-mariana/explosion-shader-unity/tree/eb2b022af73c6a7172e9c4e6a23f1f508f780677)。固定提交 `eb2b022af73c6a7172e9c4e6a23f1f508f780677`，MIT，作者 Mariana Ávalos Arce。

读取 `ExplosionShader.shader`、`GenerateTextures.cs`、`PrefabExplosion1.prefab`、`New Material.mat`，下载 `DissolvePattern.png`：

| 组件/参数 | 源项目实际值或行为 | 本作品处理 |
| --- | --- | --- |
| MeshFilter | Unity 内建网格，fileID=10207；SphereCollider 半径 0.5 | 解析球面，保留三维前后表面和真实前表面深度 |
| 分层 Perlin | scale=5 / strength=1；scale=25 / strength=0.3 | 共享 512×512 噪声纹理，低频及五倍频细节，权重 1:0.3 |
| _DispFactor | 0.395，沿顶点法线位移 | 保留球形边界；不复制原型的凹凸网格位移 |
| _Depth / _DisapFactor | 材质 (0,0.5,0) / 0.781；噪声映射渐变并 clip | 相同的噪声着色与阈值溶解思路；阈值 0.12→0.86，柔化宽度 0.08 |
| NoiseMove | 混合 RGB 噪声通道；正弦频率 2，WaveLength=0.65，WaveDisplacement=0.33 | 改成空间绕轴扭转及时间滚动，避免源代码通道归一化的除零风险 |
| 缩放 | initScale=0.3，growth=0.8，maxScale=4.5 | 改成 0.58 秒瞬爆，约 0.24 秒内快速膨胀，末段碎裂消失 |
| 渐变 | 白、浅黄、黄、橙及灰黑共十个颜色 | 改为青绿、青蓝和金色亮边，适合奇幻技能 |

噪声纹理原文件逐字节复用，路径 `assets/embersigil/textures/dissolve-pattern.png`；附带 `LICENSE-REFERENCE.txt`。这张静态溶解纹理并非源码运行时生成的三通道 Perlin，图形细节不应称为原 Unity 项目的逐像素还原。

## 官方 VFX Graph：模型与节点参考

项目：[Unity-Technologies/VisualEffectGraph-Samples](https://github.com/Unity-Technologies/VisualEffectGraph-Samples/tree/bcd800405c1b019654a0af3b4b8bd68fec590b4b)。固定提交 `bcd800405c1b019654a0af3b4b8bd68fec590b4b`。Unity Companion License，官方文件留在本机参考目录，不作为 Minecraft 资源打包。

Shader Graph 由连续 JSON 对象组成，逐对象解码后按 ID 解析 slots/edges；VFX Graph 为多文档 Unity YAML，保留 64 位对象 ID。FBX 使用 Three.js FBXLoader 解析。

| 参考文件 | 核对的结构和参数 | 取用的设计思路 |
| --- | --- | --- |
| `MeteoriteBlastMesh2.fbx` | 700 三角形，展开后 2100 顶点，有 UV；对象缩放 0.01；缩放后包围盒 X/Z≈[-1,1]、Y≈[-1,2.92] | 是沿纵向拉长的冲击模型，不能直接当作正球；本作品独立定义球形空间 |
| `MeteoriteBlastMesh.shadergraph` | Gradient Noise scale=1.44、Clamp 最小 0.3；Distortion strength=0.07；Time 乘法值 -1.51 / 0.97；AlphaClip 默认 0.5 | 噪声调制亮度、UV 流动及溶解；细节 UV 偏移取 0.07，时间速度另按秒制设定 |
| `BubbleDissolve.shadergraph` | NoiseScale=50、DissolveAmount=0.5、DissolveWight=0.2、AlphaClip=0.5；噪声/阈值差产生亮边 | 以噪声阈值生成碎裂区域和金色热边；宽度调整为 0.08 |
| `GlowSphere.shadergraph` + `.mat` | Fresnel → OneMinus → Power；材质 Power=2.33、SoftParticle=1.24；颜色≈(0.429,0.955,1) | 使用指数 2.33 的内层权重及弱球壳边缘；不移植 HDRP SoftParticle 节点 |
| `03_GroundImpact.vfx` | Size 曲线有快速峰值：归一化时间约 0.0125 时到 1.1128，0.0265 时回到 1.0043，末段归零；另有快速扩张后趋缓的曲线 | 借鉴快起慢收的爆发节奏，不复用地面输出或原曲线文件 |
| `Vortex.vfxblock` | 平面法线默认 (0,1,0)、Drag=1；Channel/Gravity/Vortex 的 Distance 曲线默认常数 1 | 绕明确轴组织运动；本作品轴倾斜、Twist=2.8、FlowSpeed=7 rad/s，是独立调参，并非粒子力场节点转译 |

本作品是依据下载组件重新设计的 Minecraft GLSL 150 效果，不声称执行或完整复刻 Unity 的 Shader Graph/VFX Graph。预览器无 HDR/Bloom；辉光主要来自有限范围的发光颜色与瞬间爆闪。
