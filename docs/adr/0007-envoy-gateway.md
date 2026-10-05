# ADR-0007 Gateway API 구현체로 Envoy Gateway를 쓴다

- 상태: 채택
- 날짜: 2026-10-04
- 관련: [ADR-0003](0003-local-k8s-runtime.md), [ADR-0006](0006-keycloak-jwt-auth.md), [design/architecture.md](../design/architecture.md), [references/docker-desktop.md](../references/docker-desktop.md)

## 배경

ADR-0003에서 클러스터 입구를 Gateway API로 만들기로 했다. Gateway API는 규격이라 실제로 요청을 받는 구현체(컨트롤러와 프록시)를 골라 설치해야 한다.

조건:

- 주문 처리는 최대 30초 걸린다. 입구의 요청 제한 시간을 그보다 길게 둘 수 있어야 한다. (OQ-004)
- 나중에 Gateway에서도 JWT를 검증하거나, 재시도·서킷 브레이커·속도 제한을 입구에서 실험할 수 있으면 좋다.
- kind 위에서 가볍게 돌아야 한다.

## 결정

- **Envoy Gateway**를 Helm으로 설치한다. 버전은 v1.9.2다(2026-10-05 결정, [tech-stack.md](../standards/tech-stack.md) "로컬 실행 인프라 버전").
- GatewayClass와 Gateway를 하나씩 두고, HTTPRoute로 `/orders`, `/admin/orders`를 `order` Service로 보낸다. 재고 서비스에는 경로를 두지 않는다. (OQ-001)
- **HTTPRoute의 `timeouts.request`를 40초로 둔다.** Envoy의 기본 요청 제한 시간은 15초라서 그대로 두면 30초 걸리는 주문을 중간에 끊는다. `[문헌]`
- **kind에서 밖으로 여는 방법**: Envoy 프록시의 Service를 NodePort로 바꾸고(EnvoyProxy 리소스), kind의 extraPortMappings로 호스트 80번 포트에 연결한다. Windows의 Docker Desktop에서는 LoadBalancer IP에 호스트가 바로 닿지 않을 수 있어서다. 다른 방법으로는 kind가 안내하는 cloud-provider-kind가 있다. (2026-10-05 결정)
- **표준 리소스를 먼저 쓴다.** Gateway, HTTPRoute 같은 Gateway API 표준 리소스로 할 수 있는 일은 표준으로 한다. Envoy Gateway만의 리소스(EnvoyProxy, SecurityPolicy, BackendTrafficPolicy 등)는 표준으로 안 될 때만 쓰고, 쓴 곳을 `infra/` 안에 모아 둔다. 다른 구현체로 옮길 때 고칠 곳을 줄이기 위해서다.

## 고려한 대안

| 대안 | 고르지 않은 이유 |
|---|---|
| NGINX Gateway Fabric | NGINX에 익숙하면 쉽다. 하지만 JWT 검증은 유료인 NGINX Plus에서만 된다 `[문헌]` |
| Istio (게이트웨이로만 사용) | 나중에 서비스 메시(서비스 간 mTLS, 트래픽 정책)로 넓힐 수 있다. 하지만 무겁고 배울 개념이 많다. 메시가 필요해지면 그때 따로 검토한다 |
| Cilium | kind의 네트워크 플러그인(CNI)을 Cilium으로 바꿔야 한다 |

## 결과

- 좋아지는 점:
  - Gateway API 전용으로 만들어진 구현체이고, 표준 적합성 시험(v1.3)을 모두 통과했다. `[문헌]`
  - JWT 검증(SecurityPolicy), 재시도·서킷 브레이커·속도 제한(BackendTrafficPolicy)을 무료로 쓸 수 있다. 입구에서의 장애 대응 실험에 쓴다.
- 감수할 점:
  - 컨트롤러와 Envoy 프록시가 메모리를 약 0.3~0.5GB 쓴다.
  - Envoy Gateway만의 리소스를 쓰면 다른 구현체로 옮길 때 다시 써야 한다.

## 바꿀 때

서비스 메시가 필요해지면(서비스 간 mTLS, 서비스 간 트래픽 정책) Istio를 다시 검토한다. 이때 입구는 Envoy Gateway를 그대로 두고 메시만 더할 수도 있다.

## 용어

- **Gateway API**: 쿠버네티스 클러스터 입구의 라우팅을 정하는 표준 규격이다. Ingress를 잇는 규격이고, Envoy Gateway가 이 규격을 구현한다.
- **구현체 (implementation)**: 규격을 실제로 동작하게 만든 제품이다. Gateway API는 규격이고, Envoy Gateway가 그 구현체다.
- **Envoy Gateway**: Envoy 프록시를 써서 Gateway API를 구현한 오픈소스 프로젝트다. 이 프로젝트의 클러스터 입구다.
- **Envoy 프록시 (Envoy proxy)**: 요청을 받아 다른 서버로 넘기는 고성능 프록시 프로그램이다. Envoy Gateway가 이 프록시를 띄워 실제 요청을 처리한다.
- **Helm**: 쿠버네티스 설정 파일 여러 개를 묶어 설치하고 버전을 관리하는 패키지 도구다. 이 프로젝트에서는 Envoy Gateway를 Helm으로 설치한다.
- **GatewayClass, Gateway**: Gateway API의 객체다. GatewayClass는 어느 구현체를 쓸지 정하고, Gateway는 요청을 받을 입구 하나를 정한다.
- **HTTPRoute**: 경로별로 요청을 어느 Service로 보낼지 정하는 Gateway API 객체다. `/orders`와 `/admin/orders`를 주문 Service로 보내고, 요청 제한 시간을 40초로 둔다.
- **NodePort**: 쿠버네티스 Service를 모든 노드의 같은 포트 번호로 여는 방식이다. 이 프로젝트에서는 Envoy 프록시를 30080으로 열고 호스트 80번에 연결한다.
- **extraPortMappings**: kind 노드 컨테이너의 포트를 호스트 포트에 연결하는 kind 설정이다. 클러스터를 만들 때만 정할 수 있어서, 바꾸려면 클러스터를 다시 만든다.
- **EnvoyProxy (리소스)**: Envoy 프록시를 어떻게 띄울지 정하는 Envoy Gateway만의 설정 객체다. 프록시 Service를 NodePort로 바꿀 때 쓴다.
- **LoadBalancer (Service 유형)**: 외부에서 닿는 IP를 붙여 주는 쿠버네티스 Service 유형이다. Windows의 Docker Desktop에서는 이 IP에 호스트가 바로 닿지 않을 수 있어서 NodePort를 쓴다.
- **cloud-provider-kind**: kind 클러스터에서 LoadBalancer 유형 Service에 IP를 붙여 주는 도구다. NodePort 대신 쓸 수 있는 다른 방법으로 적어 두었다.
- **SecurityPolicy, BackendTrafficPolicy**: Envoy Gateway만의 설정 객체다. SecurityPolicy는 입구에서 JWT 검증 같은 보안 규칙을, BackendTrafficPolicy는 재시도·서킷 브레이커·속도 제한을 정한다.
- **속도 제한 (rate limiting)**: 정해진 시간에 받을 수 있는 요청 수를 넘으면 거절하는 장치다. 나중에 Gateway에서 실험할 수 있다.
- **표준 적합성 시험 (conformance test)**: 구현체가 규격대로 동작하는지 확인하는 공식 시험이다. Envoy Gateway는 Gateway API의 이 시험을 모두 통과했다.
- **서비스 메시 (service mesh)**: 서비스 사이 통신에 암호화, 재시도, 트래픽 정책을 앱 밖에서 더하는 인프라 층이다(예: Istio). 필요해지면 그때 따로 검토한다.
- **CNI (Container Network Interface)**: 쿠버네티스 Pod의 네트워크를 만드는 플러그인 규격이다. Cilium을 쓰려면 kind의 CNI를 바꿔야 해서 고르지 않았다.
- **mTLS (mutual TLS)**: 양쪽이 서로 인증서를 내보여 상대를 확인하는 암호화 연결이다. 서비스 간 인증 방법 후보 가운데 하나다.
