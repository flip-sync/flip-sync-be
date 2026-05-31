#!/bin/bash

set -euo pipefail
trap 'status=$?; echo "FAILED_AT_LINE=$LINENO EXIT_CODE=$status COMMAND=$BASH_COMMAND"; exit $status' ERR
REPO_NAME=${1:-}
DIR_PROJECT=${2:-}
BLUE_PORT=${3:-}
GREEN_PORT=${4:-}
INNER_PORT=${5:-}
DOWN_DELTA_TIME=${6:-}
DOCKER_HUB_USERNAME=${7:-}
TOKEN=${8:-}
NGINX_DOMAIN=${9:-}
HEALTH_CHECK_PATH=${10:-ready}
IMAGE_TAG=${11:-latest}
EXTERNAL_HEALTH_URL=${12:-${EXTERNAL_HEALTH_URL:-}}
REQUIRE_EXTERNAL_HEALTH=${13:-false}

NAMESPACE=$DOCKER_HUB_USERNAME
IMAGE_REPO="${NAMESPACE}/${REPO_NAME}"
TARGET_IMAGE="${IMAGE_REPO}:${IMAGE_TAG}"
COMPOSE_FILE="${DIR_PROJECT}/docker-compose.yml"
NGINX_FILE="/etc/nginx/sites-available/${NGINX_DOMAIN}"
MOB_NGINX_FALLBACK_FILE="/etc/nginx/sites-available/smr-signal-deck.conf"
MOB_NGINX_INCLUDE_FILE="/etc/nginx/sites-available/smr-signal-deck-mob.inc"
MOB_NGINX_PARENT_FILE="/etc/nginx/sites-available/smr-signal-deck.conf"
ASSETLINKS_SOURCE="${DIR_PROJECT}/assetlinks.json"
ASSETLINKS_TARGET="/var/www/flipsync/.well-known/assetlinks.json"

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

require_command() {
  local cmd=$1
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Missing required command: ${cmd}"
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

external_http_code() {
  local status
  if [ -z "$EXTERNAL_HEALTH_URL" ]; then
    echo "SKIP"
    return 0
  fi

  status=$(curl --location --write-out '%{http_code}' --silent --output /dev/null "$EXTERNAL_HEALTH_URL" || true)
  if [ -z "$status" ]; then
    status="000"
  fi
  echo "$status"
}

has_mob_location() {
  local file=$1
  grep -Eq '^[[:space:]]*location[[:space:]]+([^[:space:]]+[[:space:]]+)?/mob/?' "$file"
}

extract_proxy_port() {
  local file=$1
  awk '
    /^[[:space:]]*location[[:space:]]/ { in_mob = ($0 ~ /\/mob\/?/) }
    in_mob && /proxy_pass[[:space:]]+http:\/\/127\.0\.0\.1:[0-9]+\/?;/ {
      line=$0
      sub(/^.*127\.0\.0\.1:/, "", line)
      sub(/[^0-9].*$/, "", line)
      print line
      exit
    }
  ' "$file"
}

replace_proxy_port() {
  local file=$1
  local current_port=$2
  local target_port=$3
  local tmp_file="${file}.tmp.$$"
  awk -v current_port="$current_port" -v target_port="$target_port" '
    /^[[:space:]]*location[[:space:]]/ { in_mob = ($0 ~ /\/mob\/?/) }
    in_mob && $0 ~ "proxy_pass[[:space:]]+http://127\\.0\\.0\\.1:" current_port "(/)?;" {
      sub("127\\.0\\.0\\.1:" current_port, "127.0.0.1:" target_port)
    }
    { print }
  ' "$file" > "$tmp_file"
  mv "$tmp_file" "$file"
}

resolve_nginx_file() {
  local configured_file=$1

  if [ -f "$configured_file" ] && has_mob_location "$configured_file"; then
    echo "$configured_file"
    return
  fi

  if [ -f "$MOB_NGINX_FALLBACK_FILE" ] && has_mob_location "$MOB_NGINX_FALLBACK_FILE"; then
    echo "$MOB_NGINX_FALLBACK_FILE"
    return
  fi

  echo "$configured_file"
}

rollback_nginx_file() {
  local backup_file=$1
  if [ -f "$backup_file" ]; then
    echo "Rolling back Nginx config from backup: $backup_file"
    cp -p "$backup_file" "$NGINX_FILE"
  fi
}

ensure_mob_include_loaded() {
  local include_file=$1
  local backup_file
  local tmp_file

  if [ "$include_file" != "$MOB_NGINX_INCLUDE_FILE" ]; then
    return 0
  fi

  if [ ! -f "$MOB_NGINX_PARENT_FILE" ]; then
    echo "Mob include parent file not found, skipping include check: $MOB_NGINX_PARENT_FILE"
    return 0
  fi

  if grep -Fq "include ${include_file};" "$MOB_NGINX_PARENT_FILE"; then
    echo "Mob include already loaded by parent Nginx file"
    return 0
  fi

  backup_file="${MOB_NGINX_PARENT_FILE}.bak.$(date +%Y%m%d%H%M%S)"
  tmp_file="${MOB_NGINX_PARENT_FILE}.tmp.$$"
  cp -p "$MOB_NGINX_PARENT_FILE" "$backup_file"
  echo "Mob include missing from parent file. Backup created: $backup_file"

  awk -v include_line="    include ${include_file};" '
    BEGIN { inserted = 0 }
    /^[[:space:]]*location[[:space:]]*=[[:space:]]*\/api\/analyze/ && inserted == 0 {
      print include_line
      print ""
      inserted = 1
    }
    { print }
    END {
      if (inserted == 0) {
        exit 2
      }
    }
  ' "$MOB_NGINX_PARENT_FILE" > "$tmp_file" || {
    echo "Failed to insert mob include into parent Nginx file"
    rm -f "$tmp_file"
    return 1
  }

  mv "$tmp_file" "$MOB_NGINX_PARENT_FILE"
  if ! sudo nginx -t; then
    rollback_nginx_file "$backup_file"
    sudo nginx -t || true
    return 1
  fi

  sudo nginx -s reload
}

install_assetlinks_file() {
  if [ ! -f "$ASSETLINKS_SOURCE" ]; then
    echo "Asset links source file not found, skipping root assetlinks install: $ASSETLINKS_SOURCE"
    return 0
  fi

  sudo mkdir -p "$(dirname "$ASSETLINKS_TARGET")"
  sudo cp "$ASSETLINKS_SOURCE" "$ASSETLINKS_TARGET"
  sudo chmod 755 /var/www /var/www/flipsync "$(dirname "$ASSETLINKS_TARGET")"
  sudo chmod 644 "$ASSETLINKS_TARGET"
  echo "Installed assetlinks.json: $ASSETLINKS_TARGET"
}

ensure_assetlinks_nginx_location() {
  local nginx_file=$1
  local assetlinks_target=$2
  local backup_file
  local tmp_file
  local block

  backup_file="${nginx_file}.assetlinks.bak.$(date +%Y%m%d%H%M%S)"
  tmp_file="${nginx_file}.assetlinks.tmp.$$"
  cp -p "$nginx_file" "$backup_file"
  echo "Nginx assetlinks backup created: $backup_file"

  if grep -Eq 'location[[:space:]]*=[[:space:]]*/\.well-known/assetlinks\.json' "$nginx_file"; then
    awk -v assetlinks_target="$assetlinks_target" '
      /^[[:space:]]*location[[:space:]]*=[[:space:]]*\/\.well-known\/assetlinks\.json[[:space:]]*\{/ {
        in_assetlinks = 1
        alias_seen = 0
        print
        next
      }
      in_assetlinks && /^[[:space:]]*alias[[:space:]]+/ {
        print "        alias " assetlinks_target ";"
        alias_seen = 1
        next
      }
      in_assetlinks && /^[[:space:]]*\}/ {
        if (alias_seen == 0) {
          print "        alias " assetlinks_target ";"
        }
        in_assetlinks = 0
        print
        next
      }
      { print }
    ' "$nginx_file" > "$tmp_file"

    mv "$tmp_file" "$nginx_file"
    if ! sudo nginx -t; then
      echo "Assetlinks Nginx configuration update failed. Rolling back."
      cp -p "$backup_file" "$nginx_file"
      sudo nginx -t || true
      return 1
    fi

    sudo nginx -s reload
    echo "Nginx assetlinks location updated for /.well-known/assetlinks.json"
    return 0
  fi

  block=$(cat <<EOF
    location = /.well-known/assetlinks.json {
        default_type application/json;
        add_header Cache-Control "public, max-age=300" always;
        alias ${assetlinks_target};
    }

EOF
)

  if grep -Eq '^[[:space:]]*server[[:space:]]*\{' "$nginx_file"; then
    awk -v block="$block" '
      BEGIN { inserted = 0 }
      {
        print
        if (inserted == 0 && $0 ~ /^[[:space:]]*server[[:space:]]*\{/) {
          printf "%s", block
          inserted = 1
        }
      }
      END {
        if (inserted == 0) {
          exit 2
        }
      }
    ' "$nginx_file" > "$tmp_file" || {
      echo "Failed to insert assetlinks location into server block"
      rm -f "$tmp_file"
      cp -p "$backup_file" "$nginx_file"
      return 1
    }
  else
    {
      printf "%s" "$block"
      cat "$nginx_file"
    } > "$tmp_file"
  fi

  mv "$tmp_file" "$nginx_file"
  if ! sudo nginx -t; then
    echo "Assetlinks Nginx configuration test failed. Rolling back."
    cp -p "$backup_file" "$nginx_file"
    sudo nginx -t || true
    return 1
  fi

  sudo nginx -s reload
  echo "Nginx assetlinks location installed for /.well-known/assetlinks.json"
}

reload_nginx_or_rollback() {
  local backup_file=$1
  local output

  if ! output=$(sudo nginx -t 2>&1); then
    echo "Nginx configuration test failed"
    echo "$output"
    rollback_nginx_file "$backup_file"
    echo "Nginx configuration restored from backup. Re-testing restored config."
    sudo nginx -t || true
    return 1
  fi

  echo "$output"
  sudo nginx -s reload
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
if [ -z "$EXTERNAL_HEALTH_URL" ] && [[ "$NGINX_DOMAIN" == *.* ]] && [[ "$NGINX_DOMAIN" != *.conf ]]; then
  EXTERNAL_HEALTH_URL="https://${NGINX_DOMAIN}/mob/${HEALTH_CHECK_PATH}"
fi

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
echo "\$11 IMAGE_TAG=$IMAGE_TAG"
echo "\$12 EXTERNAL_HEALTH_URL=${EXTERNAL_HEALTH_URL:-SKIP}"
echo "\$13 REQUIRE_EXTERNAL_HEALTH=$REQUIRE_EXTERNAL_HEALTH"

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

log_section "preflight"
require_command docker
require_command docker-compose
require_command curl
require_command awk
require_command grep
require_command sed
require_command cp
require_command mv
require_command sudo
if ! sudo -n true >/dev/null 2>&1; then
  echo "Passwordless sudo is required for deployment"
  exit 1
fi
echo "preflight ok"

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
RESOLVED_NGINX_FILE=$(resolve_nginx_file "$NGINX_FILE")
if [ "$RESOLVED_NGINX_FILE" != "$NGINX_FILE" ]; then
  echo "Using mob nginx file: $RESOLVED_NGINX_FILE"
  NGINX_FILE="$RESOLVED_NGINX_FILE"
fi
if [ ! -f "$NGINX_FILE" ]; then
    echo "Nginx file not found: $NGINX_FILE"
    echo "Checked configured file and fallback file: $MOB_NGINX_FALLBACK_FILE"
    exit 1
fi
if ! has_mob_location "$NGINX_FILE"; then
    echo "Nginx /mob location not found in: $NGINX_FILE"
    exit 1
fi
ensure_mob_include_loaded "$NGINX_FILE"
install_assetlinks_file
ensure_assetlinks_nginx_location "$NGINX_FILE" "$ASSETLINKS_TARGET"
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
echo "TARGET_IMAGE=$TARGET_IMAGE"

cat << EOF > "$COMPOSE_FILE"
services:
  ${TARGET_COLOR}:
    image: ${TARGET_IMAGE}
    container_name: ${REPO_NAME}-${TARGET_COLOR}
    ports:
      - "${TARGET_PORT}:${INNER_PORT}"
    volumes:
      - ${DIR_PROJECT}/logs:/logs
    restart: always
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=20MB
      - SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=50MB
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
      - SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=20MB
      - SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=50MB
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
TARGET_CONTAINER_IDS=$(docker ps -a --filter "name=${REPO_NAME}-${TARGET_COLOR}" --format '{{.ID}}')
if [ -n "$TARGET_CONTAINER_IDS" ]; then
  echo "$TARGET_CONTAINER_IDS" | xargs -r docker rm -f
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
NGINX_BACKUP_FILE=""

if [ "$current_port" = "$NGINX_TARGET_PORT" ]; then
echo "Nginx already points to port $current_port"
else
if [ -z "$current_port" ]; then
  echo "Failed to extract current port from $NGINX_FILE"
  exit 1
fi
NGINX_BACKUP_FILE="${NGINX_FILE}.bak.$(date +%Y%m%d%H%M%S)"
cp -p "$NGINX_FILE" "$NGINX_BACKUP_FILE"
echo "Nginx config backup created: $NGINX_BACKUP_FILE"
replace_proxy_port "$NGINX_FILE" "$current_port" "$NGINX_TARGET_PORT"
if ! reload_nginx_or_rollback "$NGINX_BACKUP_FILE"; then
  exit 1
fi

fi

if [ "$IS_SUCCESS" -eq 0 ]; then
  EXTERNAL_STATUS=$(external_http_code)
  if [ "$EXTERNAL_STATUS" = "SKIP" ]; then
    echo "External health check skipped"
  else
    echo "External health check $EXTERNAL_HEALTH_URL => $EXTERNAL_STATUS"
    if [ "$EXTERNAL_STATUS" != "200" ]; then
      if [ "$REQUIRE_EXTERNAL_HEALTH" = "true" ]; then
        echo "External health check failed. Rolling back Nginx to live port $LIVE_PORT"
        if [ -n "$NGINX_BACKUP_FILE" ]; then
          rollback_nginx_file "$NGINX_BACKUP_FILE"
        else
          replace_proxy_port "$NGINX_FILE" "$NGINX_TARGET_PORT" "$LIVE_PORT"
        fi
        sudo nginx -t
        sudo nginx -s reload
        NGINX_TARGET_PORT=$LIVE_PORT
        IS_SUCCESS=1
      else
        echo "External health check failed, but REQUIRE_EXTERNAL_HEALTH=false. Keeping target and leaving external smoke test to the runner/operator."
      fi
    fi
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
