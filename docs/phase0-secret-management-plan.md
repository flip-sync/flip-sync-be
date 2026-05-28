# Phase 0 Secret Management Plan

작성일: 2026-05-28  
대상: `flip-sync-server`

## 1. 목적

현재 운영 설정은 `application.yml`과 private 설정 저장소를 통해 이미지 빌드 시점에 리소스로 복사되는 구조다. 이 방식은 배포는 단순하지만, Docker image와 저장소에 운영 credential이 포함될 수 있어 키 유출 시 영향 범위가 크다.

이 문서는 즉시 설정을 제거하는 문서가 아니다. 운영 배포를 깨지 않기 위해 시크릿 전환 범위와 순서를 먼저 정리한다.

## 2. 시크릿 후보

아래 항목은 환경변수 또는 서버 `.env`/secret manager로 전환해야 할 후보이다.

| 설정 | 현재 위치 후보 | 전환 권장 |
| --- | --- | --- |
| DB URL | `application-prod.yml` | `SPRING_DATASOURCE_URL` |
| DB username | `application-prod.yml` | `SPRING_DATASOURCE_USERNAME` |
| DB password | `application-prod.yml` | `SPRING_DATASOURCE_PASSWORD` |
| Gmail password | `application.yml` | `SPRING_MAIL_PASSWORD` |
| JWT secret | `application.yml` | `JWT_SECRET` |
| AWS access key | `application.yml` | `CLOUD_AWS_CREDENTIALS_ACCESS_KEY` |
| AWS secret key | `application.yml` | `CLOUD_AWS_CREDENTIALS_SECRET_KEY` |
| Docker Hub token | GitHub Actions secret | 유지 |
| 서버 SSH key | GitHub Actions secret | 유지 |

## 3. 전환 원칙

1. 운영 설정 제거와 키 회전은 같은 배포에서 동시에 하지 않는다.
2. 먼저 Spring Boot가 환경변수 override를 받을 수 있는지 확인한다.
3. 서버에 `.env`를 배치하고 `docker-compose.yml`에서 `env_file`을 사용한다.
4. 배포가 안정화된 뒤 `application*.yml`의 평문 값을 placeholder로 바꾼다.
5. 마지막으로 노출된 키를 재발급한다.

## 4. 권장 진행 순서

| 단계 | 작업 | 검증 |
| --- | --- | --- |
| 1 | 서버 `/home/{user}/docker/flip-sync-server/.env` 생성 | 파일 권한 `600` |
| 2 | `deploy.sh` compose 생성부에 `env_file` 추가 | 컨테이너 env 확인 |
| 3 | Spring 환경변수 override로 DB/mail/JWT/AWS 적용 | `/ready` 200 |
| 4 | GitHub Actions에서 `.env`를 scp하지 않고 서버 파일 유지 | 재배포 후 값 유지 |
| 5 | `application*.yml` 평문 값 placeholder화 | 빌드/배포 성공 |
| 6 | Gmail app password, JWT secret, AWS key 회전 | 기존 키 비활성화 |

## 5. 주의 사항

- 사용자가 이전에 `application.yml` 데이터 기반 운영을 선택했으므로, 이 전환은 별도 승인 후 진행한다.
- AWS key와 Gmail app password는 이미 노출된 이력이 있으므로 최종적으로 회전이 필요하다.
- DB credential 전환 전에는 반드시 서버 `.env` 백업과 rollback 절차가 있어야 한다.
- `deploy.sh`가 매번 compose를 생성하므로 env_file 추가는 스크립트에 반영되어야 한다.

## 6. Acceptance Checks

| 항목 | 기대 결과 |
| --- | --- |
| `.env` 권한 | root 또는 배포 사용자만 읽기 가능 |
| `/ready` | 환경변수 적용 후 200 |
| 이메일 인증 발송 | 환경변수 mail 설정으로 정상 발송 |
| 이미지 업로드 | 환경변수 AWS 설정으로 정상 업로드 |
| Docker image 검사 | image 내부에 운영 시크릿이 포함되지 않음 |
| 키 회전 | 이전 키로는 접근 불가 |

