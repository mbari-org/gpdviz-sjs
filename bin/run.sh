#!/usr/bin/env bash

if [ "${PUSHER_APPID}" == "" ]; then
    echo "PUSHER_APPID undefined.  Forgot 'source setenv.sh'?"
    exit 1
fi

JAVA_HOME=`/usr/libexec/java_home -v 1.8.0_121` java -jar jvm/target/scala-2.12/gpdviz-assembly-0.1.0.jar "$@"
