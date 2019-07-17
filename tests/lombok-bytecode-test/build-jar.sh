#!/bin/sh

# builds the foo.jar file used in the lombok tests

# one argument: the path to a lombok distribution to use to compile Foo.java

LOMBOK_JAR=$1

javac -cp ${LOMBOK_JAR} Foo.java

jar cf foo.jar *.class

rm *.class
