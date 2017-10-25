#!/bin/bash
docker-compose down
docker-compose pull
docker-compose up -d
docker-compose logs -ft jade