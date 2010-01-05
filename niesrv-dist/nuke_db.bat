@echo off

if "%~1"=="" goto SYNTAX

REM Extra args to init_db to make it remove the old data
set ARGS=-check_tables -all -create_if_missing -overwrite_tables -load_data -overwrite_data

REM Run it
"%~dp0\init_db.bat" %* %ARGS%
REM won't return here since we are not doing a CALL



:SYNTAX
echo.
echo Utility to blow away your SearchTrack database tables.
echo BE CAREFUL!!!
echo You will need to have a database config file handy, or your
echo main configuration file.
echo.
echo SYNTAX:
echo %0 db_config_path.xml
