@echo on

REM A short version of runJunit.bat, for testing classpath changes, etc.

REM run as follows:
REM   cd /lightspeed29/junit
REM   runJunitShort

set CLASSPATH=lib/*;lib;../WebRoot/WEB-INF;../WebRoot/WEB-INF/lib/*;../WebRoot;

time /t

java org.junit.runner.JUnitCore com.lightspeedeps.test.util.EmailValidatorTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.payroll.CheckLoadRulesTest

time /t
