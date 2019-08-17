# Using the Object Construction Checker with Lombok

This document describes how to use the Object Construction Checker with
[Lombok](https://projectlombok.org).
The Object Construction Checker guarantees, at compile time, that there
will be no run-time exceptions due to the programmer failing to set a
required field in a call to a builder.

What you’ll need:
* A project that uses Lombok via the [io.freefair.lombok](https://plugins.gradle.org/plugin/io.freefair.lombok) Gradle plugin.

What to do:

1. Add the [org.checkerframework](https://github.com/kelloggm/checkerframework-gradle-plugin) Gradle plugin to the `plugins` block of your `build.gradle` file:

  ```groovy
  plugins {
      ...
      id "io.freefair.lombok" version "3.6.6"
      id "org.checkerframework" version "0.3.22"
  }
  ```

2. For a vanilla Gradle project, add the following to your `build.gradle` file (adding the entries to the extant `repositories` and `dependencies` blocks if present).
If your project has subprojects or you need other customizations, see the documentation for the
[org.checkerframework](https://github.com/kelloggm/checkerframework-gradle-plugin) plugin.

  ```groovy
  repositories {
      mavenCentral()
  }
  checkerFramework {
      skipVersionCheck = true
      checkers = ['org.checkerframework.checker.objectconstruction.ObjectConstructionChecker']
      extraJavacArgs = ['-AsuppressWarnings=type.anno.before']
  }
  dependencies {
      checkerFramework 'net.sridharan.objectconstruction:object-construction-checker:0.1.3'
      implementation 'net.sridharan.objectconstruction:object-construction-qual:0.1.3'
  }
  ```


After these two steps, building your program will run the checker and alert you at compile time if any required properties might not be set.

You should edit your original source code, **not** the files in the checker's error messages.
The checker's error messages refer to Lombok's output, which is a variant of your source code that appears in a `delombok` directory.
