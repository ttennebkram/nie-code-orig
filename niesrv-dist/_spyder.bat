@echo off

REM Run the NIE Server.
REM This batch file sets up the correct java paths and
REM then starts the JVM.



REM use the ARGS variable to tell the spider what to do
REM ======================================================



REM This sample looks at the the search form on our home page
set ARGS=-summary -text -source http://ideaeng.com

REM set ARGS=-summary -text -source -list dev-doc/miles-spider-sites-june-04.txt -cache misc_non_cvs/page_cache -vendor
REM set ARGS=-list dev-doc/newsletter-domains-june-04.txt -cache misc_non_cvs/page_cache -vendor
REM http://www.aopa.org/search.shtml
REM http://www.xilinx.com/company/search.htm
REM http://www.appliedbiosystems.com
REM http://www.cypress.com/srch_adv_all.cfm
REM http://www.ideaeng.com/search/index.html
REM set ARGS=-vendor_filter verity http://www.ideaeng.com/search/index.html http://www.aopa.org/search.shtml http://www.xilinx.com/company/search.htm http://www.appliedbiosystems.com
REM set ARGS=-form -source http://www.aopa.org/search.shtml
REM set ARGS=-summary -config -source http://www.appliedbiosystems.com
REM set ARGS=-summary -text -source http://www.appliedbiosystems.com
REM set ARGS=-list dev-doc/newsletter-domains-short.txt -cache misc_non_cvs/page_cache
REM set ARGS=-list dev-doc/newsletter-domains-short.txt -vendor_filter verity
REM set ARGS=-list dev-doc/newsletter-domains-short.txt
REM set ARGS=-list dev-doc/newsletter-domains.txt -vendor_filter verity
REM set ARGS=-list dev-doc/newsletter-domains.txt -summary -vendor
REM set ARGS=-list dev-doc/newsletter-bad-sites.txt -vendor_filter verity
REM set ARGS=-summary -source http://www.xilinx.com/company/search.htm
REM set ARGS=-config -summary -source http://www.xilinx.com
REM set ARGS=-config -summary -source http://www.aetna.com/sitesearch/search.html
REM set ARGS=-config -summary file:///d:/data/proj/niecode/search.html



REM The main Java class to run
REM =========================================
REM set MAIN_CLASS=nie.sn.SearchTuningApp
set MAIN_CLASS=nie.spider.PageInfo
REM =========================================

REM Where we store our many system files, RELATIVE to this directory.
set SYSDIR=system
REM We prepend the full directory path to it - don't change this.
set SYSDIR=%~dp0%SYSDIR%
REM To see what it does:
REM echo SYSDIR = %SYSDIR%

REM Run class_runner with the name of the main class
echo Sending normal output to p.out, errors will show up here
call "%SYSDIR%\class_runner.bat" %MAIN_CLASS% %ARGS% %* > p.out

REM Uncomment this next line if your batch file exits with an
REM error but the window disappears too quickly to read it
REM pause
