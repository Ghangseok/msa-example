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
- **설정**: ConfigMap과 Secret을 쓴다. 별도 설정 서버(Spring Cloud Config 등)를 두지 않는다. (P§6-⑥) `[제안]`

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
- kind 노드 구성: 원문 예처럼 작업 노드 2개로 할지 (P§4-1)
- ~~DB를 클러스터 안에 둘지 밖에 둘지~~ → 클러스터 밖 docker compose, 서비스마다 컨테이너 하나 ([ADR-0005](0005-oracle-xe-instance-per-service.md))

## 바꿀 때

클라우드의 관리형 쿠버네티스로 옮기더라도 Service, Deployment, Gateway API는 그대로 쓸 수 있다. 바뀌는 것은 클러스터 생성과 Gateway 구현체다.
