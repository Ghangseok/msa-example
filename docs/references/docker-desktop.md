# Docker Desktop 설정과 문제 해결

- 확인한 날: 2026-10-04
- 성격: 참고 자료(로컬 환경 운영 메모). Docker, kind, Testcontainers에 문제가 생기면 이 문서부터 본다.
- 관련: [ADR-0003](../adr/0003-local-k8s-runtime.md), [ADR-0005](../adr/0005-oracle-xe-instance-per-service.md), [design/architecture.md](../design/architecture.md)

## 1. 이 PC의 상태 (2026-10-04)

| 항목 | 값 | 비고 |
|---|---|---|
| Docker Desktop | 4.25.1 | 최신은 4.93.0. 업데이트 필요 (3-1절) |
| Docker Engine | 24.0.6, API 1.43 | |
| 백엔드 | WSL 2 (WSL 3.0.1, 커널 6.18) | cgroup v2. 최신 kind 노드 이미지를 쓸 수 있다 |
| Docker가 쓰는 메모리 | 약 11.6GiB | `.wslconfig`가 없어서 WSL 기본값(PC 메모리 23.8GB의 50%)이 쓰인다. 2026-10-04에 `.wslconfig`(memory=14GB, swap=4GB)를 만들었다. `wsl --shutdown` 뒤에 적용된다 |
| Docker가 쓰는 CPU | 12개 | WSL 기본값(전부) |
| 디스크 이미지 위치 | `C:\Users\Administrator\AppData\Local\Docker\wsl` | C: 여유 117GB |
| 내장 Kubernetes | 꺼짐 (v1.28.2) | kind를 쓰므로 끈 채로 둔다 |
| containerd 이미지 저장소 | 꺼짐 | 끈 채로 둔다 (5절) |
| TCP 2375 노출 | 꺼짐 | 끈 채로 둔다 |
| Resource Saver | 켜짐 | 그대로 둬도 된다 |
| 이미지 / 컨테이너 | 0개 / 0개 | |
| 볼륨 | 10개, 927MB, 모두 미사용 | 4~6년 전 것. 내용을 확인한 뒤 지울지 정한다 |
| 함께 쓰는 도구 | kind v0.33.0, Helm v4.3.0, kubectl(Docker Desktop 동봉) | |
| 이 프로젝트가 쓸 호스트 포트 | 80, 1521, 1522, 1523, 3000, 4317, 4318, 5001, 8180 모두 비어 있음 | 3-5절 |

## 2. 설정은 어디서 하는가

| 설정 | 위치 | 비고 |
|---|---|---|
| **메모리, CPU, 스왑** | `%UserProfile%\.wslconfig` 파일 | WSL 2 백엔드에서는 Docker Desktop 화면에 메모리 슬라이더가 없다. 파일을 고친 뒤 `wsl --shutdown`을 실행하고 Docker Desktop을 다시 켠다. 이 설정은 Docker뿐 아니라 모든 WSL 배포판에 적용된다 |
| 디스크 이미지 위치 | Settings > Resources > Advanced | C: 공간이 부족하면 다른 드라이브로 옮긴다 |
| Resource Saver | Settings > Resources > Advanced | 컨테이너가 없을 때 자원을 줄인다. 컨테이너가 뜨면 저절로 풀린다 |
| 프록시 | Settings > Resources > Proxies | 회사망에서 이미지 내려받기가 막힐 때 |
| Docker 내부 서브넷 | Settings > Resources > Network | VPN과 주소가 겹칠 때 |
| Docker 엔진(daemon.json) | Settings > Docker Engine | JSON을 잘못 쓰면 Docker가 뜨지 않는다. 지금은 기본값 그대로 둔다 |
| 내장 Kubernetes | Settings > Kubernetes | 끈 채로 둔다 |
| containerd 이미지 저장소 | 4.25: Settings > Features in development > Beta features. 최신 버전: Settings > General | 끈 채로 둔다 |
| 로그인할 때 자동 시작 | Settings > General > Start Docker Desktop when you log in | 켜 두면 "Docker가 꺼져 있음" 오류를 덜 겪는다 |
| 업데이트 | Settings > Software updates | |
| 진단 | 상단의 벌레 아이콘(Troubleshoot) | 재시작, 초기화, 진단 정보 수집 |
| 설정 파일 | `%APPDATA%\Docker\settings.json` (4.25). 최신 버전은 `settings-store.json` | 직접 고치지 말고 읽기만 한다. 이 파일의 `memoryMiB`(지금 2048)는 Hyper-V 백엔드용이라 WSL 2에서는 쓰이지 않는다 |

## 3. 이 프로젝트의 권장 설정

### 3-1. 개발을 시작하기 전에

1. **Docker Desktop을 최신으로 업데이트한다.** 4.44.2 이하의 Windows용 Docker Desktop에는 컨테이너가 호스트로 빠져나올 수 있는 취약점(CVE-2025-9074, CVSS 9.3)이 있고 4.44.3에서 고쳐졌다. `[문헌]` 지금은 이미지와 컨테이너가 하나도 없어서 업데이트하기에 가장 부담이 적은 때다.
2. 업데이트한 뒤 확인한다.
   - containerd 이미지 저장소가 꺼져 있는가 (5절의 kind 이미지 적재 문제)
   - 내장 Kubernetes가 꺼져 있는가
3. **`.wslconfig`로 메모리를 14GB로 둔다.** 서비스마다 Oracle XE를 하나씩 띄우므로(ADR-0005) 기본값(약 11.6GiB)으로는 통합 테스트까지 함께 돌리기 빠듯하다. 2026-10-04에 만들어 두었다.

   `C:\Users\Administrator\.wslconfig`

   ```ini
   [wsl2]
   memory=14GB
   swap=4GB
   ```

   적용: PowerShell에서 `wsl --shutdown`을 실행하고 Docker Desktop을 다시 켠다. 확인: `docker info --format "{{.MemTotal}}"` 값이 지금(약 12,450,000,000 바이트)보다 늘어 약 14GB 가까이 나오는지 본다. 커널이 일부를 쓰므로 14GB보다 약간 작게 나온다.
4. (선택) 로그인할 때 자동 시작을 켠다.

### 3-2. 설정값

| 항목 | 권장 | 이유 |
|---|---|---|
| 백엔드 | WSL 2 (지금 그대로) | Hyper-V보다 빠르다 |
| 메모리 | 14GB (`.wslconfig`) | 3-3절 예산. PC 24GB 중 Windows에 약 10GB가 남는다 |
| CPU | 기본값(12개) 그대로 | Oracle XE는 어차피 2개까지만 쓴다 |
| 내장 Kubernetes | 끔 | kind를 쓴다. 둘 다 켜면 자원을 이중으로 쓰고 kubectl 컨텍스트가 헷갈린다 |
| containerd 이미지 저장소 | 끔 | `kind load docker-image`가 실패할 수 있다 (5절) |
| TCP 2375 노출 | 끔 | 인증 없는 원격 실행 통로가 된다. Testcontainers는 이것 없이 동작한다 |
| Docker 엔진 JSON | 기본값 | 바꿀 이유가 없다 |
| Resource Saver | 켬 (그대로) | 영향 없음 |

### 3-3. 메모리 예산 (추정)

| 구성 요소 | 대략 | 근거 |
|---|---|---|
| kind 노드 3개 (control-plane 1 + worker 2) | 1.5~2GB | `[추론]` |
| 주문·재고 Pod 4개 (Spring Boot) | 약 2GB (Pod당 0.5GB로 가정) | 가정이다. 실제 한도는 plan에서 정한다 |
| Oracle 21c XE 2개 (`order-db`, `inventory-db`) | 약 4GB | XE 하나당 상한 2GB `[문헌]` |
| Keycloak (개발 모드) | 0.5~1GB | `[현장]` |
| Gateway 컨트롤러, 로컬 레지스트리 | 약 0.5GB | `[추론]` |
| 관측 도구 `grafana/otel-lgtm` | 약 1GB | `[추론]` |
| **실행 중 합계** | **약 10~11GB** | |
| 테스트 중 Testcontainers Oracle | +2GB | 통합 테스트를 돌릴 때만 |

14GB면 전체 실행(약 11GB)과 통합 테스트(+2GB)를 함께 돌릴 수 있지만 빠듯하다. 부족하면 테스트하는 동안 `lgtm`을 멈추거나 16GB로 올린다. 결제 서비스와 `payment-db`가 들어오면 2GB 이상 더 든다. 그때 16GB로 늘리거나, 통합 테스트를 돌릴 때 클러스터를 멈춘다.

### 3-4. 이 프로젝트가 만들 Docker 자원 (예정)

| 자원 | 이름 | 정의할 곳 |
|---|---|---|
| kind 클러스터 | `msa` (노드 컨테이너 `msa-control-plane`, `msa-worker`, `msa-worker2`) | `infra/kind/` |
| Docker 네트워크 | `kind` (kind가 만든다) | — |
| 로컬 이미지 레지스트리 | `kind-registry` (`localhost:5001`). `kind load` 대신 쓴다 | `infra/kind/` |
| Oracle XE 컨테이너 + 데이터 볼륨 (서비스마다) | `order-db` + `order-db-data`, `inventory-db` + `inventory-db-data`, 나중에 `payment-db` | `infra/compose/` |
| Keycloak 컨테이너 | `keycloak` | `infra/compose/` |
| 관측 도구 컨테이너 | `lgtm` (`grafana/otel-lgtm`) | `infra/compose/` |
| 테스트용 임시 컨테이너 | Testcontainers가 띄우고 지운다 (`testcontainers-ryuk` 포함) | 각 서비스 테스트 |

- compose의 Oracle과 Keycloak은 `kind` 네트워크에 붙인다(compose의 external network). 그래야 클러스터 안의 Pod가 `order-db:1521`, `inventory-db:1521`, `keycloak:8080`처럼 이름으로 찾는다. `[현장]`
- kind 클러스터는 언제 지워도 된다. 앱은 무상태이고, 데이터는 클러스터 밖 Oracle에 있다.

### 3-5. 호스트 포트 (예정)

| 포트 | 용도 |
|---|---|
| 80 | Gateway (kind의 extraPortMappings). `http://localhost/orders` |
| 1521 | `order-db` (주문 DB) |
| 1522 | `inventory-db` (재고 DB). 컨테이너 안은 1521 |
| 1523 | `payment-db` (결제 DB, 후속) |
| 8180 | Keycloak (컨테이너 안은 8080) |
| 5001 | 로컬 이미지 레지스트리 |
| 3000 | Grafana (`lgtm`) |
| 4317 / 4318 | OTLP gRPC / HTTP (`lgtm`). Pod에서는 `lgtm:4318` |
| 8080 | 비워 둔다. 클러스터 밖에서 서비스 하나를 직접 띄워 디버깅할 때 쓴다 |
| 127.0.0.1의 임의 포트 | kind API 서버 (kind가 정한다) |

## 4. 자주 쓰는 확인 명령 (PowerShell)

```powershell
# Docker 상태 한 줄 요약
docker info --format "Server={{.ServerVersion}} CPUs={{.NCPU}} Mem={{.MemTotal}} Cgroup={{.CgroupVersion}}"
docker version --format "Client={{.Client.APIVersion}} Server={{.Server.APIVersion}}"
docker system df            # 이미지·볼륨·캐시가 쓰는 디스크
docker stats --no-stream    # 컨테이너별 CPU·메모리
docker ps -a
docker network inspect kind

# WSL
wsl -l -v
wsl --shutdown              # .wslconfig를 바꾼 뒤
Get-Content $env:USERPROFILE\.wslconfig

# kind / Kubernetes
kind get clusters
kubectl config current-context
kubectl get nodes -o wide
kubectl get pods -A
```

## 5. 문제 해결

| 증상 | 원인 | 해결 |
|---|---|---|
| `error during connect ... //./pipe/docker_engine: The system cannot find the file specified` | Docker Desktop이 꺼져 있다 | Docker Desktop을 켠다. 자주 겪으면 로그인할 때 자동 시작을 켠다 (2026-10-04에 실제로 겪음) |
| Testcontainers: `Could not find a valid Docker environment` | 위와 같다 | 위와 같다 |
| `client version 1.xx is too old. Minimum supported API version is 1.44` | Docker Engine 29.0~29.2가 최소 API 버전을 1.44로 올렸다(29.3에서 1.40으로 다시 낮춤) `[문헌]` | 오류를 낸 도구를 최신으로 올린다. 또는 Docker Desktop을 Engine 29.3 이상이 들어 있는 버전으로 올린다 |
| `kind load docker-image`가 `content digest ... not found`나 `ctr ... import` 오류로 실패 | containerd 이미지 저장소를 쓰면 `docker save`가 여러 플랫폼 목록을 그대로 내보내는데, 실제로는 한 플랫폼만 있어서 kind가 가져오지 못한다 `[현장]` | containerd 이미지 저장소를 끈다. 또는 빌드할 때 `--provenance=false`를 붙인다. 근본적으로는 로컬 레지스트리로 push/pull한다 |
| `.wslconfig`를 고쳤는데 메모리가 그대로다 | WSL VM이 다시 시작되지 않았다 | `wsl --shutdown` 후 Docker Desktop을 다시 켜고 `docker info`로 확인한다 |
| Pod가 `OOMKilled`로 재시작되거나 Oracle이 뜨다 죽는다 | Docker VM 메모리가 부족하거나 Pod 메모리 한도가 작다 | `docker stats`와 `kubectl describe pod`로 확인한다. `.wslconfig` 메모리를 늘리거나 Pod 한도를 조정한다 |
| Pod에서 `order-db`, `inventory-db`, `keycloak` 이름을 찾지 못한다 | compose 컨테이너가 `kind` 네트워크에 붙어 있지 않다 | `docker network inspect kind`로 확인한다. compose에 external network `kind`를 지정하거나 `docker network connect kind <컨테이너>` |
| PC나 Docker Desktop을 재시작한 뒤 kubectl이 클러스터에 연결되지 않는다 | kind 노드 컨테이너가 멈춰 있다 | `docker ps -a`로 확인하고 `docker start msa-control-plane msa-worker msa-worker2`. 안 되면 클러스터를 지우고 다시 만든다(데이터는 클러스터 밖에 있다) |
| Oracle이 `ORA-00442: Oracle Database Express Edition (XE) single instance violation`으로 시작하지 않는다 | 한 컨테이너(논리 환경) 안에 XE를 둘 이상 띄웠다 `[문헌]` | 서비스마다 컨테이너를 따로 띄운다 (ADR-0005) |
| Oracle 컨테이너가 처음 뜰 때 몇 분 걸린다 | 첫 기동에 DB를 만든다 | faststart 이미지를 쓴다. 로그에 `DATABASE IS READY TO USE!`가 나올 때까지 기다린다 |
| 회사 VPN을 켜면 컨테이너 통신이 끊긴다 | Docker 내부 서브넷(기본 192.168.65.0/24)이 VPN 주소와 겹친다 | Settings > Resources > Network에서 서브넷을 바꾼다 |
| 디스크가 부족하다 | 이미지, 빌드 캐시, 볼륨이 쌓였다 | `docker system df`로 확인한 뒤 `docker image prune`, `docker builder prune`. 볼륨은 내용을 확인하고 지운다 |
| 토큰 검증이 `iss` 불일치로 실패한다 | Keycloak 주소가 클러스터 안팎에서 다르다 | [design 7절](../design/architecture.md#7-인증-adr-0006) |

## 6. 라이선스

Docker Desktop은 직원 250명 이상이거나 연 매출 1천만 달러 이상인 회사에서 쓰려면 유료 구독이 필요하다. 개인 학습과 실습은 무료다. `[문헌]`

## 참고

- [CVE-2025-9074 — Docker Desktop 4.44.3에서 수정 (CSA 싱가포르 경보)](https://www.csa.gov.sg/alerts-and-advisories/alerts/al-2025-085/)
- [Docker Engine v29 변경 사항](https://www.docker.com/blog/docker-engine-version-29/)
- [Docker 29 최소 API 버전 문제 (Docker 포럼)](https://forums.docker.com/t/docker-29-increased-minimum-api-version-breaks-traefik-reverse-proxy/150384)
