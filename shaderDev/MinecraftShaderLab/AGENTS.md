# 开发约定

这是独立的 Minecraft 1.20.1 core shader 预览工具，不是 Forge 模组。

目录固定为主项目的 `shaderDev/MinecraftShaderLab/`，作为所有 shaderDev 作品共用的宿主。工具文档中的命令从本目录执行；主项目根目录调用方式见 [上级 README](../README.md)。正式资源使用相对路径，不依赖盘符或主项目文件夹名称。

- Java 17、LWJGL 3.3.1、桌面 OpenGL 3.2 Core；源码和说明使用中文注释。
- `run.cmd -CompileOnly` 编译；`run.cmd -Verify` 使用真实 GPU 检查。
- 验证、调试默认使用隐藏接口，见 `DEBUG_API.md`：`run.cmd -DebugServer` 提供本机 API；agent 不控制用户桌面，也不接管用户的 GUI 进程。
- 用 `run.cmd -SmokeTest` 验证启动与界面，读取 `build/smoke/controls.png` 和控件 JSON；用 `tests/debug-api.ps1` 验证 API、图像和深度数据。测试自己启动的进程必须在结束时 shutdown。
- `.ps1` 保持 UTF-8 BOM。启动器回归必须用 Windows `cmd.exe` 调用 `.cmd`，覆盖 Windows PowerShell 5.1，不能仅依赖 PowerShell 7。
- 不自动改写用户的 `.fsh`、`.vsh`、shader JSON；实验参数存入 `.preview.json`。
- 保持 Color / Normal 的归一化整数语义、UV1 / UV2 的整数属性，以及矩阵列主序。
- 编译、链接、资源绑定或配置验证失败时保留原程序；成功后释放旧 GL 资源。
- 禁止在当前写入的 framebuffer 附件上进行采样。
- 提供渲染状态、时间单位、坐标和资源契约；不能把简化场景描述成完整 Minecraft 渲染器。
- 游戏资源仅从用户本机读取，不随工具分发。
