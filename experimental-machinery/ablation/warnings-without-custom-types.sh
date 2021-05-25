#!/bin/sh

# a simple script to post-process a run of the must-call checker on hadoop or hbase
# to remove 1) irrelevant lines not related to errors, 2) required.method.not.called
# errors on custom types, 3) errors in hadoop-common, which isn't typechecked

# input: a file containing the output of compiling one of the above projects

out=$1

cat "${out}" | grep "WARNING" | grep -e "objectconstruction:" -e "mustcall:" | grep -v "The type of object is: org." | grep -v "mustcall:inconsistent.mustcall.subtype" | grep -v "The type of object is: <anonymous org." | grep -v "unneeded.suppression" | grep -v "annotation.not.completed" | grep -v "mustcall:type.invalid.annotations.on.use" | grep -v "hadoop/hadoop-common-project/hadoop-common"
