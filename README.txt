INSTALLATION INSTRUCTIONS FOR KARMA

Requirements: Maven 3.0 and above, Java 1.6

1. Maven is used as the build system for Karma. It also provides the Jetty server that hosts Karma on a machine. It can be downloaded from http://maven.apache.org/download.html. Clear installation steps for Maven are provided inside its installation zip package. Make sure that the bin directory of Maven is added to your PATH on a Windows machine.

2. Open the command line and run "mvn --version" to verify that Maven is correctly installed.

3. On command line, change your current directory to the directory in which Karma is installed. 

4. Compile the Java code by running "mvn compile" on command line inside the top level directory of Karma installation. If you get Java compilation errors related to annotations such as "@override", that means you probably have Java 1.5 (You need Java 1.6 for Karma). When the command is run for the first time on a machine, it might take some time as it downloads all the required Java dependencies from Internet.

5. Run "mvn jetty:run" command inside the same directory to start the Jetty server. Again, when the command is run for the first time, it might take some time to download the required Jetty server files from Internet.

6. Once the server has started successfully, point your browser to http://localhost:8080/web-karma.html to start using Karma. 

7. Press Ctrl+C to stop the server when required, and "mvn jetty:run" to start the server again (inside the installation directory).

****************************************************************************************************

To run the jetty server, execute the following command from webkarma top directory:
	mvn jetty:run
Point your browser to http://localhost:8080/web-karma.html

NOTE: To start it on a port other than 8080 (e.g. Port number 9999): mvn -Djetty.port=9999 jetty:run

To start in logging mode (where all the logs are stored in the log folder), use the following command to start the server:
	mvn -Dslf4j=false -Dlog4j.configuration=file:./config/log4j.properties jetty:run

*** To set up password protection ***
- in /config/jettyrealm.properties change user/password (if you wish)
- in /src/main/webapp/WEB-INF/web.xml uncomment security section at the end of the file
- in pom.xml uncomment security section (search for loginServices)

*** Offline RDF Generation from a database ***
1. Model your source and publish it's model.
2. From the command line, go to the top level Karma directory and run the following command:
	mvn exec:java -Dexec.mainClass="edu.isi.karma.rdf.OfflineDbRdfGenerator" -Dexec.args="[PATH TO YOUR MODEL FILE] [OUTPUT RDF FILE NAME/PATH] [DATABASE PASSWORD]"

	e.g. mvn exec:java -Dexec.mainClass="edu.isi.karma.rdf.OfflineDbRdfGenerator" -Dexec.args="ObjCurLocView.n3 result.n3 secretPassword"
	Above command will use the ObjCurLocView.n3 model file to pubish a RDF file named result.n3

NOTE: In Maven Jetty plugin based Karma deployment, the published models are located at src/main/webapp/repository/sources/ inside the Karma directory.