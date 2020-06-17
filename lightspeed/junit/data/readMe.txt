This directory (junit/data) may be used for data files required by JUnit test cases.

Contents:
	test_script.pdf : used by test.script.PdfUtilsTest

--------------------------------------------------------------------------

NOTES ON Running JUnit from command line ...

See {eclipse workspace}/lightspeed29/junit directory with bat file, 
- extra jar files required by JUnit test are in lightspeed29/junit/lib. (javax.http... and hamcrest)

Note that the JUnit tests will use your eclipse project classes in the lightspeed.jar file, NOT the class files in WEB-INF/classes.
Before running the JUnit tests, you should ensure that the lightspeed.jar file is current. You can either enable the "makeJar" builder  in the eclipse project, or, from the command line, run "ant -f makejar.xml" in the project directory.

---------------

Create lightspeed-test.jar as follows, using makeTestJar.xml, which is an Ant build file.
In a command window (from the MyEclipse project directory) do:

> cd {eclipse workspace}/lightspeed29
> ant -f makeTestJar.xml

...which will create lightspeed-test.jar in the junit/lib directory.

This makes the test classes readily available to JUnit testing, without including them in lightspeed.jar.

---------------

run the JUnit tests as follows:
	cd {eclipse workspace}/lightspeed29/junit
	runJunit

---------------
The .bat file has:
	set CLASSPATH=lib/*;lib;../WebRoot/WEB-INF;../WebRoot/WEB-INF/lib/*;../WebRoot;

classpath purpose notes:

	lib/*;
		-- so extra jars needed are found
	lib;
		-- so log4j.properties in lib is found/used
	../WebRoot/WEB-INF;
		-- so applicationContextHibernate.xml is found
	../WebRoot/WEB-INF/lib/*;
		-- so all the usual jar files are found
	../WebRoot;
		-- so reportstemplate/script.jasper can be found.

---------------
