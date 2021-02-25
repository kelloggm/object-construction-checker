## Plumber

This is the anonymized repository containing the Plumber tool described in the
paper "Lightweight and Modular Resource Leak Verification", which was submitted
to FSE 2021.

As a reviewer, you should be able to inspect anything in this repository and
the other repositories owned by this user, without compromising double-blind
(we hope!). The repositories are:
* plumber (this one!): the tool itself, its test suite, and our experimental machinery
* checker-framework: a hard fork of the Checker Framework, fixed at the commit on their master branch that we used
                     for our experiments. We didn't make any modifications to this; you could diff it against
                     github.com/typetools/checker-framework/ at commit id 12fb7e65015015bbba541bf0cfee6270d4d25913,
                     but we included it here so these repos are self-contained
* zookeeper: our copy of apache/zookeeper
* hbase: our copy of apache/hbase
* hadoop: our copy of apache/hadoop

There are six interesting branches for zookeeper, hbase, and hadoop:
* master: the original master branch, before we made any modifications. These are fixed at the point
          when we started making edits (i.e. at the commit we analyzed).
* with-checker: master modified with its build system modified to run Plumber.
* with-annotations: with-checker modified by adding annotations. These versions are the ones we used to collect
                    the results in table 1 (except LoC, which used master).
* no-lo, no-ra, and no-af: these are the branches that run Plumber in the three conditions for the ablation
                           study in section 8.2. no-lo is set up to run without lightweight ownership (section 4),
                           no-ra without resource aliasing (section 5), and no-af without ownership creation annotations
                           (section 6).
             
There are a few differences in terminology between Plumber's implementation and the paper:
* @MustCallAlias is called @MustCallChoice
* @CreateObligation is called @ResetMustCall, and obligation creation is referred to throughout as "accumulation frames"

The three parts of section 3 correspond to three different parts of this repository:
* section 3.1 corresponds to the must-call-checker subproject.
* section 3.2 corresponds to the object-construction-checker (this repo was originally hard fork of the original
object-construction-checker in Kellogg et al. ICSE 20, which we built off of), with the exception of the file
MustCallInvokedChecker.java.
* section 3.3 corresponds to MustCallInvokedChecker.java.

The implementations of the features described in sections 4-6 are scattered throughout the 3 places listed above,
as appropriate.
