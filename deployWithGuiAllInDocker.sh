#!/bin/bash
docker-compose down
docker-compose pull
docker-compose up -d gui api mongodb
sleep 10
if [ "$(uname)" == "Darwin" ]; then
    open http://localhost
    echo "browser opend on localhost!"
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    xdg-open http://localhost
    echo "browser opend on localhost!"
elif [ "$(expr substr $(uname -s) 1 10)" == "MINGW64_NT" ]; then
    start http://localhost
    echo "browser opend on localhost!"
else
    echo "you will have to open your browser yourself !"
fi
docker-compose up -d jade
docker-compose logs -ft jade