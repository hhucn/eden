#!/bin/bash

echo ":: Stop and remove old containers..."
docker stop portainer || true
docker-compose -f docker-compose.set2.yml down
docker-compose down

echo
echo ":: Pulling latest images..."
docker-compose pull
docker-compose -f docker-compose.set2.yml pull

echo
echo ":: Starting first set..."
docker-compose up -d

echo
echo ":: Starting second set..."
docker-compose -f docker-compose.set2.yml up -d

echo
echo ":: Starting portainer..."
docker run --name portainer --rm -d -p 9000:9000 -v /var/run/docker.sock:/var/run/docker.sock -v /opt/portainer:/data portainer/portainer

echo
echo ":: Check the status at http://$(hostname):9000"
