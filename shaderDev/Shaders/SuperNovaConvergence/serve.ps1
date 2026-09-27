# 仅在回环地址提供本地静态审查文件；不依赖网络安装或外部服务。
param([int]$Port = 8766)
$ErrorActionPreference = 'Stop'
if ($Port -lt 1024 -or $Port -gt 65535) { throw '端口应在 1024 至 65535 之间。' }
$pythonCommand = Get-Command python -ErrorAction SilentlyContinue
$bundledPython = Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
Write-Host "审查地址：http://127.0.0.1:$Port/SuperNovaConvergence/preview.html"
Write-Host '关闭此进程或按 Ctrl+C 停止服务。'
if ($pythonCommand) {
    & $pythonCommand.Source -m http.server $Port --bind 127.0.0.1 --directory (Split-Path $PSScriptRoot -Parent)
} elseif (Test-Path -LiteralPath $bundledPython) {
    & $bundledPython -m http.server $Port --bind 127.0.0.1 --directory (Split-Path $PSScriptRoot -Parent)
} else {
    throw '未找到 Python。可直接打开 preview.html，再选择同目录的 main.frag；或使用已有的静态服务器。'
}
