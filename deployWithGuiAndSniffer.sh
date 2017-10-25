#!/bin/bash
docker-compose down
docker-compose pull
docker-compose up -d gui api mongodb
sleep 10
java -jar cos-30018-runnable-withAPI.jar