@echo off

set NIE_PRODUCT_NAME=NIE Server

REM Setup Java and the NIE Server to run as a Windows Service
REM Copyright 2002, 2003, 2004 New Idea Engineering, Inc.

REM This batch file sets up the correct java paths and
REM then starts the JVM.


REM Syntax Checking
REM ==================================
if "%~1"=="-help" goto EXTENDED_SYNTAX
if "%~1"=="--help" goto EXTENDED_SYNTAX
if "%~1"=="-h" goto EXTENDED_SYNTAX
if "%~1"=="/help" goto EXTENDED_SYNTAX
if "%~1"=="/h" goto EXTENDED_SYNTAX
if "%~1"=="/?" goto EXTENDED_SYNTAX
if "%~1"=="-?" goto EXTENDED_SYNTAX

if "%~1"=="" goto SYNTAX
if "%~2"=="" goto SYNTAX


REM What Java Virtual Machine to use.
REM ==============================================
REM You can supply this yourself by setting JVM.
REM If this is not in your path, you may need to use
REM an absolute path.  JVM 1.3 or above please.
REM You can set this manually, set JVM to an absolute path
REM to java.exe
REM Don't mess with it if they've set it
set DEFAULT_JVM=java
if "%JVM%"=="%DEFAULT_JVM%" goto AFTER_JVM
if not "%JVM%"=="" goto SKIP_JVM_CHANGE
set JVM=%DEFAULT_JVM%
REM For Example:
REM set JVM=d:\apps\jre\java.exe
goto AFTER_JVM
:SKIP_JVM_CHANGE
echo Using USER SUPPLIED JVM = "%JVM%"
:AFTER_JVM
REM Save this for later, niesrv.bat will overwrite it.
set NTS_JVM=%JVM%

REM What Port to use to MONITOR THE SERVICE
REM this is DIFFERENT than the normal NIE Server PORT
set DEFAULT_MONITOR_PORT=1777
REM Here we're setting if if they haven't set it.
REM And if we're using their setting, we tell them.
REM And if the utility was run before, the port will be set, but
REM probably to it's default, so no point in telling them that.
if "%MONITOR_PORT%"=="%DEFAULT_MONITOR_PORT%" goto AFTER_PORT
if not "%MONITOR_PORT%"=="" goto SKIP_PORT
set MONITOR_PORT=%DEFAULT_MONITOR_PORT%
goto AFTER_PORT
:SKIP_PORT
echo Using USER SUPPLIED MONITOR_PORT = "%MONITOR_PORT%"
:AFTER_PORT


REM Try Running Server with -check_config
REM ==========================================
REM set SN_BAT=searchnames.bat
set SN_BAT=niesrv.bat
REM Prepend the full path
set SN_BAT="%~dp0%SN_BAT%"

REM The name of the configuration file
set SN_CONFIG=%~f2

REM The name of the service, without any quotes
set NTS_SHORT_NAME=%~1


if "%3"=="-delete" goto SETUP_VARIABLES


echo.
echo ========================================================================
echo Attention:
echo.
echo Before registering or running this service, we are going to test
echo the configuration.
echo You will see some log messages scroll by while the system checks
echo the configuration - this is normal (assuming there are no errors).
echo.
echo You will actually see it run twice.
echo - The first test checks the general configuration.
echo - The second test checks that run logging is configured correctly.
echo ========================================================================
pause
echo.

set SN_CMD=%SN_BAT% -check_config %SN_CONFIG%
REM echo %SN_CMD%
call %SN_CMD%

if errorlevel 1 goto ERROR_SEARCHNAMES

REM Restore the JVM we had from before
set JVM=%NTS_JVM%


REM Now we do some checking on their configuration, in regards
REM to run log specification.
REM ==========================================================
set SN_CMD_RUNCHECK=%SN_BAT% -quiet -check_runlog_path %SN_CONFIG%
call %SN_CMD_RUNCHECK%
REM error gt or eq 4 = General problem
if errorlevel 4 goto ERROR_RUNLOG_GENERIC
REM error 3 = Problem with not being absolute
if errorlevel 3 goto ERROR_RUNLOG_NOT_ABSOLUTE
REM error 2 = Not present at all!
if errorlevel 2 goto ERROR_NO_RUNLOG
REM error lt or eq 1 = General problem
if errorlevel 1 goto ERROR_RUNLOG_GENERIC
REM Restore the JVM we had from before
set JVM=%NTS_JVM%


REM It's OK!
echo.
echo ========================================================================
echo Congratulations:
echo.
echo The configuration seemed to be correct.
echo.
if "%3"=="-console" goto CONS_MSG1
echo We will now proceed to register your service.
echo Service Name = %NTS_SHORT_NAME%
goto END_MSG2
:CONS_MSG1
echo We will now proceed to run your configuration in this console window.
:END_MSG2
echo Configuration File = %SN_CONFIG%
echo ========================================================================
pause

:SETUP_VARIABLES
REM Some variables we will use
REM ==================================

REM Where to find System Files
REM ========================================
set SYSDIR=system
REM Prepend the full path
set SYSDIR=%~dp0%SYSDIR%
REM To see it:
REM echo SYSDIR = %SYSDIR%

REM The main Java class name / path to run
REM ========================================
REM set MAIN_CLASS=nie.sn.SearchNamesApp
set MAIN_CLASS=nie.sn.SearchTuningApp
REM ========================================



REM Some Derived Variables
REM ========================================


REM Where we store our jar files, RELATIVE to the SYSTEM directory.
REM If you have trouble, you might try an absolute path.
set JAR_FILES=jar_files
REM We prepend the full directory path to it - don't change this.
set JAR_FILES=%SYSDIR%\%JAR_FILES%
REM echo %JAR_FILES%

REM Where we store NT Service Binaries, relative to the System directory
set NTS_DIR=nt_service
REM We prepend the full directory path to it - don't change this.
set NTS_DIR=%SYSDIR%\%NTS_DIR%
REM echo %NTS_DIR%

REM Where we store NT Service configuration files
REM relative to the main NTS directory
set NTS_CONF_DIR=config
REM We prepend the full directory path to it - don't change this.
set NTS_CONF_DIR=%NTS_DIR%\%NTS_CONF_DIR%
REM echo %NTS_CONF_DIR%

REM And the main template file we'll use
set NTS_CONF_TEMPLATE=template_header.nts
REM We prepend the full directory path to it - don't change this.
set NTS_CONF_TEMPLATE="%NTS_CONF_DIR%\%NTS_CONF_TEMPLATE%"
REM echo %NTS_CONF_TEMPLATE%

REM And the config file that we will create here
set NTS_CONF_FILE=service_%NTS_SHORT_NAME%.nts
REM We prepend the full directory path to it - don't change this.
set NTS_CONF_FILE="%NTS_CONF_DIR%\%NTS_CONF_FILE%"
REM echo %NTS_CONF_FILE%

REM Where we store NT Service related binaries
set NTS_BIN=%NTS_DIR%
set NTS_LIB=%NTS_DIR%

REM Their JNI DLL
set NTS_DLL=Wrapper.dll
REM Prepend the full path, don't edit this
set NTS_DLL=%NTS_BIN%\%NTS_DLL%

REM And their compiled service executable
set NTS_EXE=Wrapper.exe
REM Prepend the full path, don't edit this
REM And also surround with quotes, to protect from spaces in path
set NTS_EXE="%NTS_BIN%\%NTS_EXE%"

REM And where to log
set NTS_LOG_DIR=logs
REM Make it a full path
set NTS_LOG_DIR=%SYSDIR%\%NTS_LOG_DIR%
REM The name of the file
set NTS_LOG_FILE=nts_wrapper.log
REM Make it a full path
set NTS_LOG_FILE=%NTS_LOG_DIR%\%NTS_LOG_FILE%

REM And where to store the PID
set NTS_PID_FILE=nts_wrapper.pid
REM Make it a full path
set NTS_PID_FILE=%NTS_LOG_DIR%\%NTS_PID_FILE%


REM Some more variables for NT / Windows Service registry entries
REM Variable NTS_SHORT_NAME is set above
REM set NTS_DESCRIPTION=%NIE_PRODUCT_NAME% "%NTS_SHORT_NAME%" from config file %~n2%~x2
REM Want the full path to config - when you look in Services control panel
REM it's nice to be told exactly which of the possibly similarly named
REM files on your hard drive that this service points to.
set NTS_DESCRIPTION=%NIE_PRODUCT_NAME% "%NTS_SHORT_NAME%" from config file "%SN_CONFIG%"
REM ^^^ something like ''NIE Server "name" from config file "d:\...\my_config_9010.xml"''

REM Setup The Java Class Path
REM =========================

REM This builds a new class path for us to pass on the command line
REM We want all *.jar and *.zip files in the system jars directory.

REM Clear it first, in case it had old values
set NIE_CLASS_PATH=

REM For each file...
for %%f in (%JAR_FILES%\*.jar %JAR_FILES%\*.zip) do call %SYSDIR%\append_path.bat %%f


goto SKIP_DEBUG

echo NTS_SHORT_NAME = "%NTS_SHORT_NAME%"
echo NTS_DESCRIPTION = "%NTS_DESCRIPTION%"
echo SN_CONFIG = "%SN_CONFIG%"
echo JVM = "%JVM%"
echo MONITOR_PORT = "%MONITOR_PORT%"
echo MAIN_CLASS = "%MAIN_CLASS%"
echo NTS_LIB = "%NTS_LIB%"
echo NTS_LOG_FILE = "%NTS_LOG_FILE%"
echo NTS_PID_FILE = "%NTS_PID_FILE%"

echo NTS_EXE = "%NTS_EXE%"
echo NTS_CONF_TEMPLATE = "%NTS_CONF_TEMPLATE%"
echo NTS_CONF_FILE = "%NTS_CONF_FILE%"

echo NIE_CLASS_PATH = "%NIE_CLASS_PATH%"

:SKIP_DEBUG


REM Now Build the real NTS config file with literal values
REM ========================================================

REM Start with a fresh copy
if not exist %NTS_CONF_FILE% goto SKIP_DEL
del %NTS_CONF_FILE%
:SKIP_DEL
REM copy %NTS_CONF_TEMPLATE% %NTS_CONF_FILE%
REM set CMD=xcopy /Q/Y %NTS_CONF_TEMPLATE% %NTS_CONF_FILE%
REM echo %CMD%
type %NTS_CONF_TEMPLATE% > %NTS_CONF_FILE%
if errorlevel 1 goto ERROR_CREATING_NTS_CONF

REM Add the literal values
REM do NOT put a space after the closing % and the double arrows

echo wrapper.port=%MONITOR_PORT%>> %NTS_CONF_FILE%
if errorlevel 1 goto ERROR_CREATING_NTS_CONF
echo wrapper.java.command=%JVM%>> %NTS_CONF_FILE%
if errorlevel 1 goto ERROR_CREATING_NTS_CONF
echo wrapper.app.parameter.1=%MAIN_CLASS%>> %NTS_CONF_FILE%
if errorlevel 1 goto ERROR_CREATING_NTS_CONF
echo wrapper.app.parameter.2=%SN_CONFIG%>> %NTS_CONF_FILE%
if errorlevel 1 goto ERROR_CREATING_NTS_CONF
echo wrapper.java.library.path.1=%NTS_LIB%>> %NTS_CONF_FILE%
if errorlevel 1 goto ERROR_CREATING_NTS_CONF
echo wrapper.logfile=%NTS_LOG_FILE%>> %NTS_CONF_FILE%
if errorlevel 1 goto ERROR_CREATING_NTS_CONF
echo wrapper.pidfile=%NTS_PID_FILE%>> %NTS_CONF_FILE%
if errorlevel 1 goto ERROR_CREATING_NTS_CONF
echo wrapper.ntservice.name=%NTS_SHORT_NAME%>> %NTS_CONF_FILE%
if errorlevel 1 goto ERROR_CREATING_NTS_CONF
echo wrapper.ntservice.displayname=%NTS_SHORT_NAME%>> %NTS_CONF_FILE%
if errorlevel 1 goto ERROR_CREATING_NTS_CONF
echo wrapper.ntservice.description=%NTS_DESCRIPTION%>> %NTS_CONF_FILE%
if errorlevel 1 goto ERROR_CREATING_NTS_CONF
echo wrapper.java.classpath.1=%NIE_CLASS_PATH%>> %NTS_CONF_FILE%
if errorlevel 1 goto ERROR_CREATING_NTS_CONF

REM echo Created %NTS_CONF_FILE%
REM pause


REM Now Do Something!
REM ==================================================


REM Goto the Console Mode if needed
if "%3"=="-console" goto RUN_CONSOLE
if "%3"=="-delete" goto DELETE

REM Run the Registration Utility
REM ================================
set CMD=%NTS_EXE% -i %NTS_CONF_FILE%
REM echo CMD=
REM echo %CMD%
echo Registering service...
%CMD%
if errorlevel 1 goto ERROR_REGISTERING_FIRST_TRY

echo.
echo ============================================================================
echo Registration Worked!
echo.
echo Will now start the service.
echo.
echo To SKIP this step, and LEAVE the service STOPPED, press Control-C and then Y
echo ============================================================================
pause
goto START_IT



:ERROR_REGISTERING_FIRST_TRY
if not "%3"=="-overwrite" goto ERROR_REGISTERING_NO_OVERWRITE


:DELETE
REM Run the Registration Utility a second time, to try and remove the service
REM =========================================================================
set CMD2=%NTS_EXE% -r %NTS_CONF_FILE%
REM echo CMD2=
REM echo %CMD2%
REM echo.
echo Attempting to UN-register existing service...
echo.
%CMD2%
if errorlevel 1 goto ERROR_UNREGISTERING
if "%3"=="-delete" goto DONE_DELETE

REM Run the utility a 3rd time, to now attempt to re-register the service
echo.
echo Attempting to RE-register with the NEW configuration...
echo.

%CMD%
if errorlevel 1 goto ERROR_RE_REGISTERING

echo.
echo ==========================================================================
echo RE-registration Worked!
echo.
echo Will now start the service.
echo.
echo To skip this step, and leave service Stopped, Press Control-C and then Y
echo ==========================================================================
pause
goto START_IT


:DONE_DELETE
echo.
echo ========================================================================
echo Deletion of Service Worked!
echo ========================================================================
goto DONE



:RUN_CONSOLE
REM Run the Service from the Console
REM ================================
set MY_ARGS=wrapper.debug=true
set CMD=%NTS_EXE% -c %NTS_CONF_FILE% %MY_ARGS%
echo.
echo ========================================================================
echo Running Console command:
echo %CMD%
echo ========================================================================
echo.
%CMD%
if errorlevel 1 goto ERROR_CONSOLE
goto DONE


:START_IT
REM Start up the service!!!
REM =========================================================================
set START_CMD=net start "%NTS_SHORT_NAME%"
%START_CMD%
if errorlevel 1 goto ERROR_STARTING
echo.
echo ========================================================================
echo Service Started!
echo.
echo Your service %NTS_SHORT_NAME% has been started.
echo.
echo PLEASE REMEMBER this name, %NTS_SHORT_NAME%, as you will
echo need to know the name later when you want to control the service.
echo.
echo You can start and stop this service from your Service Manager
echo in the Control Panel.  You can also change it's configuration.
echo.
echo You can also start and stop the service from the command line with:
echo net start "%NTS_SHORT_NAME%"
echo.   and
echo net stop "%NTS_SHORT_NAME%"
echo.
echo If the name has no SPACES in it you can omit the quotes.
echo.
echo This utility is finished.  Your service is now registered and running.
echo ========================================================================
goto DONE


REM Report Errors
REM =====================================

:ERROR_SEARCHNAMES
echo.
echo ========================================================================
echo Configuration Error:
echo.
echo The configuration appears to have an error.
echo Your service will NOT be registered with Windows.
echo Please correct the error and run this utility again.
echo ========================================================================
goto DONE


:ERROR_CREATING_NTS_CONF
echo.
echo ========================================================================
echo Setup Error:
echo.
echo Unable to create the Windows Service Control File
echo for this process; these are the *.nts files.
echo.
echo Please check out the files:
echo %NTS_CONF_TEMPLATE%
echo.	and
echo %NTS_CONF_FILE%
echo.
echo Please correct any problems, such as permissions, and try again.
echo ========================================================================
goto DONE


:ERROR_REGISTERING_NO_OVERWRITE
echo.
echo ========================================================================
echo Windows Service Registration Error:
echo.
echo It appears that the service was NOT registered.
echo.
echo If it failed because the service has been previously registered
echo then you can add a third command line option:
echo -overwrite
echo this will tell the system to remove existing service with the
echo same name.  BE CAREFUL!
echo ========================================================================
goto DONE


:ERROR_UNREGISTERING
echo.
echo ========================================================================
echo Error Unregistering:
echo.
echo -overwrite or -delete was set on the comnand line but was
echo unable to de-register the old copy of this service.
echo.
echo If you see a message saying something like service is "marked for deletion"
echo and if you have your Services Control Panel open, you might close that
echo and try re-running this utility.  Sometimes services can't be replaced
echo while that control panel is open.
echo ========================================================================
goto DONE

:ERROR_RE_REGISTERING
echo.
echo ========================================================================
echo Error Re-Registering:
echo.
echo -overwrite was set on the comnand line
echo and the old copy of the service was removed.
echo.
echo However, was unable to re-register the new copy of this service.
echo ========================================================================
goto DONE

:ERROR_CONSOLE
echo.
echo ========================================================================
echo Error Running:
echo.
echo -console was set on the comnand line so this was run from the console.
echo The service wrapper returned an error.
echo ========================================================================
goto DONE

:ERROR_STARTING
echo.
echo ========================================================================
echo Error Starting Service:
echo.
echo Got an error starting your service %NTS_SHORT_NAME%
echo The service did not start properly.
echo Please correct the error and try again.
echo ========================================================================
goto DONE

:ERROR_RUNLOG_GENERIC
:ERROR_NO_RUNLOG
echo.
echo ========================================================================
echo Error In Configuration:
echo.
echo A generic error was returned when checking the run log configuration.
echo Please check log messages to the screen and your log file.
goto ERROR_RUNLOG_SYNTAX

:ERROR_NO_RUNLOG
echo.
echo ========================================================================
echo Error In Configuration:
echo.
echo When running %NIE_PRODUCT_NAME% as a Windows Service, you MUST
echo specify a location for runtime log output.
echo.
echo When a program runs as a service, there is no "standard error" to
echo send errors and warnings to; by specifying an alternate location
echo to log messages to, you can read the output of the program.
echo You must tell us where to send these messages in your configuration file.
goto ERROR_RUNLOG_SYNTAX

:ERROR_RUNLOG_NOT_ABSOLUTE
echo.
echo ========================================================================
echo Error In Configuration:
echo.
echo The configured run log location is not absolute.
echo.
echo Although you have specified a location for run time logging,
echo the file name you gave us does not appear to be absolute.
echo We require an absolute path when running as a Windows serivce.
echo.
echo Please correct the location and make sure it starts with
echo a drive letter, colon and backslash.
goto ERROR_RUNLOG_SYNTAX

:ERROR_RUNLOG_SYNTAX
echo.
echo Here is an example of a proper run log configuration:
echo.
echo your-config.xml
echo ---------------
echo ^<nie_config^>
echo.	^<run_logging
echo.		location="D:\apps\niesrv\logs\my-server.log"
echo.		verbosity="default"
echo.	/^>
echo.	... rest of config file ...
echo ^</nie_config^>
echo.
echo Note that the location must be an ABSOLUTE path, starting with a drive
echo letter, then a colon, then a backslash; you must not use relative paths.
echo ========================================================================
goto DONE


:SYNTAX
echo.
echo %0 : Utility to register the NIE Server as a Windows Service
echo Syntax:
echo %0 [-help]
echo.	OR
echo %0 service_name server_config_file.xml
echo.		[-overwrite, -delete, -console]
echo Where:
echo service_name is how you will refer to the service in
echo.	Windows Service Control Panel, and
echo sn_config_file.xml is the path name to your XML
echo.	configuration file for the NIE Server.
echo.
echo Important Notes about the Service Name:
echo.	We suggest that you NOT use spaces or weird characters in the name.
echo.	The name should just be letters, numbers and the underscore _
echo.	For example, use a name like main_web_server
echo.	And please REMEMBER what you call it.
REM echo.	Also, if you run more than one instance of the NIE Search Server
REM echo.	on your server, give each of them a descriptive name
REM echo.	and be sure to set MONITOR_PORT for each one (see below)
echo.
echo -overwrite = Overwrite any existing service with this name
echo.	such as from a previous run of this utility.
echo.	Useful if you haved edited the configuration.
echo -delete = Delete any existing service with this name
echo.	BE VERY CAREFUL WITH THIS OPTION.
echo.	Check your Service Manager to make sure you have the right name.
echo -console = Run from the console.  Useful for testing and debugging.
echo.
echo OPTIONAL Enviroment Variables:
echo.	JVM = FULL path to your Java executable; this may fix some problems.
echo.	MONITOR_PORT = nnnn, the Windows Service Monitor Port
echo.		This is only needed if you are running MULTIPLE NIE servers
echo.		as MULTIPLE Windows Services.
echo.		This is NOT the same port as your NIE Server port.

goto DONE

:EXTENDED_SYNTAX
REM Nothing else to say right now
goto SYNTAX


REM We're all done
:DONE
echo.
