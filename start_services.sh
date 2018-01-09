#!/bin/bash

echo ":: Pulling latest images..."
docker-compose pull
docker-compose -f docker-compose.set2.yml pull

echo ":: Starting first set..."
docker-compose up -d

echo ":: Starting second set..."
docker-compose -f docker-compose.set2.yml up -d

echo ":: Starting portainer..."
docker run --rm -d -p 9000:9000 -v /var/run/docker.sock:/var/run/docker.sock -v /opt/portainer:/data portainer/portainer

echo ":: Check the status at http://$(hostname):9000"
