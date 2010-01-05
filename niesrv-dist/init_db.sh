#!/bin/sh

ARGS=-check_table -all -create_if_missing

# The main Java class to run
# ====================================
MAIN_CLASS=nie.core.DBConfig
# ====================================

# Where we store our many system files, RELATIVE to this directory.
SYSDIR=system
# We prepend the full directory path to it - don't change this.
SYSDIR=`dirname $0`/$SYSDIR
# To see what it does:
# echo SYSDIR = $SYSDIR

# Run class_runner with the name of the main class
$SYSDIR/class_runner.sh $MAIN_CLASS $* $ARGS
