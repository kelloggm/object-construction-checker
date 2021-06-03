#!/bin/sh

# input: a file containing the output of ./run-always-call-on-zookeeper.sh

# output: the input, without
# 1) irrelevant lines not related to errors, and
# 2) required.method.not.called errors on custom types (those starting with "org.").

zookeeper_out=$1

cat "${zookeeper_out}" | grep "ERROR" | grep -v "The type of object is: org." | grep -v "mustcall:inconsistent.mustcall.subtype"
