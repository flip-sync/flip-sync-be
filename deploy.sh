#!/bin/bash

REPO_NAME=$1
DIR_PROJECT=$2
BLUE_PORT=$3
GREEN_PORT=$4
INNER_PORT=$5
DOWN_DELTA_TIME=$6
DOCKER_HUB_USERNAME=$7
TOKEN=$8
NGINX_DOMAIN=$9
HEALTH_CHECK_PATH=${10}

NAMESPACE=$DOCKER_HUB_USERNAME


echo "===================="
echo "실행중인 백그라운드 종료"
if [ -f "$DIR_PROJECT/sh/down_live_after_delay.pid" ]; then
    OLD_PID=$(cat "$DIR_PROJECT/sh/down_live_after_delay.pid")
    if ps -p "$OLD_PID" > /dev/null; then
        echo "Terminating existing background process..."
        kill -9 "$OLD_PID"
    else
        echo "No existing background process found with PID $OLD_PID"
    fi
else
    echo "No existing background process found"
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
echo "port check"
# sites-available/$NGINX_DOMAIN 파일을 생성해 놔야함
# 포트 번호 추출
NOW_PORT=$(grep 'proxy_pass' "/etc/nginx/sites-available/${NGINX_DOMAIN}" | grep -oP '(?<=:)\d+(?=;)')

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
echo "create docker compose"

URL="https://hub.docker.com/v2/repositories/$NAMESPACE/$REPO_NAME/tags"

# 태그 정보 가져오기
BEFORE_TAG=$(curl -s -H "Authorization: Bearer $TOKEN" "$URL" | jq -r '
           .results[].name' | grep -v '^latest$' | sort -n | tail -2 | head -1)

# 결과 출력
echo "Before Tag: $BEFORE_TAG"
echo "Live color tag: $BEFORE_TAG"

cat << EOF > "$DIR_PROJECT/docker-compose.yml"
services:
  ${TARGET_COLOR}:
    image: ${NAMESPACE}/${REPO_NAME}:latest
    container_name: ${REPO_NAME}-${TARGET_COLOR}
    ports:
      - "${TARGET_PORT}:${INNER_PORT}"
    volumes:
      - ${DIR_PROJECT}/logs:/logs
    restart: always
    environment:
      - TZ=Asia/Seoul
  ${LIVE_COLOR}:
    image: ${NAMESPACE}/${REPO_NAME}:${BEFORE_TAG}
    container_name: ${REPO_NAME}-${LIVE_COLOR}
    ports:
      - "${LIVE_PORT}:${INNER_PORT}"
    volumes:
      - ${DIR_PROJECT}/logs:/logs
    environment:
      - TZ=Asia/Seoul
EOF
echo "===================="


echo "===================="
echo "now state"
Target=$(curl --write-out '%{http_code}' --silent --output /dev/null "127.0.0.1:$TARGET_PORT/$HEALTH_CHECK_PATH")
Live=$(curl --write-out '%{http_code}' --silent --output /dev/null "127.0.0.1:$LIVE_PORT/$HEALTH_CHECK_PATH")

echo "API Target: $Target"
echo "API Live: $Live"
echo "===================="


echo "===================="
echo "docker login"
echo "$TOKEN" | docker login -u "$DOCKER_HUB_USERNAME" --password-stdin || exit 1
echo "===================="

# Deploy the new version to the new environment
echo "===================="
echo "docker compose up"

which docker-compose || exit 1

docker-compose -f "$DIR_PROJECT/docker-compose.yml" pull ${TARGET_COLOR} || exit 1
docker-compose -f "$DIR_PROJECT/docker-compose.yml" up -d ${TARGET_COLOR} || exit 1

Target=$(curl --write-out '%{http_code}' --silent --output /dev/null "127.0.0.1:$TARGET_PORT/$HEALTH_CHECK_PATH")
Live=$(curl --write-out '%{http_code}' --silent --output /dev/null "127.0.0.1:$LIVE_PORT/$HEALTH_CHECK_PATH")

echo "API Target: $Target"
echo "API Live: $Live"
echo "===================="

run_tests() {
  # shellcheck disable=SC2034
  for i in {1..10}; do
    Target=$(curl --write-out '%{http_code}' --silent --output /dev/null "127.0.0.1:$TARGET_PORT/$HEALTH_CHECK_PATH")
    Live=$(curl --write-out '%{http_code}' --silent --output /dev/null "127.0.0.1:$LIVE_PORT/$HEALTH_CHECK_PATH")

    echo "API Target: $Target"
    echo "API Live: $Live"

    if [ "$Target" -eq 200 ]; then
      return 0
    fi
    echo "API is not ready yet cnt: [$i/10]]"
    # Wait for 6 seconds before the next try
    sleep 6
  done

  echo "API has not become ready within 60 seconds"
  # If the API did not respond with 200 OK within 60 seconds, return a failure code
  return 1
}


echo "===================="
echo "run tests"
if run_tests; then
  # If Lives passed, switch the traffic to the new environment
  echo "Success!! Change Target: $TARGET_COLOR"
  NGINX_TARGET_PORT=$TARGET_PORT
  IS_SUCCESS=0

else
  # If Lives failed, stop the new environment
  echo "Fail.. Change LIVE: $LIVE_COLOR"
  NGINX_TARGET_PORT=$LIVE_PORT
  IS_SUCCESS=1
fi
echo "===================="



echo "===================="
echo "nginx check"
current_port=$(grep 'proxy_pass' "/etc/nginx/sites-available/$NGINX_DOMAIN" | awk -F: '{print $3}' | awk -F/ '{print $1}' | awk -F\; '{print $1}')

if [ "$current_port" -eq "$NGINX_TARGET_PORT" ]; then
echo "same port"
echo "current_port: $current_port"
echo "NGINX_TARGET_PORT: $NGINX_TARGET_PORT"
exit 1
else
sed -i "s/proxy_pass http:\/\/127.0.0.1:$current_port;/proxy_pass http:\/\/127.0.0.1:$NGINX_TARGET_PORT;/" "/etc/nginx/sites-available/$NGINX_DOMAIN"
# Nginx 설정 검사
output=$(sudo nginx -t 2>&1)

# "success" 문구가 있는지 확인
if [[ $output == *"test is successful"* ]]; then
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

echo "===================="
echo "down live after delay"

if [ "$IS_SUCCESS" ]; then
  if docker ps --format '{{.Names}}' | grep "$REPO_NAME-$LIVE_COLOR" > /dev/null; then

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
    echo "Not Found Live: $LIVE_COLOR"
  fi
else
  if docker ps --format '{{.Names}}' | grep "$REPO_NAME-$TARGET_COLOR" > /dev/null; then
    echo "Down Target: $TARGET_COLOR"
    docker stop "$REPO_NAME-$TARGET_COLOR"
  else
    echo "Not Found Target: $TARGET_COLOR"
  fi
fi
echo "===================="
