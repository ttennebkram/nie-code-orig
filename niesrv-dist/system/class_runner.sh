#!/bin/sh

# Setup Java and Run a Java Class
# Copyright 2002 New Idea Engineering, Inc.

# WARNING:
# Do Not Run this directly.
# It is to be called from other batch files.
# First argument must be a runnable Java class name

# This shell script sets up the correct java paths and
# then starts the JVM.


# Some variables we will use
# ==================================

# The main system direcctory, basically ./ of class_runner
# Do this before issuing the shift command, which will lose 0
SYSDIR=`dirname $0`
# To see it:
# echo SYSDIR = $SYSDIR
# For setting to another directory
# SYSDIR=system
# SYSDIR=`dirname $0`/$SYSDIR

# The main Java class name / path to run
MAIN_CLASS=$1
# Remove from arguments
shift

# What Java Virtual Machine to use.
# If this is not in your path, you may need to use
# an absolute path.  JVM 1.3 or above please.
JVM=java
# For Example:
# JVM=/home/mbennett/java-bin/java

# Where we store our jar files, RELATIVE to the SYSTEM directory.
# If you have trouble, you might try an absolute path.
JAR_FILES=jar_files
# We prepend the full directory path to it - don't change this.
JAR_FILES=$SYSDIR/$JAR_FILES
# echo JAR_FILES = $JAR_FILES


# Setup The Java Class Path
# =========================

# This builds a new class path for us to pass on the command line
# We want all *.jar and *.zip files in the system jars directory.

# Clear it first, in case it had old values
NIE_CLASS_PATH=

# For each file...
for f in $JAR_FILES/*.jar $JAR_FILES/*.zip
do
	NIE_CLASS_PATH=$NIE_CLASS_PATH:$f
done

# echo NIE_CLASS_PATH=$NIE_CLASS_PATH


# Run the JVM and main Application
# ================================

echo Running application...
$JVM -cp $NIE_CLASS_PATH:$CLASSPATH $MAIN_CLASS $@


# Report Errors
# =====================================
# echo $?
if test $? != 0
then

echo .
echo Error: The Java Virtual Machine returned an error code.
echo See the file troubleshooting.txt

fi

# done
