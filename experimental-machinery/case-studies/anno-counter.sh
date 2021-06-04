#!/bin/sh

# A simple script to count annotations in the case studies.
# The argument is the name of the project.
# TODO: why did we uses this instead of AnnotationStatistics? IIRC we had some trouble getting
# it to play nicely with Maven? I do remember double-checking the output of this very carefully.

project=$1

echo "@Owning:"
grep -EoniR --include \*.java "@Owning" "${project}" | wc -l

echo "@NotOwning (counted with @Owning):"
grep -EoniR --include \*.java "@NotOwning" "${project}" | wc -l

echo "@EnsuresCalledMethods:" # This will also count @EnsuresCalledMethodsIf. That's by design.
grep -EoniR --include \*.java "@EnsuresCalledMethods" "${project}" | wc -l

echo "@MustCall:"
grep -EoniR --include \*.java "@MustCall\(" "${project}" | wc -l

echo "@InheritableMustCall (counted with @MustCall):"
grep -EoniR --include \*.java "@InheritableMustCall" "${project}" | wc -l

echo "@MustCallAlias:"
grep -EoniR --include \*.java "@MustCallAlias" "${project}" | wc -l

echo "@PolyMustCall (counted with @MustCallAlias):"
grep -EoniR --include \*.java "@PolyMustCall" "${project}" | wc -l

echo "@CreatesObligation:"
grep -EoniR --include \*.java "@CreatesObligation" "${project}" | wc -l
