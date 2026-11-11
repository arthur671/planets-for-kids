# Builds build\dist\Planets-windows.zip: the game plus its own small Java runtime,
# so it runs on any 64-bit Windows PC with nothing else installed.
# Needs JDK 21 or newer (for javac and jpackage) on the PATH.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

& .\gradlew.bat -q installDist
if ($LASTEXITCODE -ne 0) { throw "Gradle build failed" }

# Only the Windows 64-bit native libraries are needed.
$in = 'build\jpackage-input'
if (Test-Path $in) { Remove-Item -Recurse -Force $in }
New-Item -ItemType Directory $in | Out-Null
Get-ChildItem build\install\planets\lib\*.jar |
    Where-Object { $_.Name -notmatch 'natives-(linux|macos)' -and $_.Name -notmatch 'natives-windows-(x86|arm64)' } |
    Copy-Item -Destination $in

if (Test-Path build\dist) { Remove-Item -Recurse -Force build\dist }
& jpackage --type app-image --name Planets --app-version 1.0.0 `
    --input $in --main-jar planets.jar --main-class planets.Main `
    --icon assets\planets.ico --dest build\dist `
    --java-options '-Xmx1g' `
    --add-modules java.base,java.desktop,java.logging,java.management,java.prefs,java.xml,jdk.unsupported `
    --jlink-options '--strip-debug --no-header-files --no-man-pages --compress=zip-6'
if ($LASTEXITCODE -ne 0) { throw "jpackage failed" }

Compress-Archive -Path build\dist\Planets -DestinationPath build\dist\Planets-windows.zip
Get-Item build\dist\Planets-windows.zip | ForEach-Object { "{0}  {1:N0} MB" -f $_.FullName, ($_.Length / 1MB) }
