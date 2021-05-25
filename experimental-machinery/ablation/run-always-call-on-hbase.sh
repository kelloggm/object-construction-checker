#set -x

cd hbase
mvn --projects hbase-server --also-make clean compile -DskipTest
