#!/usr/bin/env bash

cd "$(dirname "$0")" || exit 1

WAIT_SEC=0
RESTART=false

while [[ $# -gt 0 ]]; do
  case $1 in
    --wait)
      WAIT_SEC="$2"
      shift 2
      ;;
    --restart)
      RESTART=true
      shift
      ;;
    *)
      shift
      ;;
  esac
done

./gradlew build || exit 1

if [[ -n "$DEBUG_SERVER_PATH" ]]; then
  for f in build/libs/*.jar; do
    cp "$f" "$DEBUG_SERVER_PATH/plugins/" &&
    echo "$(basename "$f") deployed"
  done
fi

if [[ "$RESTART" == true ]]; then
  if [[ -n "$DEBUG_DOCKER_NAME" ]]; then
    echo "Restarting docker $DEBUG_DOCKER_NAME"
    docker restart "$DEBUG_DOCKER_NAME" > /dev/null
  else
    echo "Docker name isn't specified at \$DEBUG_DOCKER_NAME"
  fi
fi

if (( WAIT_SEC > 0 )); then
  echo "Waiting $WAIT_SEC seconds..."
  sleep "$WAIT_SEC"
fi
