To build a version of the Object Construction Checker, and install it locally:

```
git clone https://github.com/kelloggm/object-construction-checker.git
(cd object-construction-checker && ./gradlew install)
```

To make Gradle use it, add to your `build.gradle` file:

```
repositories {
    mavenLocal()
}
```

Then, follow the instructions in the other READMEs, using version `0.1.8-SNAPSHOT` of the Object Construction Checker artifacts.
