param(
    [string]$VenvPath = "backend\.musicgen_venv"
)

$ErrorActionPreference = "Stop"

if (!(Test-Path $VenvPath)) {
    python -m venv $VenvPath
}

$Python = Join-Path $VenvPath "Scripts\python.exe"

& $Python -m pip install --upgrade pip
& $Python -m pip install torch --index-url https://download.pytorch.org/whl/cpu
& $Python -m pip install -r backend\requirements-musicgen.txt

Write-Host "MusicGen environment is ready: $VenvPath"
