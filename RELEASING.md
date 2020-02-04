Releasing
=========

Note that these steps require pushing directly to the master branch on GitHub.  Be sure that other developers are not pushing changes concurrently.

 1. Change the version in `gradle.properties` to a non-SNAPSHOT version.
 2. Update `README.md` with the new release version.
 3. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
 4. `git tag -a vX.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
 5. `./gradlew -Dorg.gradle.parallel=false clean uploadArchives`
 6. Update the `gradle.properties` to the next SNAPSHOT version.
 7. Update `README-developers.md` to refer to the new SNAPSHOT version.
 8. `git commit -am "Prepare next development version."`
 9. `git push && git push --tags`
 10. Visit [Sonatype Nexus](https://oss.sonatype.org/) and promote the artifact.
