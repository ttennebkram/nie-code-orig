@echo off

REM Setup Java and Run a Java Class
REM Copyright 2002 New Idea Engineering, Inc.

REM WARNING:
REM Do Not Run this directly.
REM It is to be called from other batch files.
REM First argument must be a runnable Java class name

REM This batch file sets up the correct java paths and
REM then starts the JVM.


REM Some variables we will use
REM ==================================

REM The main system direcctory, basically ./ of class_runner
REM Do this before issuing the shift command, which will lose 0
set SYSDIR=%~dp0
REM To see it:
REM echo SYSDIR = %SYSDIR%
REM For setting to another directory
REM set SYSDIR=system
REM set SYSDIR=%~dp0%SYSDIR%

REM The main Java class name / path to run
set MAIN_CLASS=%1
REM Remove from arguments
shift
REM Note that %* still has the old parameter, so only use %1 %2 %3 etc.


REM What Java Virtual Machine to use.
REM ==============================================
REM You can supply this yourself by setting JVM.
REM If this is not in your path, you may need to use
REM an absolute path.  JVM 1.3 or above please.
REM You can set this manually, set JVM to an absolute path
REM to java.exe before running this script.
REM Don't mess with it if they've set it
REM What we would normally use:
set DEFAULT_JVM=java
REM If it's already set, but is set to the default, skip any action
if "%JVM%"=="%DEFAULT_JVM%" goto AFTER_JVM
REM If it's NOT null, then it's theirs, so leave it alone
if not "%JVM%"=="" goto SKIP_JVM_CHANGE
REM OK, safe for us to set it to our default
set JVM=%DEFAULT_JVM%
REM For Example:
REM set JVM=d:\apps\jre\java.exe
goto AFTER_JVM
:SKIP_JVM_CHANGE
echo Using USER SUPPLIED JVM = "%JVM%"
:AFTER_JVM
REM echo JVM=%JVM%


REM Where we store our jar files, RELATIVE to the SYSTEM directory.
REM If you have trouble, you might try an absolute path.
set JAR_FILES=jar_files
REM We prepend the full directory path to it - don't change this.
REM set JAR_FILES=%SYSDIR%\%JAR_FILES%
REM in this case sysdir already has a trailing backslash
set JAR_FILES=%SYSDIR%%JAR_FILES%
REM echo %JAR_FILES%


REM How Much Memory we should Grab
REM ==============================================
REM You can supply this yourself by setting NIE_MAX_MEMORY
REM What we would normally use:
REM Java defaults this to 64m
REM Units in Megabytes
REM Do NOT specify any Units, we will add "m" for Megs
set DEFAULT_MAX_MEMORY=64
REM If it's already set, but is set to the default, skip any action
if "%NIE_MAX_MEMORY%"=="%DEFAULT_MAX_MEMORY%" goto AFTER_MAX_MEMORY
REM If it's NOT null, then it's theirs, so leave it alone
if not "%NIE_MAX_MEMORY%"=="" goto SKIP_MAX_MEMORY_CHANGE
REM OK, safe for us to set it to our default
set NIE_MAX_MEMORY=%DEFAULT_MAX_MEMORY%
goto AFTER_MAX_MEMORY
:SKIP_MAX_MEMORY_CHANGE
echo Using USER suppled NIE_MAX_MEMORY = "%NIE_MAX_MEMORY%"
echo Reminder: this unit is measured in Megabytes
echo Do NOT add your own m or k suffix
:AFTER_MAX_MEMORY
REM echo NIE_MAX_MEMORY=%NIE_MAX_MEMORY%


REM Setup The Java Class Path
REM =========================

REM This builds a new class path for us to pass on the command line
REM We want all *.jar and *.zip files in the system jars directory.

REM Clear it first, in case it had old values
set NIE_CLASS_PATH=

REM For each file...
for %%f in ("%JAR_FILES%\*.jar" "%JAR_FILES%\*.zip") do call "%SYSDIR%\append_path.bat" "%%f"

REM echo %NIE_CLASS_PATH%


REM Run the JVM and main Application
REM ================================

echo Running application...
REM %JVM% -cp %NIE_CLASS_PATH%;%CLASSPATH% %MAIN_CLASS% %*
REM "%JVM%" -cp "%NIE_CLASS_PATH%;%CLASSPATH%" %MAIN_CLASS% %1 %2 %3 %4 %5 %6 %7 %8 %9
"%JVM%" -cp "%NIE_CLASS_PATH%;%CLASSPATH%" -Xmx%NIE_MAX_MEMORY%m %MAIN_CLASS% %1 %2 %3 %4 %5 %6 %7 %8 %9
if errorlevel 1 goto ERROR_JVM
goto DONE


REM Report Errors
REM =====================================

:ERROR_JVM
echo.
echo Error: The Java Virtual Machine returned an error code.
echo See the file troubleshooting.txt
REM exit 1
goto DONE


REM We're all done
:DONE

