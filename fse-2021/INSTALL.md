#### Reproducing our results

To reproduce our results, you can use our docker image. Install Docker
and follow the instructions at the beginning of README.md.

#### Kicking the tires

See the "Kicking the tires" section of file README.md.

#### Extending our work

The `object-construction-checker` directory contains the source code for
the tool.

Its test suite (a set of simple Java programs with expected errors) appears
in the the `object-construction-checker/tests/mustcall` and
`object-construction-checker/tests/socket` subdirectories. To run the
tests, run `./gradlew build` from the root directory. You can add new tests
by placing them in one of these directories, if you want to see how the
tool works on small examples.

This artifact contains a version of our tool that reproduces the numbers in
the paper.  Going forward, the tool will be distributed with the Checker
Framework (https://checkerframework.org) and called the "Resource Leak
Checker".  Future researchers who wish to build on or compare against our
tool should use that version, which will incorporate bug fixes and
precision improvements.
