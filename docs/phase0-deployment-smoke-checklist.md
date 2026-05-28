# Phase 0 Deployment Smoke Checklist

작성일: 2026-05-28  
대상: `flip-sync-server` blue/green 배포

## 1. 목적

이 체크리스트는 GitHub Actions 배포 후 `/mob` 백엔드가 정상적으로 살아 있는지 빠르게 확인하기 위한 최소 검증 절차다. 배포 성공 여부는 컨테이너 실행만으로 판단하지 않고, Nginx 라우팅과 외부 URL까지 확인한다.

## 2. 배포 전 확인

| 항목 | 확인 방법 | 기대 결과 |
| --- | --- | --- |
| workflow 파일 문법 | GitHub Actions 화면에서 workflow 로드 가능 여부 확인 | workflow가 인식됨 |
| deploy script 문법 | `bash -n deploy.sh` | 오류 없음 |
| Docker tag | Actions env `IMAGE_TAG=${{ github.run_number }}` | run number tag 생성 |
| health path | Actions env `HEALTH_CHECK_PATH=ready` | target health check가 `/ready`를 봄 |
| 줄바꿈 | `git ls-files --eol deploy.sh .github/workflows/flip-sync-be.yml` | `w/lf` |

## 3. GitHub Actions 로그 확인

배포 job에서 아래 로그가 순서대로 보여야 한다.

```text
[runtime] before ssh-keyscan
[runtime] before mkdir/chmod
[runtime] before chown
[runtime] before scp deploy.sh
[runtime] before diag connected
[runtime] before diag project dir
[runtime] before diag deploy file
[runtime] before diag sudo
[runtime] before diag bash-n
[runtime] before deploy exec
```

`deploy.sh` 실행 후 아래 섹션이 보여야 한다.

```text
deploy args
preflight
port check
resolve live image
create docker compose
current health state
docker login
docker compose up
run tests
nginx check
down live after delay
done
```

## 4. 서버 내부 확인

서버에서 직접 확인할 때는 아래 명령을 사용한다.

```bash
sudo docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}\t{{.Image}}'
```

기대 결과:

- `flip-sync-server-blue` 또는 `flip-sync-server-green` 중 하나가 live 상태다.
- target 컨테이너가 새 `IMAGE_TAG` 이미지를 사용한다.
- live port는 `48122` 또는 `38122` 중 하나다.

내부 health check:

```bash
curl -i http://127.0.0.1:48122/ready
curl -i http://127.0.0.1:38122/ready
```

기대 결과:

- active target은 `200`을 반환한다.
- DB 연결 실패 시 `/ready`는 `503`을 반환해야 한다.

## 5. 외부 URL 확인

`deploy.sh` 내부의 외부 URL 확인은 서버가 자기 public domain을 다시 호출할 수 없는 환경을 고려해 기본 경고로 처리한다. 실제 외부 URL 최종 판정은 배포가 끝난 뒤 runner 또는 운영자 환경에서 확인한다.

배포 후 외부에서 확인할 URL:

```text
https://fliplyze.com/mob/ready
https://fliplyze.com/mob/health
https://fliplyze.com/mob/api-docs
```

기대 결과:

| URL | 기대 상태 |
| --- | --- |
| `/mob/ready` | 200 |
| `/mob/health` | 200 |
| `/mob/api-docs` | 200 |

## 6. Nginx 확인

Nginx 설정 검사:

```bash
sudo nginx -t
```

현재 `/mob` proxy port 확인:

```bash
sudo grep -n 'proxy_pass' /etc/nginx/sites-available/*
```

기대 결과:

- `/mob` location이 `127.0.0.1:48122` 또는 `127.0.0.1:38122`를 바라본다.
- `nginx -t`가 실패하면 `deploy.sh`가 백업 파일로 rollback해야 한다.

## 7. 실패 시 확인 순서

1. Actions 로그에서 `FAILED_AT_LINE`과 `COMMAND`를 확인한다.
2. `preflight`에서 실패했다면 서버의 필수 명령어, sudo 권한, Docker 상태를 확인한다.
3. `run tests`에서 실패했다면 target 컨테이너 로그를 확인한다.
4. `nginx check`에서 실패했다면 Nginx 백업 파일이 복구됐는지 확인한다.
5. 외부 health check가 실패했다면 Nginx가 live port로 rollback됐는지 확인한다.

target 컨테이너 로그:

```bash
sudo docker logs --tail 200 flip-sync-server-blue
sudo docker logs --tail 200 flip-sync-server-green
```

## 8. Phase 0 완료 기준

아래 항목이 모두 충족되면 Phase 0 배포 안정화는 통과로 본다.

| 기준 | 완료 여부 |
| --- | --- |
| `deploy.sh` 문법 검증 통과 |  |
| Kotlin compile 통과 |  |
| GitHub Actions에서 run number image tag 배포 |  |
| target `/ready` 200 확인 후 Nginx 전환 |  |
| 외부 `/mob/ready` 200 확인 |  |
| 실패 시 기존 live port 유지 |  |
| Nginx 설정 실패 시 백업 rollback |  |
