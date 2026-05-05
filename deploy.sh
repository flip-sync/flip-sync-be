#!/bin/bash

set -euo pipefail
echo 'status=$?; echo "FAILED_AT_LINE=$LINENO EXIT_CODE=$status COMMAND=$BASH_COMMAND"; exit $status' ERR
trap
REPO_NAME=${1:-}
DIR_PROJECT=${2:-}
BLUE_PORT=${3:-}
GREEN_PORT=${4:-}
INNER_PORT=${5:-}
DOWN_DELTA_TIME=${6:-}
DOCKER_HUB_USERNAME=${7:-}
TOKEN=${8:-}
NGINX_DOMAIN=${9:-}
HEALTH_CHECK_PATH=${10:-api-docs}

NAMESPACE=$DOCKER_HUB_USERNAME
IMAGE_REPO="${NAMESPACE}/${REPO_NAME}"
COMPOSE_FILE="${DIR_PROJECT}/docker-compose.yml"
NGINX_FILE="/etc/nginx/sites-available/${NGINX_DOMAIN}"

log_section() {
  echo "===================="
  echo "$1"
}

require_value() {
  local name=$1
  local value=$2
  if [ -z "$value" ]; then
    echo "Missing required argument: ${name}"
    exit 1
  fi
}

http_code() {
  local port=$1
  local status
  status=$(curl --write-out '%{http_code}' --silent --output /dev/null "http://127.0.0.1:${port}/${HEALTH_CHECK_PATH}" || true)
  if [ -z "$status" ]; then
    status="000"
  fi
  echo "$status"
}

extract_proxy_port() {
  local file=$1
  sed -nE 's/.*proxy_pass[[:space:]]+http:\/\/127\.0\.0\.1:([0-9]+)\/?;.*/\1/p' "$file" | head -n1
}

replace_proxy_port() {
  local file=$1
  local current_port=$2
  local target_port=$3
  sed -i -E "0,/proxy_pass[[:space:]]+http:\/\/127\.0\.0\.1:${current_port}(\/)?;/s//proxy_pass http:\/\/127.0.0.1:${target_port}\1;/" "$file"
}

find_live_image() {
  docker inspect -f '{{.Config.Image}}' "${REPO_NAME}-${LIVE_COLOR}" 2>/dev/null || true
}

find_previous_tag() {
  local tags_url="https://hub.docker.com/v2/repositories/${NAMESPACE}/${REPO_NAME}/tags?page_size=100"
  curl -fsSL "$tags_url" 2>/dev/null \
    | jq -r '.results[]?.name' 2>/dev/null \
    | grep -E '^[0-9]+$' \
    | sort -n \
    | tail -2 \
    | head -1 || true
}

log_target_diagnostics() {
  echo "===================="
  echo "target diagnostics"
  docker ps -a --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}\t{{.Image}}' || true
  docker inspect -f 'target_state={{.State.Status}} exit={{.State.ExitCode}} restart={{.RestartCount}}' "${REPO_NAME}-${TARGET_COLOR}" 2>/dev/null || true
  docker logs --tail 200 "${REPO_NAME}-${TARGET_COLOR}" 2>&1 || true
  echo "===================="
}

HEALTH_CHECK_PATH=${HEALTH_CHECK_PATH#/}

log_section "deploy args"
echo "\$1 REPO_NAME=$REPO_NAME"
echo "\$2 DIR_PROJECT=$DIR_PROJECT"
echo "\$3 BLUE_PORT=$BLUE_PORT"
echo "\$4 GREEN_PORT=$GREEN_PORT"
echo "\$5 INNER_PORT=$INNER_PORT"
echo "\$6 DOWN_DELTA_TIME=$DOWN_DELTA_TIME"
echo "\$7 DOCKER_HUB_USERNAME=$DOCKER_HUB_USERNAME"
echo "\$8 TOKEN_LENGTH=${#TOKEN}"
echo "\$9 NGINX_DOMAIN=$NGINX_DOMAIN"
echo "\$10 HEALTH_CHECK_PATH=$HEALTH_CHECK_PATH"

require_value "REPO_NAME" "$REPO_NAME"
require_value "DIR_PROJECT" "$DIR_PROJECT"
require_value "BLUE_PORT" "$BLUE_PORT"
require_value "GREEN_PORT" "$GREEN_PORT"
require_value "INNER_PORT" "$INNER_PORT"
require_value "DOWN_DELTA_TIME" "$DOWN_DELTA_TIME"
require_value "DOCKER_HUB_USERNAME" "$DOCKER_HUB_USERNAME"
require_value "TOKEN" "$TOKEN"
require_value "NGINX_DOMAIN" "$NGINX_DOMAIN"

mkdir -p "${DIR_PROJECT}/logs" "${DIR_PROJECT}/sh"

log_section "stop previous background job"
echo "실행중인 백그라운드 종료"
if [ -f "$DIR_PROJECT/sh/down_live_after_delay.pid" ]; then
    OLD_PID=$(cat "$DIR_PROJECT/sh/down_live_after_delay.pid")
    if ps -p "$OLD_PID" > /dev/null 2>&1; then
        echo "Terminating existing background process: $OLD_PID"
        kill -9 "$OLD_PID"
    else
        echo "No running background process found for PID $OLD_PID"
    fi
else
    echo "No previous background process file found"
fi
echo "===================="


#echo "===================="
#echo "변수 확인"
#echo "REPO_NAME: $REPO_NAME"
#echo "DIR_PROJECT: $DIR_PROJECT"
#echo "BLUE_PORT: $BLUE_PORT"
#echo "GREEN_PORT: $GREEN_PORT"
#echo "INNER_PORT: $INNER_PORT"
#echo "DOWN_DELTA_TIME: $DOWN_DELTA_TIME"
#echo "DOCKER_HUB_USERNAME: $DOCKER_HUB_USERNAME"
#echo "TOKEN: $TOKEN"
#echo "HEALTH_CHECK_PATH: $HEALTH_CHECK_PATH"
#echo "NGINX_DOMAIN: $NGINX_DOMAIN"
#
#
#if [ -z "$REPO_NAME" ] || [ -z "$BLUE_PORT" ] || [ -z "$GREEN_PORT" ] || [ -z "$INNER_PORT" ] || [ -z "$DIR_PROJECT" ] || [ -z "$DOWN_DELTA_TIME" ] || [ -z "$TOKEN" ] || [ -z "$NGINX_DOMAIN" ]; then
#    echo "One or more variables are empty."
#    exit 1
#fi
#echo "===================="


echo "===================="
log_section "port check"
# sites-available/$NGINX_DOMAIN 파일을 생성해 놔야함
# 포트 번호 추출
if [ ! -f "$NGINX_FILE" ]; then
    echo "Nginx file not found: $NGINX_FILE"
    exit 1
fi
echo "proxy_pass lines in $NGINX_FILE"
grep -n 'proxy_pass' "$NGINX_FILE" || true
NOW_PORT=$(extract_proxy_port "$NGINX_FILE" || true)

# 결과 출력
if [[ -n "$NOW_PORT" ]]; then
    echo "추출된 포트 번호: $NOW_PORT"
else
    echo "포트 번호를 추출하지 못했습니다."
    exit 1
fi

if [ "$NOW_PORT" == "$GREEN_PORT" ]
then
  LIVE_PORT=$GREEN_PORT
  LIVE_COLOR="green"
  TARGET_PORT=$BLUE_PORT
  TARGET_COLOR="blue"
else
  LIVE_PORT=$BLUE_PORT
  LIVE_COLOR="blue"
  TARGET_PORT=$GREEN_PORT
  TARGET_COLOR="green"
fi

cd "$DIR_PROJECT" || exit 1
echo "LIVE_PORT: $LIVE_PORT"
echo "LIVE_COLOR: $LIVE_COLOR"
echo "TARGET_PORT: $TARGET_PORT"
echo "TARGET_COLOR: $TARGET_COLOR"
echo "===================="


echo "===================="
log_section "resolve live image"
LIVE_IMAGE=$(find_live_image)
PREVIOUS_TAG=""
if [ -z "$LIVE_IMAGE" ]; then
  PREVIOUS_TAG=$(find_previous_tag)
  echo "PREVIOUS_TAG_FROM_HUB=$PREVIOUS_TAG"
  if [ -n "$PREVIOUS_TAG" ]; then
    LIVE_IMAGE="${IMAGE_REPO}:${PREVIOUS_TAG}"
  fi
fi
if [ -z "$LIVE_IMAGE" ]; then
  LIVE_IMAGE="${IMAGE_REPO}:latest"
fi
echo "LIVE_IMAGE=$LIVE_IMAGE"

log_section "create docker compose"

: # live image is resolved from the running container or Docker Hub tag list

cat << EOF > "$COMPOSE_FILE"
services:
  ${TARGET_COLOR}:
    image: ${IMAGE_REPO}:latest
    container_name: ${REPO_NAME}-${TARGET_COLOR}
    ports:
      - "${TARGET_PORT}:${INNER_PORT}"
    volumes:
      - ${DIR_PROJECT}/logs:/logs
    restart: always
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - TZ=Asia/Seoul
  ${LIVE_COLOR}:
    image: ${LIVE_IMAGE}
    container_name: ${REPO_NAME}-${LIVE_COLOR}
    ports:
      - "${LIVE_PORT}:${INNER_PORT}"
    volumes:
      - ${DIR_PROJECT}/logs:/logs
    restart: always
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - TZ=Asia/Seoul
EOF
nl -ba "$COMPOSE_FILE"


log_section "current health state"
Target=$(http_code "$TARGET_PORT")
Live=$(http_code "$LIVE_PORT")

echo "API Target: $Target"
echo "API Live: $Live"
echo "===================="


log_section "docker login"
echo "$TOKEN" | docker login -u "$DOCKER_HUB_USERNAME" --password-stdin

# Deploy the new version to the new environment
log_section "docker compose up"

which docker-compose || exit 1

docker-compose -f "$COMPOSE_FILE" pull "$TARGET_COLOR"
if docker ps -a --format '{{.Names}}' | grep -q "^${REPO_NAME}-${TARGET_COLOR}$"; then
  docker rm -f "${REPO_NAME}-${TARGET_COLOR}" || true
fi
docker-compose -f "$COMPOSE_FILE" up -d "$TARGET_COLOR"

Target=$(http_code "$TARGET_PORT")
Live=$(http_code "$LIVE_PORT")

echo "API Target: $Target"
echo "API Live: $Live"
echo "===================="

run_tests() {
  local target_status
  local live_status
  local i

  for i in {1..10}; do
    target_status=$(http_code "$TARGET_PORT")
    live_status=$(http_code "$LIVE_PORT")

    echo "API Target: $target_status"
    echo "API Live: $live_status"

    if [ "$target_status" -eq 200 ]; then
      return 0
    fi

    echo "API is not ready yet cnt: [$i/10]"
    sleep 6
  done

  echo "API has not become ready within 60 seconds"
  return 1
}


log_section "run tests"
if run_tests; then
  echo "Success. Change target: $TARGET_COLOR"
  NGINX_TARGET_PORT=$TARGET_PORT
  IS_SUCCESS=0

else
  echo "Fail. Keep live: $LIVE_COLOR"
  NGINX_TARGET_PORT=$LIVE_PORT
  IS_SUCCESS=1
  log_target_diagnostics
fi
echo "===================="



echo "===================="
log_section "nginx check"
current_port=$(extract_proxy_port "$NGINX_FILE" || true)

if [ "$current_port" = "$NGINX_TARGET_PORT" ]; then
echo "Nginx already points to port $current_port"
else
replace_proxy_port "$NGINX_FILE" "$current_port" "$NGINX_TARGET_PORT"
# Nginx 설정 검사
output=$(sudo nginx -t 2>&1)

# "success" 문구가 있는지 확인
output=$(sudo nginx -t 2>&1)
if [[ $output == *"test is successful"* ]]; then
  sudo nginx -s reload
  # 설정이 올바르면 Nginx를 리로드
  sudo nginx -s reload
else
  # 설정에 문제가 있으면 에러 메시지 출력
  echo "Nginx configuration test failed"
  echo "$output"
  exit 1
fi

fi
echo "===================="

log_section "down live after delay"

if [ "$IS_SUCCESS" -eq 0 ]; then
  if docker ps --format '{{.Names}}' | grep -q "^${REPO_NAME}-${LIVE_COLOR}$"; then

    echo "Creating directory: $DIR_PROJECT/sh"
    mkdir -p "$DIR_PROJECT/sh"
    if [ $? -ne 0 ]; then
      echo "디렉토리 생성 실패: $DIR_PROJECT/sh"
      exit 1
    fi

    echo "Creating down_live_after_delay.sh"
    cat << EOF > "$DIR_PROJECT/sh/down_live_after_delay.sh"
#!/bin/bash

sleep $DOWN_DELTA_TIME
echo "Down Live after delay: $LIVE_COLOR"
docker stop $REPO_NAME-$LIVE_COLOR
EOF

    if [ $? -ne 0 ]; then
      echo "스크립트 파일 생성 실패"
      exit 1
    fi

    echo "Setting execute permission for down_live_after_delay.sh"
    chmod +x "$DIR_PROJECT/sh/down_live_after_delay.sh"
    if [ $? -ne 0 ]; then
      echo "chmod 실패: $DIR_PROJECT/sh/down_live_after_delay.sh"
      exit 1
    fi

    echo "Running down_live_after_delay.sh with nohup"
    nohup "$DIR_PROJECT/sh/down_live_after_delay.sh" > /dev/null 2>&1 &
    if [ $? -ne 0 ]; then
      echo "nohup 실행 실패"
      exit 1
    fi

    echo "Saving PID to down_live_after_delay.pid"
    echo $! > "$DIR_PROJECT/sh/down_live_after_delay.pid"
    if [ $? -ne 0 ]; then
      echo "PID 파일 저장 실패: $DIR_PROJECT/sh/down_live_after_delay.pid"
      exit 1
    fi

    echo "Down Live after delay: $LIVE_COLOR, PID: $!, $DOWN_DELTA_TIME"
  else
    echo "Live container not running: $REPO_NAME-$LIVE_COLOR"
  fi
else
  if docker ps --format '{{.Names}}' | grep -q "^${REPO_NAME}-${TARGET_COLOR}$"; then
    echo "Down Target: $TARGET_COLOR"
    docker stop "$REPO_NAME-$TARGET_COLOR"
  else
    echo "Target container not running: $REPO_NAME-$TARGET_COLOR"
  fi
  exit 1
fi
log_section "done"
