# ADR-0003 로컬 실행 환경은 kind 위의 쿠버네티스 기본 기능으로 만든다

- 상태: 채택
- 날짜: 2026-10-04
- 관련: [design/architecture.md](../design/architecture.md), STD-003, NFR-001, NFR-003, NFR-006
- 출처: P = [마이크로서비스와 쿠버네티스 기초](../references/msa-k8s-primer.md)

## 배경

이 프로젝트는 현대 MSA를 실험하는 것이 목적이다. Pod 장애, 롤링 업데이트, 프로브, 확장 같은 운영 상황(P§7)을 로컬에서 직접 겪어 봐야 한다. 서비스를 어디서 실행하고, 서비스끼리 어떻게 찾고, 밖에서 들어온 요청을 어떻게 받을지 정해야 한다.

## 결정

- **클러스터**: kind(Kubernetes IN Docker)로 로컬 쿠버네티스 클러스터를 만든다. (P§4-1)
- **서비스 디스커버리**: 쿠버네티스 Service를 쓴다. Eureka 같은 별도 서버를 두지 않는다. (P§4-3)
- **클러스터 입구**: Gateway API로 만든다. (P§5-1)
- **설정**: ConfigMap과 Secret을 쓴다. 별도 설정 서버(Spring Cloud Config 등)를 두지 않는다. (P§6-⑥)

## 고려한 대안

| 대안 | 고르지 않은 이유 |
|---|---|
| docker-compose만 사용 | 롤링 업데이트, 프로브에 따른 재시작, replicas 유지 같은 쿠버네티스 동작을 실험할 수 없다. 단, DB 같은 보조 도구를 띄우는 데는 쓸 수 있다 |
| minikube, k3d | 쓸 수 있다. 원문이 kind를 기준으로 쓰였고, 노드를 Docker 컨테이너로 만들어 여러 노드를 쉽게 흉내 낼 수 있어 kind를 고른다 |
| Spring Cloud Netflix Eureka + Spring Cloud Gateway | 쿠버네티스가 이미 하는 디스커버리와 라우팅을 앱 안에서 한 번 더 하게 된다 |
| Ingress | 쿠버네티스 문서는 Ingress API를 더 바꾸지 않고(frozen) Gateway API를 권한다 `[문헌]` |

## 결과

- 좋아지는 점: 운영 상황 테스트(TC-101~109)를 로컬에서 재현할 수 있다. 앱 코드에 디스커버리 라이브러리가 들어가지 않는다.
- 감수할 점: 로컬 PC 자원을 많이 쓴다. kind 노드, Spring Boot Pod 4개, Oracle XE 2개, Keycloak이 함께 뜬다.

## 아직 정할 것

- ~~Gateway API 구현체~~ → Envoy Gateway ([ADR-0007](0007-envoy-gateway.md))
- ~~kind 노드 구성~~ → control-plane 1 + worker 2 (`docs/design/architecture.md` 11절, 2026-10-04)
- ~~DB를 클러스터 안에 둘지 밖에 둘지~~ → 클러스터 밖 docker compose, 서비스마다 컨테이너 하나 ([ADR-0005](0005-oracle-xe-instance-per-service.md))

## 바꿀 때

클라우드의 관리형 쿠버네티스로 옮기더라도 Service, Deployment, Gateway API는 그대로 쓸 수 있다. 바뀌는 것은 클러스터 생성과 Gateway 구현체다.

## 용어

- **kind (Kubernetes IN Docker)**: Docker 컨테이너를 노드로 써서 쿠버네티스 클러스터를 만드는 도구다. 이 프로젝트의 로컬 클러스터는 kind로 만든다.
- **노드 (node)**: 쿠버네티스에서 Pod가 실제로 실행되는 컴퓨터 한 대다. kind에서는 Docker 컨테이너 하나가 노드 하나다.
- **Pod**: 쿠버네티스가 컨테이너를 실행하는 가장 작은 단위다. 언제든 지워지고 새로 만들어지므로, 남아야 하는 데이터를 Pod 안에 두지 않는다.
- **쿠버네티스 Service (Kubernetes Service)**: 여러 Pod 앞에 고정된 이름과 주소를 붙여 주는 쿠버네티스 객체다. 주문 서비스는 `http://inventory:8080`으로 재고 서비스를 부른다.
- **Deployment**: Pod를 정해진 개수만큼 유지하고 새 버전으로 바꿔 주는 쿠버네티스 객체다. 서비스마다 하나 두고 Pod 수를 2개로 둔다.
- **롤링 업데이트 (rolling update)**: Pod를 하나씩 새 버전으로 바꿔, 서비스를 멈추지 않고 배포하는 방식이다. 바꾸는 동안 옛 버전과 새 버전이 함께 요청을 처리한다.
- **replicas**: Deployment가 유지하는 Pod 수다. 서비스마다 2 이상으로 둬서 Pod 하나가 사라져도 서비스가 멈추지 않게 한다.
- **서비스 디스커버리 (service discovery)**: 호출할 서비스의 주소를 찾아내는 방법이다. 이 프로젝트는 별도 서버 없이 쿠버네티스 Service 이름으로 찾는다.
- **Eureka**: Spring Cloud가 제공하는 서비스 디스커버리 서버다. 쿠버네티스 Service로 충분해서 이 프로젝트에서는 쓰지 않는다.
- **Gateway API**: 쿠버네티스 클러스터 입구의 라우팅을 정하는 표준 규격이다. Ingress를 잇는 규격이고, Envoy Gateway가 이 규격을 구현한다.
- **Ingress**: Gateway API보다 먼저 나온 쿠버네티스의 입구 규격이다. 쿠버네티스 문서가 더는 바꾸지 않는다고 해서 Gateway API를 쓴다.
- **ConfigMap, Secret**: 쿠버네티스가 환경마다 다른 값을 Pod에 넣어 주는 객체다. 일반 설정값은 ConfigMap에, 비밀번호 같은 값은 Secret에 둔다.
- **Spring Cloud Config**: 여러 서비스의 설정을 한 서버에서 나눠 주는 Spring Cloud 서버다. ConfigMap과 Secret으로 충분해서 쓰지 않는다.
- **minikube, k3d**: kind처럼 로컬에서 쿠버네티스 클러스터를 만드는 도구다. 원문이 kind를 기준으로 쓰여 kind를 골랐다.
