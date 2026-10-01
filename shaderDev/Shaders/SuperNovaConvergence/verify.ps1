param([switch]$VisualOnly, [int]$Samples = 7,
    [ValidateSet('gather','expand','cloud','fade')][string[]]$BenchmarkPhases = @('gather','expand','cloud','fade'),
    [int[]]$Counts = @(1,4,8,16), [int[]]$Qualities = @(32,56,80),
    [switch]$CompareResolution, [string]$BaselinePath = '')
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$labRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../MinecraftShaderLab'))
$outRoot = Join-Path $labRoot ('build/supernova/' + [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss'))
$null = New-Item -ItemType Directory -Force $outRoot
$sessionFile = Join-Path $outRoot 'session.json'
$labProcess = $null
$session = $null
$frames = [Collections.Generic.List[object]]::new()
$measurements = [Collections.Generic.List[object]]::new()
function Request-Lab([string]$Command, $Body = $null) {
    $parameters = @{Uri=($session.baseUrl + '/v1/' + $Command); Headers=@{'X-ShaderLab-Token'=$session.token}; TimeoutSec=40}
    if ($null -ne $Body) {
        $parameters.Method = 'Post'
        $parameters.ContentType = 'application/json'
        $parameters.Body = $Body | ConvertTo-Json -Depth 12 -Compress
    }
    $response = Invoke-RestMethod @parameters
    if (!$response.ok) { throw ($response | ConvertTo-Json -Depth 12) }
    return $response.result
}
function Capture-Lab([string]$Name, [double]$Time, [int]$Width=800, [int]$Height=600) {
    $frame = Request-Lab 'render' @{time=$Time; width=$Width; height=$Height; attachments=$true}
    $frames.Add(@{name=$Name; time=$Time; path=$frame.capture; gpuMs=$frame.customGpuMs; hash=$frame.sha256})
    Write-Host "$Name : $($frame.customGpuMs) ms"
    return $frame
}
try {
    $arguments = '/d /c ""' + $labRoot + '\run.cmd" -DebugServer -SessionFile "' + $sessionFile + '""'
    $labProcess = Start-Process -FilePath $env:ComSpec -WindowStyle Hidden -PassThru -ArgumentList $arguments -RedirectStandardOutput "$outRoot/server.out.log" -RedirectStandardError "$outRoot/server.err.log"
    $deadline = [DateTime]::UtcNow.AddSeconds(60)
    while (!(Test-Path -LiteralPath $sessionFile)) {
        if ($labProcess.HasExited) { throw "Server exited: $outRoot/server.out.log" }
        if ([DateTime]::UtcNow -gt $deadline) { throw 'Startup timeout' }
        Start-Sleep -Milliseconds 100
    }
    $session = [IO.File]::ReadAllText($sessionFile) | ConvertFrom-Json
    if ($session.state -ne 'ready' -or $session.visible) { throw 'Expected a ready hidden session' }
    $status = Request-Lab 'load' @{path=(Join-Path $PSScriptRoot 'main.preview.json')}
    $null = Request-Lab 'configure' @{uniforms=@{RepeatPreview=0}}
    foreach ($phase in @(@('start',0),@('gather',1.7),@('before',2.783333),@('flash',2.825),@('expand',3.1),@('cloud',3.75),@('fade',5.3),@('end',5.9))) {
        $null = Capture-Lab $phase[0] $phase[1]
    }
    $repeat = Request-Lab 'render' @{time=5.9; width=800; height=600}
    if ($repeat.sha256 -ne $frames[$frames.Count-1].hash) { throw 'Frozen frame is not deterministic' }
    $endScene = Join-Path (Split-Path $frames[$frames.Count-1].path -Parent) 'scene-color.png'
    if ((Get-FileHash -LiteralPath $endScene).Hash -ne (Get-FileHash -LiteralPath $frames[$frames.Count-1].path).Hash) { throw 'Effect leaves pixels after its lifetime' }
    $null = Request-Lab 'reload' @{}
    $reloaded = Request-Lab 'render' @{time=5.9; width=800; height=600}
    if ($reloaded.sha256 -ne $repeat.sha256) { throw 'Reload changes the empty end frame' }
    $null = Request-Lab 'configure' @{uniforms=@{RepeatPreview=0}}
    foreach ($case in @(@('side',90,8,13),@('above',-35,65,13),@('inside',25,12,2))) {
        $null = Request-Lab 'configure' @{camera=@{yaw=$case[1];pitch=$case[2];distance=$case[3]}}
        $null = Capture-Lab $case[0] 3.75
    }
    $null = Request-Lab 'configure' @{camera=@{yaw=25;pitch=12;distance=13}}
    $yawRadians = 25.0 * [Math]::PI / 180.0
    $pitchRadians = 12.0 * [Math]::PI / 180.0
    $eye = @((13*[Math]::Cos($pitchRadians)*[Math]::Sin($yawRadians)), (13*[Math]::Sin($pitchRadians)+1), (13*[Math]::Cos($pitchRadians)*[Math]::Cos($yawRadians)))
    $null = Request-Lab 'configure' @{uniforms=@{EffectCenter=$eye}}
    $null = Capture-Lab 'camera-at-center' 3.1
    $null = Request-Lab 'configure' @{uniforms=@{EffectCenter=@(0,3.5,0)}}
    $null = Request-Lab 'configure' @{uniforms=@{CloudOpacity=1}}
    $opaque = Capture-Lab 'opacity-100' 4.5
    $null = Request-Lab 'configure' @{uniforms=@{CloudOpacity=0.5}}
    $half = Capture-Lab 'opacity-50' 4.5
    $null = Request-Lab 'configure' @{uniforms=@{CloudOpacity=0}}
    $transparent = Capture-Lab 'opacity-0' 4.5
    $opacityScene = Join-Path (Split-Path $transparent.capture -Parent) 'scene-color.png'
    if ((Get-FileHash -LiteralPath $transparent.capture).Hash -ne (Get-FileHash -LiteralPath $opacityScene).Hash) { throw 'Zero cloud opacity leaves visible cloud pixels' }
    $images = @([Drawing.Bitmap]::new($opaque.capture),[Drawing.Bitmap]::new($half.capture),[Drawing.Bitmap]::new($transparent.capture))
    try {
        $changed = 0; $maxError = 0.0
        for ($y=0;$y -lt 600;$y+=3) { for ($x=0;$x -lt 800;$x+=3) {
            $a=$images[0].GetPixel($x,$y); $b=$images[1].GetPixel($x,$y); $c=$images[2].GetPixel($x,$y)
            foreach ($channel in @('R','G','B')) {
                $maxError=[Math]::Max($maxError,[Math]::Abs($b.$channel - ($a.$channel+$c.$channel)*0.5))
            }
            if ([Math]::Abs($a.R-$c.R)+[Math]::Abs($a.G-$c.G)+[Math]::Abs($a.B-$c.B) -gt 5) { $changed++ }
        } }
        if ($changed -lt 100 -or $maxError -gt 2.5) { throw "Opacity does not scale premultiplied cloud contribution: changed=$changed maxError=$maxError" }
        Write-Host "Cloud opacity 50% verified: changed=$changed max RGB error=$maxError / 255"
    } finally { foreach ($image in $images) { $image.Dispose() } }
    $null = Request-Lab 'configure' @{uniforms=@{CloudOpacity=0.5}}
    foreach ($quality in @(32,56,80)) {
        $null = Request-Lab 'configure' @{uniforms=@{VolumeSteps=$quality}}
        $null = Capture-Lab "quality-$quality" 3.75
    }
    $null = Request-Lab 'configure' @{uniforms=@{VolumeSteps=56;EffectCenter=@(-1.5,1.5,-2.5);EffectScale=0.07}}
    foreach ($hiddenTime in @(1.7,3.75)) {
        $hidden = Capture-Lab "occluded-$hiddenTime" $hiddenTime
        $hiddenScene = Join-Path (Split-Path $hidden.capture -Parent) 'scene-color.png'
        if ((Get-FileHash -LiteralPath $hidden.capture).Hash -ne (Get-FileHash -LiteralPath $hiddenScene).Hash) { throw 'Fully occluded effect leaks through terrain' }
    }
    $null = Request-Lab 'configure' @{uniforms=@{EffectCenter=@(0,3.5,0);EffectScale=1}}
    $null = Capture-Lab 'wide' 3.75 1280 720
    $null = Capture-Lab 'portrait' 3.75 540 960
    $null = Capture-Lab 'odd-size' 3.75 537 311
    foreach ($seed in @(803,9999)) {
        $null = Request-Lab 'configure' @{uniforms=@{Seed=$seed}}
        $null = Capture-Lab "seed-$seed" 3.75
    }
    foreach ($ignition in @(0.5,12.0)) {
        $null = Request-Lab 'configure' @{uniforms=@{ExplosionTime=$ignition;Seed=17}}
        $null = Capture-Lab "countdown-$ignition" ($ignition-1.0/60)
    }
    $null = Request-Lab 'configure' @{uniforms=@{ExplosionTime=2.8;InstanceCount=8;InstanceColumns=4;InstanceSpacing=5;EffectScale=0.65};camera=@{distance=24}}
    $null = Capture-Lab 'multi-gather' 1.7 1280 720
    $null = Capture-Lab 'multi-cloud' 3.75 1280 720
    $null = Request-Lab 'configure' @{uniforms=@{AgeStagger=0.25}}
    $null = Capture-Lab 'staggered' 3.75 1280 720
    foreach ($variant in @('full','half')) {
        $preset = if ($variant -eq 'full') { 'full.preview.json' } else { 'main.preview.json' }
        $null = Request-Lab 'load' @{path=(Join-Path $PSScriptRoot $preset)}
        $null = Request-Lab 'configure' @{camera=@{distance=24};uniforms=@{RepeatPreview=0;InstanceCount=8;InstanceColumns=4;InstanceSpacing=5;EffectScale=0.65;AgeStagger=0;Seed=17}}
        $null = Capture-Lab "$variant-8-cloud" 3.75 1280 720
        $null = Capture-Lab "$variant-8-fade" 5.3 1280 720
    }
    if (!$VisualOnly) {
        if ($Samples -lt 3 -or $Samples -gt 50) { throw 'Samples must be in [3,50]' }
        $variants = @(@{name='half';path=(Join-Path $PSScriptRoot 'main.preview.json')})
        if ($CompareResolution) { $variants = @(@{name='full';path=(Join-Path $PSScriptRoot 'full.preview.json')}) + $variants }
        if ($BaselinePath) { $variants = @(@{name='before';path=[IO.Path]::GetFullPath($BaselinePath)}) + $variants }
        foreach ($variant in $variants) {
        $null = Request-Lab 'load' @{path=$variant.path}
        $null = Request-Lab 'configure' @{uniforms=@{RepeatPreview=0}}
        foreach ($resolution in @(@(1280,720),@(1920,1080))) {
            foreach ($layout in @('spread','overlap')) {
                foreach ($count in $Counts) {
                    $columns = [int][Math]::Ceiling([Math]::Sqrt($count))
                    $spacing = if ($layout -eq 'spread') { 5 } else { 0 }
                    $null = Request-Lab 'configure' @{camera=@{distance=24};uniforms=@{InstanceCount=$count;InstanceColumns=$columns;InstanceSpacing=$spacing;EffectScale=0.65;AgeStagger=0;Seed=17}}
                    foreach ($quality in $Qualities) {
                        $null = Request-Lab 'configure' @{uniforms=@{VolumeSteps=$quality;TrailSteps=18}}
                        foreach ($phaseName in $BenchmarkPhases) {
                            $phaseTimes = @{gather=1.7;expand=3.1;cloud=3.75;fade=5.3}
                            $phase = @($phaseName,$phaseTimes[$phaseName])
                            $times = [Collections.Generic.List[double]]::new()
                            for ($i=0; $i -lt $Samples+3; $i++) {
                                $frame = Request-Lab 'render' @{width=$resolution[0];height=$resolution[1];time=$phase[1]}
                                if ($null -eq $frame.customGpuMs) { throw 'GPU timer unavailable' }
                                if ($i -ge 3) { $times.Add($frame.customGpuMs) }
                            }
                            $sorted = @($times | Sort-Object)
                            $row = @{variant=$variant.name;volumeScale=$frame.volumeScale;width=$resolution[0];height=$resolution[1];layout=$layout;instances=$count;volumeSteps=$quality;phase=$phase[0];samples=$times.ToArray();capture=$frame.capture;medianMs=$sorted[[int][Math]::Floor($sorted.Count/2)];p95Ms=$sorted[[int][Math]::Ceiling($sorted.Count*0.95)-1]}
                            $measurements.Add($row)
                            Write-Host "$($variant.name) $($resolution[0])x$($resolution[1]) $layout $count / $quality / $($phase[0]): $($row.medianMs) ms"
                        }
                    }
                }
            }
        }
        }
    }
    $report = @{gpu=$status.gpu;opengl=$status.opengl;frames=$frames.ToArray();measurements=$measurements.ToArray();samples=$Samples;warmup=3;result='PASS';date=[DateTime]::UtcNow.ToString('o')}
    [IO.File]::WriteAllText("$outRoot/report.json", ($report | ConvertTo-Json -Depth 15), [Text.UTF8Encoding]::new($false))
    $sheet = [Drawing.Bitmap]::new(1200, [int]([Math]::Ceiling($frames.Count/3)*325))
    $graphics = [Drawing.Graphics]::FromImage($sheet)
    $font = [Drawing.Font]::new('Arial',12)
    try {
        $graphics.Clear([Drawing.Color]::FromArgb(15,18,22))
        for ($i=0;$i -lt $frames.Count;$i++) {
            $x = ($i%3)*400; $y = [int][Math]::Floor($i/3)*325
            $picture = [Drawing.Image]::FromFile($frames[$i].path)
            try {
                $factor = [Math]::Min(400.0/$picture.Width,300.0/$picture.Height)
                $w = [int]($picture.Width*$factor); $h = [int]($picture.Height*$factor)
                $graphics.DrawImage($picture,$x+[int]((400-$w)/2),$y,$w,$h)
            } finally { $picture.Dispose() }
            $graphics.DrawString(($frames[$i].name + ' / ' + $frames[$i].time + 's'),$font,[Drawing.Brushes]::White,$x+8,$y+301)
        }
        $sheet.Save("$outRoot/contact-sheet.png",[Drawing.Imaging.ImageFormat]::Png)
    } finally { $font.Dispose(); $graphics.Dispose(); $sheet.Dispose() }
    Write-Host "REPORT: $outRoot/report.json"
    Write-Host "SHEET: $outRoot/contact-sheet.png"
} finally {
    if ($null -ne $session -and $session.state -eq 'ready') { $null = Request-Lab 'shutdown' @{} }
    if ($null -ne $labProcess -and !$labProcess.WaitForExit(10000)) { throw 'Server did not exit after shutdown' }
}
