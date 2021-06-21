#!/usr/bin/env bash

cd plume-util
# do a clean build since we always want full output from tool
./gradlew clean compileJava
