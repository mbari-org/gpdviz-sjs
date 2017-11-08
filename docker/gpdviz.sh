#!/usr/bin/env bash

echo "launch_gpdviz: waiting for bit before connecting to db"
sleep 10

echo "launch_gpdviz: running server..."
java -jar jar/gpdviz.jar run-server -d
