param([string]$Root = (Split-Path $PSScriptRoot -Parent))
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
Add-Type -AssemblyName System.Drawing
$labTestRoot = Join-Path $Root ('build/api-tests/' + [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0,6))
New-Item -ItemType Directory -Force $labTestRoot | Out-Null
$labSessionPath = Join-Path $labTestRoot 'session.json'
$labChecks = New-Object 'System.Collections.Generic.List[string]'
$labClient = [Net.Http.HttpClient]::new()
$labClient.Timeout = [TimeSpan]::FromSeconds(40)
$labSession = $null
$labHelper = $null
function Assert-Lab([bool]$Condition, [string]$Message) {
    if (!$Condition) { throw $Message }
    $labChecks.Add($Message)
}
function Invoke-Lab([string]$Path, $Body = $null, [int]$Expected = 200, [bool]$Auth = $true) {
    $method = if ($null -eq $Body) { [Net.Http.HttpMethod]::Get } else { [Net.Http.HttpMethod]::Post }
    $request = [Net.Http.HttpRequestMessage]::new($method, ($labSession.baseUrl + $Path))
    if ($Auth) { $request.Headers.Add('X-ShaderLab-Token', $labSession.token) }
    if ($null -ne $Body) { $request.Content = [Net.Http.StringContent]::new(($Body | ConvertTo-Json -Depth 12 -Compress), [Text.Encoding]::UTF8, 'application/json') }
    try {
        $response = $labClient.SendAsync($request).GetAwaiter().GetResult()
        try {
            $text = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            if ([int]$response.StatusCode -ne $Expected) { throw "Unexpected HTTP status $($response.StatusCode) for ${Path}: $text" }
            return $text | ConvertFrom-Json
        } finally { $response.Dispose() }
    } finally { $request.Dispose() }
}
try {
    # 与双击入口相同的 Windows PowerShell 5.1；子进程及其 GL 窗口均不显示。
    $labHelper = Start-Process -FilePath "$env:SystemRoot/System32/WindowsPowerShell/v1.0/powershell.exe" -WindowStyle Hidden -PassThru -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File',('"' + $Root + '/run.ps1"'),'-DebugServer','-SessionFile',('"' + $labSessionPath + '"')) -RedirectStandardOutput "$labTestRoot/server.out.log" -RedirectStandardError "$labTestRoot/server.err.log"
    $deadline = [DateTime]::UtcNow.AddSeconds(30)
    while (!(Test-Path -LiteralPath $labSessionPath)) {
        if ($labHelper.HasExited) { throw "Debug server exited; see $labTestRoot/server.err.log" }
        if ([DateTime]::UtcNow -gt $deadline) { throw 'Debug server startup timed out' }
        Start-Sleep -Milliseconds 100
    }
    $labSession = [IO.File]::ReadAllText($labSessionPath) | ConvertFrom-Json
    Assert-Lab ($labSession.state -eq 'ready' -and !$labSession.visible) 'Hidden server publishes ready session'
    $unauthorized = Invoke-Lab '/v1/status' $null 403 $false
    Assert-Lab (!$unauthorized.ok -and $unauthorized.error.kind -eq 'forbidden') 'Missing token is rejected'
    $state = (Invoke-Lab '/v1/status').result
    Assert-Lab ($state.loaded -and !$state.visible -and $state.paused) 'Status exposes loaded deterministic renderer without visible windows'
    $clientState = (& "$Root/debug-client.ps1" -Session $labSessionPath -Command status) | ConvertFrom-Json
    Assert-Lab ($clientState.ok -and $clientState.result.pid -eq $labSession.pid) 'CLI client reads the same hidden session'
    $noFrame = Invoke-Lab '/v1/frame.png' $null 404
    Assert-Lab ($noFrame.error.kind -eq 'no_frame') 'PNG endpoint reports no frame before render'

    foreach ($name in @('surface','entity','sky','depth','fantasy-rift','fantasy-void','fantasy-frost')) {
        $loaded = (Invoke-Lab '/v1/load' @{path="examples/$name.preview.json"}).result
        $rendered = (Invoke-Lab '/v1/render' @{width=640;height=480;time=2.75;attachments=($name -eq 'depth')}).result
        Assert-Lab ($loaded.loaded -and (Test-Path -LiteralPath $rendered.capture) -and $rendered.sha256.Length -eq 64) "GPU capture via API: $name"
        if ($name -eq 'depth') {
            Assert-Lab ((Get-Item -LiteralPath $rendered.depth.terrain.raw).Length -eq 640*480*4) 'Depth float32 dump size matches framebuffer'
            $terrain = [IO.File]::ReadAllBytes($rendered.depth.terrain.raw)
            $scene = [IO.File]::ReadAllBytes($rendered.depth.scene.raw)
            $occluded = 0
            for ($i=0; $i -lt $terrain.Length; $i+=4) { if ([BitConverter]::ToSingle($scene,$i) -lt [BitConverter]::ToSingle($terrain,$i)-0.000001) { $occluded++ } }
            Assert-Lab ($occluded -gt 20) 'Depth snapshots retain entity occlusion'
        }
    }
    Invoke-Lab '/v1/load' @{path='examples/surface.preview.json'} | Out-Null
    $first = (Invoke-Lab '/v1/render' @{time=1.25;width=320;height=240}).result
    $second = (Invoke-Lab '/v1/render' @{time=1.25;width=320;height=240}).result
    Assert-Lab ($first.sha256 -eq $second.sha256) 'Fixed time and camera produce identical PNG hashes'
    $configured = (Invoke-Lab '/v1/configure' @{uniforms=@{Pulse=1.0};camera=@{yaw=80;pitch=12}}).result
    $changed = (Invoke-Lab '/v1/render' @{}).result
    Assert-Lab ($changed.sha256 -ne $first.sha256 -and $configured.camera.yaw -eq 80) 'Camera and uniform commands affect actual pixels'
    $pulse = @($changed.uniforms | Where-Object { $_.name -eq 'Pulse' })[0]
    Assert-Lab ($pulse.uploaded[0] -eq 1.0) 'Status reads back the uploaded GPU uniform'
    $invalid = Invoke-Lab '/v1/configure' @{time=99;camera=@{distance=0}} 400
    $after = (Invoke-Lab '/v1/status').result
    Assert-Lab (!$invalid.ok -and $after.time -eq 1.25) 'Invalid configure request leaves prior state intact'
    Invoke-Lab '/v1/render' @{width=99999} 422 | Out-Null
    $resized = (Invoke-Lab '/v1/render' @{width=537;height=311;time=2}).result
    Assert-Lab ($resized.width -eq 537 -and $resized.height -eq 311) 'Framebuffer resize works through API'
    $requestFile = Join-Path $labTestRoot 'request with spaces.json'
    [IO.File]::WriteAllText($requestFile, '{"width":320,"height":240,"time":3.5}', [Text.UTF8Encoding]::new($false))
    $cliFrame = (& "$Root/debug-client.ps1" -Session $labSessionPath -Command render -InputFile $requestFile) | ConvertFrom-Json
    & "$Root/debug-client.ps1" -Session $labSessionPath -Command frame -Output "$labTestRoot/client frame.png" | Out-Null
    Assert-Lab ($cliFrame.ok -and $cliFrame.result.time -eq 3.5 -and (Test-Path -LiteralPath "$labTestRoot/client frame.png")) 'CLI accepts request files and exports PNG without shell JSON escaping'

    $fixture = Join-Path $labTestRoot 'fixture'
    New-Item -ItemType Directory -Force $fixture | Out-Null
    $utf8 = [Text.UTF8Encoding]::new($false)
    [IO.File]::WriteAllText("$fixture/test.vsh", "#version 150`nin vec3 Position;`nin vec2 UV0;out vec2 uv;`nvoid main(){gl_Position=vec4(Position,1);uv=UV0;}`n", $utf8)
    $validFragment = "#version 150`nin vec2 uv;out vec4 fragColor;`nvoid main(){fragColor=vec4(uv,0.3,1);}`n"
    [IO.File]::WriteAllText("$fixture/test.fsh", $validFragment, $utf8)
    [IO.File]::WriteAllText("$fixture/test.json", '{"vertex":"test","fragment":"test","attributes":["Position","UV0"],"samplers":[],"uniforms":[]}', $utf8)
    [IO.File]::WriteAllText("$fixture/test.preview.json", '{"shader":"test.json","target":"screen","state":{"depthTest":false,"depthWrite":false,"cull":false}}', $utf8)
    $valid = (Invoke-Lab '/v1/load' @{path="$fixture/test.preview.json"}).result
    $goodFrame = (Invoke-Lab '/v1/render' @{time=0}).result
    [IO.File]::WriteAllText("$fixture/test.fsh", ($validFragment + 'BROKEN_SHADER'), $utf8)
    $failed = Invoke-Lab '/v1/reload' @{} 422
    $retained = (Invoke-Lab '/v1/status').result
    $retainedFrame = (Invoke-Lab '/v1/render' @{time=0}).result
    Assert-Lab ($failed.error.kind -eq 'load_error' -and $retained.generation -eq $valid.generation -and $retainedFrame.sha256 -eq $goodFrame.sha256) 'Compile error is structured and preserves last valid pixels'
    [IO.File]::WriteAllText("$fixture/test.fsh", $validFragment.Replace('0.3','0.7'), $utf8)
    $repaired = (Invoke-Lab '/v1/reload' @{}).result
    $repairedFrame = (Invoke-Lab '/v1/render' @{time=0}).result
    Assert-Lab ($repaired.generation -gt $valid.generation -and $repairedFrame.sha256 -ne $goodFrame.sha256) 'Repair reload replaces the program and rendered pixels'
    $imageRequest = [Net.Http.HttpRequestMessage]::new([Net.Http.HttpMethod]::Get, ($labSession.baseUrl+'/v1/frame.png'))
    $imageRequest.Headers.Add('X-ShaderLab-Token',$labSession.token)
    $imageResponse = $labClient.SendAsync($imageRequest).GetAwaiter().GetResult()
    try {
        $imageBytes = $imageResponse.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult()
        $digest = [Security.Cryptography.SHA256]::Create()
        try { $hash = [BitConverter]::ToString($digest.ComputeHash($imageBytes)).Replace('-','').ToLowerInvariant() } finally { $digest.Dispose() }
        Assert-Lab ($hash -eq $repairedFrame.sha256) 'PNG endpoint returns the actual captured framebuffer'
    } finally { $imageResponse.Dispose(); $imageRequest.Dispose() }
    # 真实双阶段体积：覆盖奇数尺寸、缩小纹理绑定和最后阶段的实际 uniform。
    $volumeVertex = @'
#version 150
in vec3 Position;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
void main(){gl_Position=ProjMat*ModelViewMat*vec4(Position,1);}
'@
    $volumeFragment = @'
#version 150
uniform sampler2D AuraSampler;
uniform vec2 RenderSize;
uniform int ResolvePass;
out vec4 fragColor;
void main(){
    vec2 uv=gl_FragCoord.xy/RenderSize;
    fragColor=ResolvePass==1?texture(AuraSampler,uv):vec4(uv*0.4,0.2,0.5);
}
'@
    [IO.File]::WriteAllText("$fixture/volume.vsh",$volumeVertex,$utf8)
    [IO.File]::WriteAllText("$fixture/volume.fsh",$volumeFragment,$utf8)
    [IO.File]::WriteAllText("$fixture/volume.json",'{"vertex":"volume","fragment":"volume","attributes":["Position"],"blend":{"srcrgb":"one","dstrgb":"1-srcalpha","srcalpha":"one","dstalpha":"1-srcalpha"},"samplers":[{"name":"AuraSampler"}],"uniforms":[{"name":"ModelViewMat","type":"matrix4x4","count":16,"values":[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1]},{"name":"ProjMat","type":"matrix4x4","count":16,"values":[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1]},{"name":"RenderSize","type":"float","count":2,"values":[1,1]},{"name":"ResolvePass","type":"int","count":1,"values":[0]}]}',$utf8)
    $volumePreview='{"shader":"volume.json","target":"volume","volumeScale":0.5,"textures":{"AuraSampler":"@volume_color"},"scene":{"terrain":false,"entity":false},"camera":{"distance":1.5,"yaw":45,"pitch":0}}'
    [IO.File]::WriteAllText("$fixture/volume.preview.json",$volumePreview,$utf8)
    Invoke-Lab '/v1/load' @{path="$fixture/volume.preview.json"} | Out-Null
    $volumeFrame=(Invoke-Lab '/v1/render' @{width=537;height=311;time=0}).result
    $renderSize=@($volumeFrame.uniforms | Where-Object name -eq 'RenderSize')[0].uploaded
    $resolve=@($volumeFrame.uniforms | Where-Object name -eq 'ResolvePass')[0].uploaded
    Assert-Lab ($volumeFrame.volumeScale -eq 0.5 -and $renderSize[0] -eq 537 -and $renderSize[1] -eq 311 -and $resolve[0] -eq 1) 'Reduced volume resolves at full odd-sized viewport without GPU errors'
    $volumeSame=(Invoke-Lab '/v1/render' @{width=537;height=311;time=0}).result
    Assert-Lab ($volumeSame.sha256 -eq $volumeFrame.sha256) 'Reduced volume target is cleared and deterministic'
    [IO.File]::WriteAllText("$fixture/volume.preview.json",$volumePreview.Replace('"volumeScale":0.5','"volumeScale":0.1'),$utf8)
    Invoke-Lab '/v1/reload' @{} 422 | Out-Null
    $volumeRetained=(Invoke-Lab '/v1/render' @{width=537;height=311;time=0}).result
    Assert-Lab ($volumeRetained.sha256 -eq $volumeFrame.sha256) 'Invalid volume scale preserves previous program and render state'
    # screen 使用相同离屏契约，验证实际像素、奇数尺寸和 pass 切换。
    [IO.File]::WriteAllText("$fixture/volume.vsh","#version 150`nin vec3 Position;void main(){gl_Position=vec4(Position.xy,0,1);}`n",$utf8)
    $screenPreview=$volumePreview.Replace('"target":"volume"','"target":"screen"')
    [IO.File]::WriteAllText("$fixture/screen.preview.json",$screenPreview,$utf8)
    Invoke-Lab '/v1/load' @{path="$fixture/screen.preview.json"} | Out-Null
    $screenFrame=(Invoke-Lab '/v1/render' @{width=537;height=311;time=0}).result
    $screenResolve=@($screenFrame.uniforms | Where-Object name -eq 'ResolvePass')[0].uploaded
    $screenSize=@($screenFrame.uniforms | Where-Object name -eq 'RenderSize')[0].uploaded
    Assert-Lab ($screenFrame.volumeScale -eq 0.5 -and $screenResolve[0] -eq 1 -and $screenSize[0] -eq 537 -and $screenSize[1] -eq 311) 'Reduced screen resolves at full odd-sized viewport'
    $screenSame=(Invoke-Lab '/v1/render' @{width=537;height=311;time=0}).result
    Assert-Lab ($screenSame.sha256 -eq $screenFrame.sha256) 'Reduced screen clears its intermediate target deterministically'
    $screenImage=[Drawing.Bitmap]::new($screenFrame.capture)
    try {
        $screenLeft=$screenImage.GetPixel(80,155); $screenRight=$screenImage.GetPixel(450,155)
        Assert-Lab ($screenRight.R -gt $screenLeft.R+50 -and $screenLeft.B -gt 40) 'Screen resolve samples the actual low-resolution color attachment'
    } finally { $screenImage.Dispose() }
    [IO.File]::WriteAllText("$fixture/screen.preview.json",$screenPreview.Replace('"volumeScale":0.5','"volumeScale":1'),$utf8)
    Invoke-Lab '/v1/reload' @{} | Out-Null
    $screenDirect=(Invoke-Lab '/v1/render' @{width=320;height=240;time=0}).result
    $screenDirectPass=@($screenDirect.uniforms | Where-Object name -eq 'ResolvePass')[0].uploaded
    Assert-Lab ($screenDirect.volumeScale -eq 1 -and $screenDirectPass[0] -eq -1) 'Full-resolution screen declares the direct pass without sampling the intermediate target'
    Invoke-Lab '/v1/load' @{path='examples/surface.preview.json'} | Out-Null
    $normalAgain=(Invoke-Lab '/v1/render' @{width=320;height=240;time=1.25}).result
    Assert-Lab ($normalAgain.sha256 -eq $first.sha256 -and $normalAgain.volumeScale -eq 1) 'Switching away from reduced volume restores ordinary rendering'
    Invoke-Lab '/v1/shutdown' @{} | Out-Null
    Assert-Lab ($labHelper.WaitForExit(10000)) 'Shutdown exits the hidden helper'
    $stopped = [IO.File]::ReadAllText($labSessionPath) | ConvertFrom-Json
    Assert-Lab ($stopped.state -eq 'stopped') 'Session descriptor records stopped state'
    $report = @{result='PASS';checks=@($labChecks.ToArray());artifacts=$labTestRoot}
    [IO.File]::WriteAllText("$labTestRoot/report.json", ($report | ConvertTo-Json -Depth 8), $utf8)
    $report | ConvertTo-Json -Depth 8
} finally {
    if ($labHelper -and !$labHelper.HasExited) {
        if ($labSession) { try { Invoke-Lab '/v1/shutdown' @{} | Out-Null } catch {} }
        if (!$labHelper.WaitForExit(5000)) {
            if ($labSession) { Stop-Process -Id $labSession.pid -ErrorAction SilentlyContinue }
            $labHelper.Kill()
        }
    }
    $labClient.Dispose()
}
