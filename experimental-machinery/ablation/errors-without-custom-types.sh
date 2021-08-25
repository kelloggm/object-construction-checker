#!/bin/sh

# input: a file containing the output of ./run-always-call-on-zookeeper.sh

# output: the input, without
# 1) irrelevant lines not related to errors, and
# 2) required.method.not.called errors on custom types (those starting with "org.").

# PROBLEM: This script strips off information after the first line of an error message.  For example, the full output may contain
#   [ERROR] /home/mernst/research/types/cf-case-studies/zookeeper-fork-kelloggm-branch-with-annotations-verified-2/zookeeper-server/src/main/java/org/apache/zookeeper/server/quorum/QuorumCnxManager.java:[1214,29] error: [objectconstruction:contracts.postcondition.not.satisfied] postcondition of finish is not satisfied.
#     found   : no information about this.sock
#     required: this.sock is @CalledMethods("close")
# of which only the first line is retained in the output of this script.

zookeeper_out=$1

# shellcheck disable=SC2002
cat "${zookeeper_out}" | grep "ERROR" | grep -e "objectconstruction:" -e "mustcall:" -e "mustcallnoaccumulationframes:" | grep -v "The type of object is: org." | grep -v "mustcall:inconsistent.mustcall.subtype" | sort -u
