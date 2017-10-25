#!/bin/bash
docker-compose down
docker-compose pull
docker-compose up -d gui api mongodb
sleep 10
if [ "$(uname)" == "Darwin" ]; then
    open http://localhost:3000
    echo "browser opend on localhost:3000!"
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    xdg-open http://localhost:3000
    echo "browser opend on localhost:3000!"
elif [ "$(expr substr $(uname -s) 1 10)" == "MINGW64_NT" ]; then
    start http://localhost:3000
    echo "browser opend on localhost:3000!"
else
    echo "you will have to open your browser yourself !"
fi
java -jar cos-30018-runnable-withAPI.jar
