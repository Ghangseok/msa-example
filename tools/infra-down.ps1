<#
.SYNOPSIS
로컬 실행 인프라를 내린다.

.DESCRIPTION
kind 클러스터 msa, compose 컨테이너(order-db, inventory-db, keycloak, lgtm), 레지스트리 컨테이너를 지운다.
Oracle 데이터 볼륨(order-db-data, inventory-db-data)은 남긴다. 다시 띄우면 데이터가 그대로 있다.
레지스트리에 push한 이미지는 지워진다. 다시 push하면 된다.
다시 띄울 때는 tools/infra-up.ps1을 쓴다.

.PARAMETER RemoveData
Oracle 데이터 볼륨도 지운다. 되돌릴 수 없다. DB를 처음부터 다시 만들 때 쓴다.

.EXAMPLE
.\tools\infra-down.ps1

.EXAMPLE
.\tools\infra-down.ps1 -RemoveData
#>
param(
    [switch]$RemoveData
)

# 네이티브 명령은 종료 코드로 성공을 판단한다 (infra-up.ps1과 같은 이유).
Set-StrictMode -Version Latest

$Root = Split-Path -Parent $PSScriptRoot
$ClusterName = 'msa'
$RegistryName = 'kind-registry'
$EnvFile = Join-Path $Root '.env'
$ComposeFile = Join-Path $Root 'infra/compose/compose.yaml'

function Invoke-Step {
    param(
        [Parameter(Mandatory = $true)][string]$Title,
        [Parameter(Mandatory = $true)][scriptblock]$Command
    )
    Write-Host "==> $Title" -ForegroundColor Cyan
    $global:LASTEXITCODE = 0
    & $Command | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "$Title 실패 (종료 코드 $LASTEXITCODE)"
    }
}

# compose 파일이 .env의 비밀번호를 필수로 읽으므로 내릴 때도 .env가 있어야 한다.
if (-not (Test-Path -LiteralPath $EnvFile)) {
    throw ".env가 없다. 저장소 루트에서 'Copy-Item .env.example .env'로 만들고 값을 채운다."
}

$clusters = @(kind get clusters 2>$null)
if ($clusters -contains $ClusterName) {
    Invoke-Step "kind 클러스터 $ClusterName 지우기" { kind delete cluster --name $ClusterName }
}

$composeArgs = @('compose', '--env-file', $EnvFile, '-f', $ComposeFile, 'down')
if ($RemoveData) {
    $composeArgs += '--volumes'
    Invoke-Step 'compose 컨테이너와 Oracle 데이터 볼륨 지우기' { docker @composeArgs }
} else {
    Invoke-Step 'compose 컨테이너 지우기 (Oracle 데이터 볼륨은 남긴다)' { docker @composeArgs }
}

docker inspect $RegistryName 2>$null | Out-Null
if ($LASTEXITCODE -eq 0) {
    Invoke-Step "레지스트리 $RegistryName 지우기" { docker rm -f -v $RegistryName }
}

Write-Host ''
Write-Host '로컬 실행 인프라를 내렸다.' -ForegroundColor Green
