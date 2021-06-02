#!/bin/sh

# a simple script to post-process a run of the must-call checker on zookeeper

# to remove 1) irrelevant lines not related to errors, and 2) required.method.not.called

# errors on custom types

# input: a file containing the output of ./run-always-call-on-zookeeper.sh

zookeeper_out=$1

cat "${zookeeper_out}" | grep "ERROR" | grep -v "The type of object is: org." | grep -v "mustcall:inconsistent.mustcall.subtype"
