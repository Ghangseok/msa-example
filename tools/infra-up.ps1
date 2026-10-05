<#
.SYNOPSIS
로컬 실행 인프라 전체를 띄운다.

.DESCRIPTION
아래를 순서대로 띄운다. 이미 있는 것은 건너뛰므로 여러 번 실행해도 된다.
  1. 루트 .env 검사. .env.example에 있는 항목이 모두 채워져 있어야 한다.
  2. 로컬 이미지 레지스트리 kind-registry (localhost:5001)
  3. kind 클러스터 msa (infra/kind/cluster.yaml)
  4. 노드의 레지스트리 설정, 레지스트리를 kind 네트워크에 연결
  5. Envoy Gateway(Helm)와 GatewayClass, Gateway (infra/k8s/gateway/)
  6. compose의 order-db, inventory-db, keycloak, lgtm (infra/compose/compose.yaml)

kind 네트워크는 3단계에서 생기므로 compose는 맨 뒤에 띄운다.
내릴 때는 tools/infra-down.ps1을 쓴다.

.EXAMPLE
.\tools\infra-up.ps1
#>

# 네이티브 명령(docker, kind, kubectl, helm)은 종료 코드로 성공을 판단한다.
# PowerShell 5.1에서 $ErrorActionPreference = 'Stop'을 쓰면 stderr에 쓴 진행 메시지까지 오류로 보고 멈출 수 있다.
Set-StrictMode -Version Latest

$Root = Split-Path -Parent $PSScriptRoot
$ClusterName = 'msa'
$Context = "kind-$ClusterName"
$RegistryName = 'kind-registry'
$RegistryPort = 5001
$RegistryImage = 'registry:3.1.2'
$EnvoyGatewayVersion = 'v1.9.2'

$EnvFile = Join-Path $Root '.env'
$EnvExampleFile = Join-Path $Root '.env.example'
$KindConfig = Join-Path $Root 'infra/kind/cluster.yaml'
$RegistryHostsFile = Join-Path $Root 'infra/kind/registry-hosts.toml'
$LocalRegistryHostingFile = Join-Path $Root 'infra/kind/local-registry-hosting.yaml'
$GatewayDir = Join-Path $Root 'infra/k8s/gateway'
$ComposeFile = Join-Path $Root 'infra/compose/compose.yaml'
$KeycloakCheckUrl = 'http://localhost:8180/realms/msa/.well-known/openid-configuration'

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

function Read-DotEnv {
    param([Parameter(Mandatory = $true)][string]$Path)
    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        $text = $line.Trim()
        if ($text -eq '' -or $text.StartsWith('#')) { continue }
        $index = $text.IndexOf('=')
        if ($index -lt 1) { continue }
        $values[$text.Substring(0, $index).Trim()] = $text.Substring($index + 1).Trim()
    }
    return $values
}

# 1. .env 검사
if (-not (Test-Path -LiteralPath $EnvFile)) {
    throw ".env가 없다. 저장소 루트에서 'Copy-Item .env.example .env'로 만들고 값을 채운다."
}
$expected = Read-DotEnv $EnvExampleFile
$actual = Read-DotEnv $EnvFile
$missing = @($expected.Keys | Where-Object { -not $actual.ContainsKey($_) -or $actual[$_] -eq '' } | Sort-Object)
if ($missing.Count -gt 0) {
    throw ".env에 값이 비어 있는 항목이 있다: $($missing -join ', ')"
}

docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Docker에 연결하지 못했다. Docker Desktop을 켠다. (docs/references/docker-desktop.md 5절)"
}

# 2. 로컬 이미지 레지스트리
$registryRunning = docker inspect -f '{{.State.Running}}' $RegistryName 2>$null
if ($LASTEXITCODE -ne 0) {
    Invoke-Step "레지스트리 $RegistryName 만들기 ($RegistryImage, localhost:$RegistryPort)" {
        docker run -d --restart=always -p "127.0.0.1:${RegistryPort}:5000" --network bridge --name $RegistryName $RegistryImage
    }
} elseif ($registryRunning -ne 'true') {
    Invoke-Step "레지스트리 $RegistryName 시작" { docker start $RegistryName }
} else {
    Write-Host "==> 레지스트리 $RegistryName 실행 중"
}

# 3. kind 클러스터
$clusters = @(kind get clusters 2>$null)
if ($clusters -contains $ClusterName) {
    # PC나 Docker Desktop을 다시 켠 뒤에는 노드 컨테이너가 멈춰 있을 수 있다.
    $nodes = @(kind get nodes --name $ClusterName)
    Invoke-Step "kind 클러스터 $ClusterName 이 이미 있다. 노드 컨테이너 시작" { docker start $nodes }
} else {
    Invoke-Step "kind 클러스터 $ClusterName 만들기" { kind create cluster --config $KindConfig }
}
Invoke-Step "노드 3개 Ready 대기" {
    kubectl --context $Context wait --for=condition=Ready nodes --all --timeout=5m
}

# 4. 노드가 localhost:5001 이미지를 레지스트리 컨테이너에서 받게 한다 (kind 공식 예제와 같은 방식)
$registryDir = "/etc/containerd/certs.d/localhost:$RegistryPort"
foreach ($node in @(kind get nodes --name $ClusterName)) {
    Invoke-Step "$node 노드에 레지스트리 설정 복사" {
        docker exec $node mkdir -p $registryDir
        if ($LASTEXITCODE -eq 0) {
            docker cp $RegistryHostsFile "${node}:$registryDir/hosts.toml"
        }
    }
}
$registryNetwork = docker inspect -f '{{json .NetworkSettings.Networks.kind}}' $RegistryName
if ($registryNetwork -eq 'null') {
    Invoke-Step "레지스트리를 kind 네트워크에 연결" { docker network connect kind $RegistryName }
}
Invoke-Step "레지스트리 주소 ConfigMap 적용" {
    kubectl --context $Context apply -f $LocalRegistryHostingFile
}

# 5. Envoy Gateway
Invoke-Step "Envoy Gateway $EnvoyGatewayVersion 설치 (Helm)" {
    helm upgrade --install eg oci://docker.io/envoyproxy/gateway-helm --version $EnvoyGatewayVersion `
        --namespace envoy-gateway-system --create-namespace --kube-context $Context
}
Invoke-Step "Envoy Gateway 컨트롤러 준비 대기" {
    kubectl --context $Context wait --timeout=5m --namespace envoy-gateway-system deployment/envoy-gateway --for=condition=Available
}
Invoke-Step "EnvoyProxy, GatewayClass, Gateway 적용" {
    kubectl --context $Context apply -f $GatewayDir
}
Invoke-Step "Gateway 준비 대기" {
    kubectl --context $Context wait --timeout=5m --namespace default gateway/msa-gateway --for=condition=Programmed
}

# 6. 클러스터 밖의 상태 있는 인프라
Invoke-Step "compose 컨테이너 시작 (Oracle을 처음 만들 때는 몇 분 걸린다)" {
    docker compose --env-file $EnvFile -f $ComposeFile up -d --wait --wait-timeout 600
}
Write-Host "==> Keycloak realm msa 준비 대기" -ForegroundColor Cyan
$deadline = (Get-Date).AddMinutes(3)
while ($true) {
    try {
        Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 -Uri $KeycloakCheckUrl | Out-Null
        break
    } catch {
        if ((Get-Date) -gt $deadline) {
            throw "Keycloak이 3분 안에 준비되지 않았다. 'docker logs keycloak'으로 확인한다."
        }
        Start-Sleep -Seconds 3
    }
}

Write-Host ''
Write-Host '로컬 실행 인프라가 준비됐다.' -ForegroundColor Green
Write-Host '  Gateway       http://localhost/'
Write-Host "  레지스트리    localhost:$RegistryPort"
Write-Host '  order-db      localhost:1521/XEPDB1 (ORDER_SVC)'
Write-Host '  inventory-db  localhost:1522/XEPDB1 (INVENTORY_SVC)'
Write-Host '  Keycloak      http://localhost:8180 (realm msa)'
Write-Host '  Grafana       http://localhost:3000'
