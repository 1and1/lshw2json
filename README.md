# lshw2json

Simple conversion from a LSHW XML input file to LSHW JSON output file.

## Building

Building is done using Apache Maven as a build tool.

    $ mvn clean package

## Execution

After building you can run the program in the command
line similar to the following example:

    $ java -jar target/lshw2json-1.0-SNAPSHOT-jar-with-dependencies.jar  /tmp/lshw.xml  /tmp/lshw.j

