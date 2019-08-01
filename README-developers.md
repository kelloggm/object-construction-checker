To build a version of the Object Construction Checker, and install it locally:

```
git clone https://github.com/msridhar/returnsrecv-checker.git
(cd returnsrecv-checker && ./gradlew build && ./gradlew install)
git clone https://github.com/kelloggm/object-construction-checker.git
(cd object-construction-checker && ./gradlew build && ./gradlew install)
```

To make Gradle use it, add to your `build.gradle` file (in addition to the
instructions elsewhere):

```
repositories {
    mavenLocal()
}
```
