@echo off
REM This is called from the main seachnames.bat file
REM echo Before: %NIE_CLASS_PATH%
set BEFORE_BIT=%1
REM Remove any included quotation marks with the tilde command
set NEW_BIT=%~1
if not "%NEW_BIT%"=="" goto DOIT
set NEW_BIT=%1

:DOIT
REM echo Adding %NEW_BIT%
set NIE_CLASS_PATH=%NIE_CLASS_PATH%;%NEW_BIT%
REM echo After: %NIE_CLASS_PATH%
