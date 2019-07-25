This directory contains the various test suites for the object construction checker.

Each test suite is meant to reflect a particular application domain. The test suites are:
* "basic": generic tests to make sure the typechecker is working as intended. These don't reflect an application domain.

To run the guice example use `mvn compile` followed by `mvn exec:java -Dexec.mainClass="Main"`.