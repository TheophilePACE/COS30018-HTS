FROM openjdk:alpine
COPY cos-30018-runnable-withAPI.jar .
CMD java -jar cos-30018-runnable-withAPI.jar
