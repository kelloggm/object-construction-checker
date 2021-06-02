#set -x

cd hadoop
mvn --projects hadoop-hdfs-project/hadoop-hdfs --also-make clean compile -DskipTests
