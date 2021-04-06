#!/usr/bin/env bash

set -e
set -u
set -o pipefail


# This script builds the Checker Framework and our system at the appropriate
# commit hashes and installs them. It is idempotent.

# This script needs to be run at least once before running the ./run-always-call-on-*.sh
# scripts in the ablation/ directory, which can be used to run the individual benchmarks
# after you've checked them out.

# prereq: JAVA_HOME must point to a Java 11 JDK

PLUMBER_BRANCH=always-call-checker
PLUMBER_REPO=https://github.com/kelloggm/object-construction-checker.git

# uncomment these lines if you need to use a local copy of the Checker Framework

# CF_BRANCH=master
# CF_REPO=https://github.com/typetools/checker-framework.git

# clone + build the CF
# if [ ! -d checker-framework ]; then
#     git clone "${CF_REPO}"
# fi

# cd checker-framework
# git checkout "${CF_BRANCH}"
# git pull
# ./gradlew publishToMavenLocal
# cd ..

# clone + build plumber
if [ ! -d plumber ]; then
    git clone "${PLUMBER_REPO}"
fi

cd plumber
git checkout "${PLUMBER_BRANCH}"
git pull
./gradlew install
cd ..