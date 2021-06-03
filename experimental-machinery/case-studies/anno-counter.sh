#!/bin/sh

# A simple script to count annotations in the case studies.
# The argument is the name of the project.
# TODO: why did we uses this instead of AnnotationStatistics? IIRC we had some trouble getting
# it to play nicely with Maven? I do remember double-checking the output of this very carefully.

project=$1

echo "@Owning:"
grep -EoniR "@Owning" "${project}" | wc -l

echo "@NotOwning:"
grep -EoniR "@NotOwning" "${project}" | wc -l

echo "@EnsuresCalledMethods:"
grep -EoniR "@EnsuresCalledMethods" "${project}" | wc -l

echo "@MustCall:"
grep -EoniR "@MustCall" "${project}" | grep -v "MustCallAlias" | wc -l

echo "@InheritableMustCall:"
grep -EoniR "@InheritableMustCall" "${project}" | wc -l

echo "@MustCallAlias:"
grep -EoniR "@MustCallAlias" "${project}" | wc -l

echo "@CreatesObligation:"
grep -EoniR "@CreatesObligation" "${project}" | wc -l
