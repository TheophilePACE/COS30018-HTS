# COS30018-HTS
Intelligent agents to choose electricity provider smartly

## Java library Dependencies (included in jar)

JAVA JADE Plateform :
http://jade.tilab.com/download/jade/ 

JSON ORG : 
http://mvnrepository.com/artifact/org.json/json/20170516 

SUPERCSV :
https://github.com/super-csv/super-csv/releases/download/v2.4.0/super-csv-distribution-2.4.0-bin.zip

## Dependencies 

You must have Java, Docker and docker-compose to run the system.

## Run the system

To run the system, use the deployWithGuiAndSniffer.sh script. Go to the Executable directory, and paste “./deployWithGuiAndSniffer.sh” in your console.

Other options:
To run only the Jade Agent system: to have the Jade Agent system with Jade built-in UI but no GUI, run deployWithoutGui.sh
To run the whole system in Docker containers (useful if you don’t have Java), run deployWithGuiAllInDocker.sh. 
