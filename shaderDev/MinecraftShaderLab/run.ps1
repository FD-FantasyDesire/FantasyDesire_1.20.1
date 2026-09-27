param(
    [string]$Shader = '',
    [string]$MinecraftJar = '',
    [switch]$Verify,
    [switch]$CompileOnly,
    [switch]$DebugServer,
    [ValidateRange(0,65535)][int]$Port = 0,
    [string]$SessionFile = '',
    [switch]$SmokeTest
)
$ErrorActionPreference = 'Stop'
$labRoot = $PSScriptRoot
$labModes = @($Verify, $CompileOnly, $DebugServer, $SmokeTest) | Where-Object { $_ }
if (@($labModes).Count -gt 1) { throw 'Choose only one of -Verify, -CompileOnly, -DebugServer or -SmokeTest.' }
# Explorer 可能还持有旧环境；同时检查当前环境、用户环境、系统环境和 PATH。
$labJavaCandidates = @($env:JAVA_HOME, [Environment]::GetEnvironmentVariable('JAVA_HOME','User'), [Environment]::GetEnvironmentVariable('JAVA_HOME','Machine'))
$labJava = $null
foreach ($candidate in $labJavaCandidates) {
    if ($candidate -and (Test-Path -LiteralPath "$candidate/bin/javac.exe") -and (Test-Path -LiteralPath "$candidate/bin/java.exe")) { $labJava = "$candidate/bin"; break }
}
if (!$labJava) {
    $labCompiler = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($labCompiler) { $labJava = Split-Path $labCompiler.Source }
}
if (!$labJava) { throw 'JDK 17 or newer was not found. Set JAVA_HOME to a JDK directory.' }
$labDeps = Join-Path $labRoot '.deps'
$labClasses = Join-Path $labRoot 'build/classes'
New-Item -ItemType Directory -Force $labDeps,$labClasses | Out-Null
# 固定版本；先复用本机 Gradle 缓存，缺失时才从 Maven Central 下载。
$labArtifacts = @(
    @('org.lwjgl','lwjgl','3.3.1',''), @('org.lwjgl','lwjgl','3.3.1','natives-windows'),
    @('org.lwjgl','lwjgl-glfw','3.3.1',''), @('org.lwjgl','lwjgl-glfw','3.3.1','natives-windows'),
    @('org.lwjgl','lwjgl-opengl','3.3.1',''), @('org.lwjgl','lwjgl-opengl','3.3.1','natives-windows'),
    @('com.google.code.gson','gson','2.10.1',''), @('org.joml','joml','1.10.5','')
)
foreach ($artifact in $labArtifacts) {
    $group,$name,$version,$classifier = $artifact
    $suffix = if ($classifier) { "-$classifier" } else { '' }
    $file = "$name-$version$suffix.jar"
    $destination = Join-Path $labDeps $file
    if (Test-Path -LiteralPath $destination) { continue }
    $cacheRoot = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $env:USERPROFILE '.gradle' }
    $cached = Get-ChildItem -LiteralPath "$cacheRoot/caches/modules-2/files-2.1/$group/$name/$version" -Filter $file -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($cached) { Copy-Item -LiteralPath $cached.FullName -Destination $destination }
    else {
        $groupPath = $group.Replace('.','/')
        $uri = "https://repo.maven.apache.org/maven2/$groupPath/$name/$version/$file"
        Write-Host "下载 $file"
        Invoke-WebRequest -Uri $uri -OutFile "$destination.part" -UseBasicParsing
        Move-Item -LiteralPath "$destination.part" -Destination $destination
    }
}
$labSources = @(Get-ChildItem -LiteralPath "$labRoot/src/main/java" -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
$labMain = Join-Path $labClasses '.compiled'
$labLatest = ($labSources | Get-Item | Sort-Object LastWriteTime -Descending | Select-Object -First 1).LastWriteTime
if (!(Test-Path -LiteralPath $labMain) -or (Get-Item -LiteralPath $labMain).LastWriteTime -lt $labLatest) {
    & "$labJava/javac.exe" -encoding UTF-8 --release 17 -cp "$labDeps/*" -d $labClasses @labSources
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    [IO.File]::WriteAllText($labMain, [DateTime]::UtcNow.ToString('O'))
}
if ($CompileOnly) { exit 0 }
$labArgs = @('--root', $labRoot)
if ($Shader) { $labArgs += @('--shader', [IO.Path]::GetFullPath($Shader)) }
if ($MinecraftJar) { $labArgs += @('--minecraft', [IO.Path]::GetFullPath($MinecraftJar)) }
if ($Verify) { $labArgs += '--verify' }
if ($SmokeTest) { $labArgs += '--smoke' }
if ($DebugServer) { $labArgs += @('--debug', '--port', $Port.ToString()) }
if ($SessionFile) { $labArgs += @('--session', [IO.Path]::GetFullPath($SessionFile)) }
& "$labJava/java.exe" '-Dfile.encoding=UTF-8' -cp "$labClasses;$labDeps/*" lab.Main @labArgs
exit $LASTEXITCODE
