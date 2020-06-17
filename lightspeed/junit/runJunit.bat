@echo on

REM run as follows:
REM   cd /lightspeed29/junit
REM   runJunit

set CLASSPATH=lib/*;lib;../WebRoot/WEB-INF;../WebRoot/WEB-INF/lib/*;../WebRoot;

date /t
time /t

java org.junit.runner.JUnitCore com.lightspeedeps.test.payroll.CheckLoadRulesTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.payroll.ExtraFilterParserTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.payroll.MpvRuleTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.payroll.NtPremiumRuleTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.payroll.OvertimeRuleTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.payroll.StartFormUpdate
java org.junit.runner.JUnitCore com.lightspeedeps.test.payroll.TimecardCreatorTest

REM still in development:
REM java org.junit.runner.JUnitCore com.lightspeedeps.test.payroll.WeeklyRuleTest

java org.junit.runner.JUnitCore com.lightspeedeps.test.payroll.WorkDayNumberTest

java org.junit.runner.JUnitCore com.lightspeedeps.test.report.DatabaseCheckTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.report.EventLogSummaryTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.report.FailedLogonJobTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.report.OverdueReportTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.report.StripboardReportGenTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.script.ConvertPageLengthTest

REM not configured as JUnit test:
REM java org.junit.runner.JUnitCore com.lightspeedeps.test.script.ImportFileTest

java org.junit.runner.JUnitCore com.lightspeedeps.test.script.PdfReadTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.script.PdfUtilsTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.script.ScriptEmailTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.script.ScriptImportTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.script.ScriptPagesTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.script.SynopsisTest

java org.junit.runner.JUnitCore com.lightspeedeps.test.util.EmailValidatorTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.util.MobileDeviceDetectionTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.util.PhoneNumberValidatorTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.util.SunriseSunsetTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.util.SunriseSunsetTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.util.TimeConverterTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.util.UtilitiesTest

java org.junit.runner.JUnitCore com.lightspeedeps.test.CheckoutNotificationTest
java org.junit.runner.JUnitCore com.lightspeedeps.test.PermissionTest

REM ** END of JUnit tests **
:end
time /t
