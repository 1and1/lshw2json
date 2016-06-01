# lshw2json

Simple conversion from a LSHW XML input file to LSHW JSON output file.

LSHW is a Unix tool that dumps information about the hardware components that are
in a computer. LSHW itself has the possiblity to output XML files and also JSON files. 
When converting, this software tries to imitate the layout of the JSON files.
The software does NOT aim to produce 100% identical outputs to the original
LSHW JSON format.

## Why this software?

LSHW can output XML and JSON files. Yes. It happens that we only had XML files and I needed to have the JSON formats
for a project.

## Building

Building is done using Apache Maven as a build tool.

    $ mvn clean package

## Execution

After building you can run the program in the command
line similar to the following example:

    $ java -jar target/lshw2json-1.0-SNAPSHOT-jar-with-dependencies.jar  /tmp/lshw.xml  /tmp/lshw.j
