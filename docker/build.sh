#!/usr/bin/env bash
set -e

if [ "$1" == "gpdviz" ]; then
    GPDVIZ_VERSION=$(sed -E 's/gpdviz.version = (.+)$/\1/' jvm/src/main/resources/reference.conf)
    echo "Using GPDVIZ_VERSION=${GPDVIZ_VERSION}"
    docker build -t "gpdviz:${GPDVIZ_VERSION}" \
           --build-arg GPDVIZ_VERSION=${GPDVIZ_VERSION}  .

elif [ "$1" == "gpdviz_db" ]; then
    docker build -t gpdviz_db:0.0.1 -f docker/Dockerfile-db docker

else
    echo "USAGE: docker/build.sh [gpdviz] [gpdviz_db]"
fi
