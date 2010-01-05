#!/bin/sh

# Run the NIE Server.
# This shell script sets up the correct java paths and
# then starts the JVM.

# The main Java class to run
# ====================================
MAIN_CLASS=nie.config_ui.Configurator
# ====================================

# Where we store our many system files, RELATIVE to this directory.
SYSDIR=system
# We prepend the full directory path to it - don't change this.
SYSDIR=`dirname $0`/$SYSDIR
# To see what it does:
# echo SYSDIR = $SYSDIR

# Run class_runner with the name of the main class
$SYSDIR/class_runner.sh $MAIN_CLASS $*
