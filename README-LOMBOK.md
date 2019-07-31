# Using the Object Construction Checker with Lombok

This document describes how to use the Object Construction Checker with
[Lombok](https://projectlombok.org).
The Object Construction Checker guarantees, at compile time, that there
will be no run-time exceptions due to the programmer failing to set a
required field in a call to a builder.

What you’ll need:
* A project that uses Lombok via the [io.freefair.lombok](https://plugins.gradle.org/plugin/io.freefair.lombok) Gradle plugin.

What to do:

1. Add the [org.checkerframework](https://plugins.gradle.org/plugin/org.checkerframework) Gradle plugin to the `plugins` block of your `build.gradle` file:

```groovy
plugins {
    ...
    id "io.freefair.lombok" version "3.6.6"
    id "org.checkerframework" version "0.3.16"
}
```

2. Add the following to your `build.gradle` file (adding the entries to the extant `repositories` and `dependencies` blocks if present):

```groovy
repositories {
    mavenLocal()
    maven { url "http://repo.maven.apache.org/maven2" }
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots' }
}
checkerFramework {
    skipVersionCheck = true
    checkers = ['org.checkerframework.checker.objectconstruction.ObjectConstructionChecker']
    extraJavacArgs = ['-AsuppressWarnings=type.anno.before']
}
dependencies {
    checkerFramework 'net.sridharan.objectconstruction:object-construction-checker:0.1.1-SNAPSHOT'
    implementation 'net.sridharan.objectconstruction:object-construction-qual:0.1.1-SNAPSHOT'
}
```


After these two steps, building your program will run the checker and alert you at compile time if any required properties might not be set.
