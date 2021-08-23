#!/usr/bin/env bash                                                                      

set -e
set -u
set -o pipefail

# This script computes the time taken to type-check benchmark programs.
# The output of this script is a set of files, each of which contains timing
# information for a single run. these results will be zipped into a single
# file called results.zip that will appear in the original directory.

# This script requires two variables to be set before it runs:
# JAVA11_HOME must point towards a Java 11 JDK
# JAVA8_HOME must point towards a Java 8 JDK

# It is suggested that you create a new temporary directory before running
# this script. It will place the results in the results/ subdirectory of the
# directory from which it is run.
# For example:
#   DIR=$HOME/tmp/build-all-and-collect-timing-info-$(hostname)
#   rm -rf $DIR
#   mkdir -p $DIR
#   cd $DIR
#   ln -s $t/object-construction-checker-branch-always-call-checker/experimental-machinery/case-studies/build-all-and-collect-timing-info.sh .
#   ./build-all-and-collect-timing-info.sh 2>&1 | tee out.txt


# Internally, this script builds the Checker Framework and our system at the appropriate
# commit hashes, installs them, and then downloads and analyzes each
# benchmark, printing out the time taken. The time taken is computed
# using the time command, so this script might not work without
# modification on a Mac. The script then cleans and re-analyzes each
# benchmark $ATTEMPTS times. In the paper, we use the median (?) of the
# results.


# configuration

ATTEMPTS=5

CF_BRANCH=current-occ-version
CF_REPO=https://github.com/msridhar/checker-framework.git

OCC_BRANCH=master
OCC_REPO=https://github.com/kelloggm/object-construction-checker.git

ZK_BRANCH=with-annotations
ZK_REPO=https://github.com/kelloggm/zookeeper.git
ZK_CMD="mvn --projects zookeeper-server --also-make clean install -DskipTests"
ZK_CLEAN="mvn clean"

HADOOP_BRANCH=with-annotations
HADOOP_REPO=https://github.com/Nargeshdb/hadoop
HADOOP_CMD="mvn --projects hadoop-hdfs-project/hadoop-hdfs --also-make clean compile -DskipTests"
HADOOP_CLEAN="mvn clean"

HBASE_BRANCH=with-annotations
HBASE_REPO=https://github.com/Nargeshdb/hbase
HBASE_CMD="mvn --projects hbase-server --also-make clean compile -DskipTests"
HBASE_CLEAN="mvn clean"

PU_BRANCH=with-annotations
PU_REPO=https://github.com/kelloggm/plume-util.git
PU_CMD="./gradlew compileJava"
PU_CLEAN="./gradlew clean"

# script starts

# testing for JAVA8_HOME, not an unintentional reference to JAVA8_HOME
# shellcheck disable=SC2153
if [ "x${JAVA8_HOME}" = "x" ]; then
  echo "JAVA8_HOME not set"
  exit 1
fi

# testing for JAVA11_HOME, not an unintentional reference to JAVA8_HOME
# shellcheck disable=SC2153
if [ "x${JAVA11_HOME}" = "x" ]; then
  echo "JAVA11_HOME not set"
  exit 1
fi

OLD_JAVA_HOME="${JAVA_HOME}"

CURDIR=$(pwd)
RESULTS="${CURDIR}/results"

if [ -d "${RESULTS}" ]; then
    rm -rf "${RESULTS}"
fi

mkdir "${RESULTS}"

export JAVA_HOME="${JAVA11_HOME}"

# clone + build OCC
if [ ! -d object-construction-checker ]; then
    git clone "${OCC_REPO}"
fi

cd object-construction-checker
git checkout "${OCC_BRANCH}"
git pull
./gradlew install
cd ..

export JAVA_HOME="${JAVA8_HOME}"

# download Zookeeper
if [ ! -d zookeeper ]; then
    git clone "${ZK_REPO}"
fi

cd zookeeper
git checkout "${ZK_BRANCH}"
git pull
# do $ATTEMPTS trials on Zookeeper
for i in $(seq "${ATTEMPTS}"); do
    ${ZK_CLEAN} &> /dev/null
    echo "attempt ${i} on Zookeeper starting:"
    time (${ZK_CMD} &> "${RESULTS}/zookeeper.run.${i}.log" || true) &> "${RESULTS}/zookeeper.run.${i}.time"
done
cd ..

# download Hadoop
if [ ! -d hadoop ]; then
    git clone "${HADOOP_REPO}"
fi

cd hadoop
git checkout "${HADOOP_BRANCH}"
git pull
# do $ATTEMPTS trials on Hadoop
for i in $(seq "${ATTEMPTS}"); do
    ${HADOOP_CLEAN} &> /dev/null
    echo "attempt ${i} on Hadoop starting:"
    time (${HADOOP_CMD} &> "${RESULTS}/hadoop.run.${i}.log" || true) &> "${RESULTS}/hadoop.run.${i}.time"
done
cd ..

if [ ! -d hbase ]; then
    git clone "${HBASE_REPO}"
fi

cd hbase
git checkout "${HBASE_BRANCH}"
git pull
# This is necessary, or the build command will fail. This command takes nearly
# 30 minutes to run, but I don't know how to avoid executing it.
# I think it sets up the dependencies, but I'm not all that confident.
mvn --projects hbase-server --also-make clean install -DskipTests &> hbase.install.log || echo "Could not build hbase-server"
# do $ATTEMPTS trials on HBase
for i in $(seq "${ATTEMPTS}"); do
    ${HBASE_CLEAN} &> /dev/null
    echo "attempt ${i} on HBase starting:"
    time (${HBASE_CMD} &> "${RESULTS}/hbase.run.${i}.log" || true) &> "${RESULTS}/hbase.run.${i}.time"
done
cd ..

# download plume-util
if [ ! -d plume-util ]; then
    git clone "${PU_REPO}"
fi

cd plume-util
git checkout "${PU_BRANCH}"
git pull
# do $ATTEMPTS trials on plume-util
for i in $(seq "${ATTEMPTS}"); do
    ${PU_CLEAN} &> /dev/null
    echo "attempt ${i} on plume-util starting:"
    time (${PU_CMD} &> "${RESULTS}/plume-util.run.${i}.log" || true) &> "${RESULTS}/plume-util.run.${i}.time"
done
cd ..

zip -r results.zip "${RESULTS}"

export JAVA_HOME="${OLD_JAVA_HOME}"
