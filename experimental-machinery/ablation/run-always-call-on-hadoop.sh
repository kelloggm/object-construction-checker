#!/usr/bin/env bash

if [ "x${JAVA8_HOME}" = "x" ]; then
  echo "Please set JAVA8_HOME to run the checker on ZooKeeper. ZooKeeper requires Java 8."
  exit 1
else
  java_home_old="${JAVA_HOME}"
  export JAVA_HOME="${JAVA8_HOME}"
fi

cd hadoop
mvn --projects hadoop-hdfs-project/hadoop-hdfs --also-make clean compile -DskipTests

export JAVA_HOME="${java_home_old}"
