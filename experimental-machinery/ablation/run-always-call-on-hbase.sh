#set -x

cd hbase
# the install task must be run (as opposed to compile), as hbase
# requires that sub-project jars be installed in the local maven repo
mvn --projects hbase-server --also-make clean install -DskipTests
