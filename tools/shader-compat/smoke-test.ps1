param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$fdRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$fdOutput = Join-Path $fdRoot 'build/shader-compat-probe'
$fdClasspathFile = Join-Path $fdRoot 'build/classpath/runClient_minecraftClasspath.txt'
if (!(Test-Path $fdClasspathFile)) { throw 'Run gradlew prepareRunClient first to generate the runtime classpath.' }
$fdClasspath = @((Join-Path $fdRoot 'build/classes/java/main')) + (Get-Content $fdClasspathFile)
$fdClasspathText = $fdClasspath -join ';'
New-Item -ItemType Directory -Force -Path $fdOutput | Out-Null
& (Join-Path $JavaHome 'bin/javac.exe') -encoding UTF-8 -proc:none -classpath $fdClasspathText -d $fdOutput (Join-Path $PSScriptRoot 'RenderStateSmokeTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Probe compilation failed.' }
& (Join-Path $JavaHome 'bin/java.exe') ("-Djdk.net.unixdomain.tmpdir=" + [System.IO.Path]::GetPathRoot($fdRoot)) -classpath ($fdOutput + ';' + $fdClasspathText) 'tennouboshiuzume.mods.FantasyDesire.client.compat.RenderStateSmokeTest' $fdRoot
if ($LASTEXITCODE -ne 0) { throw 'Hidden GL probe failed.' }
