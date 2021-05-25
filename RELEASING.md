Releasing
=========

Note that these steps require pushing directly to the master branch on GitHub.  Be sure that other developers are not pushing changes concurrently.

 1. Change the version in `gradle.properties` to a non-SNAPSHOT version.  E.g., if the `VERSION_NAME` in `gradle.properties` is `X.Y.Z-SNAPSHOT`, change it to `X.Y.Z`.  Also change all mentions of the current version in `README.md` to refer to version `X.Y.Z` instead.
 2. `export NEWVER=X.Y.Z`, where `X.Y.Z` is the new version from step 1.
 3. `git commit -am "Prepare for release $NEWVER."`
 4. `git tag -a v"$NEWVER" -m "Version $NEWVER"`
 5. `./gradlew --no-parallel clean uploadArchives`
 6. Update the `gradle.properties` to the next SNAPSHOT version, `X.Y.Z+1-SNAPSHOT`.
 7. Update `README-developers.md` to refer to `X.Y.Z+1-SNAPSHOT`.
 8. `git commit -am "Prepare next development version."`
 9. `git push && git push --tags`
 10. Visit [Sonatype Nexus](https://oss.sonatype.org/) and promote the artifact.
