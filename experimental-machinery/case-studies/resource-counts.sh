#!/usr/bin/env bash

# A simple script for finding the output from the -AcountMustCall option to the checker.
# The input should be a single file containing the output of running the checker before
# any output is filtered.

grep "obligation(s)" $1
