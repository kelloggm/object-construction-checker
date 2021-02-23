#!/usr/bin/env bash

set -e
set -u
set -o pipefail

# this script performs the ablation study in section 7.2 on the Zookeeper benchmark.

# prereq: zookeeper is checked out in the current directory, with the no-lo, no-ra, no-af branches available locally.
# prereq: run-always-call-on-zookeeper.sh is in the current directory
# prereq: the checker and the Checker Framework are built in the appropriate places

run_ablation () {

    variant_name=$1

    cd zookeeper

    git checkout "${variant_name}"

    cd ..

    sh run-always-call-on-zookeeper.sh &> "${variant_name}-results" || true

    echo "new errors produced by ${variant_name} configuration:"

    ./errors-without-custom-types.sh "${variant_name}-results" | grep "error:" | sort | uniq | wc -l

    echo "old errors no longer produced by ${variant_name} configuration:"

    grep "unneeded.suppression" "${variant_name}-results" | wc -l

    echo "the result for the paper is the difference between these two numbers"

}

run_ablation "no-lo"

echo ""

run_ablation "no-ra"

echo ""

run_ablation "no-af"
