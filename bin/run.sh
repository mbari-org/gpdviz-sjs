#!/usr/bin/env bash

JAVA_HOME=`/usr/libexec/java_home -v 1.8.0_121` java -jar jvm/target/scala-2.12/gpdviz-assembly-0.2.0.jar "$@"
