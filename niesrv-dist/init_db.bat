@echo off

if "%~1"=="" goto SYNTAX

set ARGS="%~1" -check_table -all -create_if_missing
REM set ARGS="%~1" -check_table nie_log


REM The main Java class to run
REM ====================================
set MAIN_CLASS=nie.core.DBConfig
REM ====================================

REM Where we store our many system files, RELATIVE to this directory.
set SYSDIR=system
REM We prepend the full directory path to it - don't change this.
set SYSDIR=%~dp0%SYSDIR%
REM To see what it does:
REM echo SYSDIR = %SYSDIR%

REM Run class_runner with the name of the main class
%SYSDIR%\class_runner.bat %MAIN_CLASS% %ARGS%
REM %SYSDIR%\class_runner_dev.bat %MAIN_CLASS% %ARGS%

:SYNTAX
echo.
echo Utility to setup NIE tables in database.
echo You will need to have a database config file handy.
echo.
echo SYNTAX:
echo %0 db_config_path.xml
