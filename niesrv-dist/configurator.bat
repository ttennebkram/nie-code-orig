@echo off

REM Run the NIE Server.
REM This batch file sets up the correct java paths and
REM then starts the JVM.

REM The main Java class to run
REM =========================================
REM set MAIN_CLASS=nie.sn.SearchTuningApp
set MAIN_CLASS=nie.config_ui.Configurator2
REM You can get the older tabbed configurator
REM with _configurator_v1.bat
REM =========================================

REM Where we store our many system files, RELATIVE to this directory.
set SYSDIR=system
REM We prepend the full directory path to it - don't change this.
set SYSDIR=%~dp0%SYSDIR%
REM To see what it does:
REM echo SYSDIR = %SYSDIR%

REM Run class_runner with the name of the main class
call "%SYSDIR%\class_runner.bat" %MAIN_CLASS% %*

REM Uncomment this next line if your batch file exits with an
REM error but the window disappears too quickly to read it
REM pause
