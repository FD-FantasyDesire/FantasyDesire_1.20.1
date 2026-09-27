# 无桌面操作的验证接口

开发和 agent 验收默认使用这里的接口。它启动独立隐藏进程，与用户正在操作的预览窗口分离；不创建可见窗口，不请求焦点，不注入键鼠。GLSL 编译和绘制仍由真实 GPU 完成。

隐藏 GLFW 上下文仍需要可用的桌面 OpenGL 驱动；这不是无需显示驱动的 EGL/软件渲染服务。

本页命令以 **`shaderDev/MinecraftShaderLab` 为工作目录**；从主项目根目录运行的完整示例见 [shaderDev 的 Agent 验证流程](../README.md#agent-验证流程)。API 的相对路径始终以工具目录为基准，例如 `../MyEffect/main.preview.json` 指向同级作品。

## 三种验证入口

```powershell
# 双击启动路径的回归检查：Windows PowerShell 5.1、隐藏控制台、首次绘制。
.\run.cmd -SmokeTest

# shader、重载、场景的 GPU 集成检查。
.\run.cmd -Verify

# 启动并测试 API，再自动关闭自己的进程；不会连接用户的 GUI 实例。
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tests\debug-api.ps1
```

产物分别为 `build/smoke/`、`build/verification/`、`build/api-tests/<运行编号>/`。

`smoke` 导出 `viewport.png`、`controls.png` 和 `report.json`。控制台图片由 Swing 在进程内部绘制，JSON 也包含控件层级、位置、文本和 uniform 行数，可用于检查布局，不需要截图用户桌面。

## 持续调试会话

```powershell
.\run.cmd -DebugServer -SessionFile .\build\debug\agent-session.json
```

进程持续运行直到收到 `shutdown`。需要后台启动时用 `Start-Process -WindowStyle Hidden`；不要把控制台窗口显示给用户。端口默认 `0`，由操作系统选择空闲端口；可用 `-Port` 明确指定。

读取 `-SessionFile` 指定的 JSON 获取 `baseUrl`、`token`、`pid` 和 `state`。只有 `state: "ready"` 才能调用；退出后变为 `stopped`。未指定文件时保存到 `build/debug/session-<pid>.json`。服务仅监听 `127.0.0.1`，请求必须带 `X-ShaderLab-Token`；不接受浏览器 Origin 请求。

CLI 客户端避免手动拼 HTTP：

```powershell
$session = Join-Path (Get-Location) 'build/debug/agent-session.json'
$client = Join-Path (Get-Location) 'debug-client.ps1'
& $client -Session $session -Command status
& $client -Session $session -Command load -Body '{"path":"examples/fantasy-frost.preview.json"}'
& $client -Session $session -Command configure -Body '{"time":2.75,"camera":{"yaw":45,"pitch":20}}'
& $client -Session $session -Command render -Body '{"width":800,"height":600,"attachments":true}'
& $client -Session $session -Command frame -Output .\build\debug\last.png
& $client -Session $session -Command shutdown
```

复杂请求可用 `-InputFile request.json`，避免 shell 转义。非成功响应使客户端以失败状态退出，并显示服务返回的 JSON 错误。

所有 OpenGL 命令在同一个渲染线程执行。空闲时不持续绘制；时间冻结，只有显式 `render` 才生成帧，便于复现和逐帧对比。调试会话不自动重载文件，保存后显式调用 `reload`；普通 GUI 仍保留自动重载。

## API v1

成功返回 `{"ok":true,"requestId":1,"result":{...}}`；失败返回 `{"ok":false,"requestId":1,"error":{"kind":"...","message":"..."}}`。PNG 接口成功时直接返回图片。

| 方法 / 路径 | 请求 / 返回 |
| --- | --- |
| `GET /v1/status` | GPU、加载状态、当前/请求文件、程序版本、相机、状态、uniform、依赖和错误 |
| `POST /v1/load` | `{"path":"examples/entity.preview.json"}`；相对路径以工具目录为基准，也接受绝对路径 |
| `POST /v1/reload` | `{}`；重载上次请求的文件，失败保留有效程序 |
| `POST /v1/configure` | 修改下方支持的场景参数；整份请求验证后才生效 |
| `POST /v1/render` | `{"width":800,"height":600,"time":2.75,"attachments":true}`；字段均可省略 |
| `GET /v1/frame.png` | 最近一次成功捕获的实际 framebuffer PNG；尚未绘制时返回 404 |
| `POST /v1/shutdown` | `{}`；应答后释放 GPU 资源，关闭当前调试进程 |

`configure` 示例：

```json
{
  "time": 2.75,
  "target": "entity",
  "entity": "zombie",
  "camera": {"yaw": 35, "pitch": 25, "distance": 9, "fov": 60},
  "state": {"depthTest": true, "depthWrite": false, "cull": true, "overlay": true},
  "scene": {"terrain": true, "entity": true},
  "clearOverrides": false,
  "uniforms": {"VoidStrength": 0.65, "EchoStrength": 0.6}
}
```

只发送需要改变的字段即可。切换 `target` 不会隐式改变深度和剔除状态，需一起指定预期状态。未声明 uniform、非法目标、非有限数值、无效布尔值或越界相机参数会被拒绝。

`status.uniforms` 区分 `defaults`、`configured`、`binding`、`override`；`uploaded` 从 GPU 读回，是最近一次上传的值。`configure` 不绘制，读回值将在下一次 `render` 后更新。优化掉的 uniform 的 `active` 为 false。

`generation` 只在 shader 成功替换时递增。加载失败后 `requested` 可指向失败文件，`descriptor` 仍指向正在使用的有效程序。`lastError` 记录最近命令错误；`status` 保留渲染器加载诊断。

## 图像与深度产物

每次 render 写入 `build/debug/captures-<pid>/frame-<编号>/`，返回 PNG 绝对路径及 SHA-256。同一机器、同一程序和输入下，可比较哈希来检查时间冻结或参数变化；不同驱动之间不要求逐像素相同。

- `color.png`：效果合成后的帧。
- `state.json`：该帧的相机、时间、状态及实际上传的 uniform。
- `attachments: true` 额外写入 `terrain-color.png`、`scene-color.png`、两份 depth PNG 与 `.f32`。
- `.f32`：小端 float32，逐行排列，原点在左下，值为 OpenGL 非线性深度 `[0,1]`，尺寸为 `width * height * 4` 字节。
- depth PNG：同一深度量化为 16 位灰度，原点在左上。远处数值接近 1 属于正常投影分布，精确比较使用 `.f32`。

默认 800×600；单边范围 16–4096，总像素数最多 8,388,608。返回的深度统计包含最小值、最大值和非背景像素数。

HTTP 400 表示请求参数错误，403 表示认证错误，422 表示加载或渲染失败，503 表示队列已满。命令等待上限 30 秒，超时返回 504：尚未开始的命令被取消，已经开始的 GPU 操作不能安全中断，应查询状态后再决定是否重试。

## 启动问题排查

`run.cmd` 的每次启动日志位于 `build/logs/launcher-*.log`。无参数双击失败时保留错误窗口；带参数的自动验证失败时立即返回非零退出码，不暂停等待按键。

`run.ps1` 必须保存为 **UTF-8 BOM**，这由 `.editorconfig` 明确指定；Windows PowerShell 5.1 无法可靠识别无 BOM 的中文脚本。验收必须经过 `cmd.exe → run.cmd → powershell.exe`，不能只从 PowerShell 7 调用脚本。
