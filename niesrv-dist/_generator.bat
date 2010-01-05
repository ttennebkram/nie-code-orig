@echo off

REM Populate the database with simulated search activity
REM based on data from a spreadsheet

REM This batch file sets up the correct java paths and
REM then starts the JVM.

REM The main Java class to run
REM ====================================
set MAIN_CLASS=nie.sr2.util.Populator
REM ====================================

REM Where we store our many system files, RELATIVE to this directory.
set SYSDIR=system
REM We prepend the full directory path to it - don't change this.
set SYSDIR=%~dp0%SYSDIR%
REM To see what it does:
REM echo %SYSDIR%

REM Run class_runner with the name of the main class
"%SYSDIR%\class_runner.bat" %MAIN_CLASS% %*
