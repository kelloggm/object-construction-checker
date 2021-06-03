cd zookeeper || exit 1
mvn -B --projects zookeeper-server --also-make clean install -DskipTests
