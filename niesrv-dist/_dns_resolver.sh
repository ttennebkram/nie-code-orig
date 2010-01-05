#!/bin/sh

# The main Java class to run
# ====================================
MAIN_CLASS=nie.sr2.util.DNSLookup2
# ====================================

# Where we store our many system files, RELATIVE to this directory.
SYSDIR=system
# We prepend the full directory path to it - don't change this.
SYSDIR=`dirname $0`/$SYSDIR
# To see what it does:
# echo SYSDIR = $SYSDIR

# Run class_runner with the name of the main class
$SYSDIR/class_runner.sh $MAIN_CLASS $*
