Releasing
=========

NOTE: these instructions assume we are cutting releases for the returns receiver checker in lockstep with this checker, with the same version number.

 1. Change the version in `gradle.properties` to a non-SNAPSHOT version.
 2. In the `build.gradle` file in object-construction-qual and object-construction-checker, change the dependencies to returnsrcvr-qual and returnsrcvr-checker to use the same version used in step 1.
 3. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
 4. `git tag -a vX.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
 5. `./gradlew -Dorg.gradle.parallel=false clean uploadArchives`
 6. Update the `gradle.properties` to the next SNAPSHOT version.
 7. In the `build.gradle` file in object-construction-qual and object-construction-checker, change the dependencies to returnsrcvr-qual and returnsrcvr-checker to use the same version used in step 6.
 8. `git commit -am "Prepare next development version."`
 9. `git push && git push --tags`
 10. Visit [Sonatype Nexus](https://oss.sonatype.org/) and promote the artifact.
