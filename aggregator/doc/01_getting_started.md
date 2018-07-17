# A1. Getting Started with EDEN

This is a quick guide aimed at somebody who quickly wants to launch an EDEN instance.  
The console commands shown here are designed to work on any Linux and Mac Shell.  
Although we did not test it it should by now also work on most modern Windows systems.  

## Downloading and Setup

You can find the source-code of EDEN at [github](https://github.com/hhucn/eden/tree/master).


**Prerequisites**
We provide EDEN inside multiple Docker containers. Please install `docker` and `docker-compose` 
on your System to properly run EDEN.


To download the whole package including D-BAS use: 
```
git clone --recursive git@github.com:hhucn/eden.git
```
*(Check out "[Using your own D-BAS instance](02_using_own_dbas.html)" if you already have a D-BAS instance and want to integrate it)*


## Installation
Switch to the EDEN folder you just cloned with
```
cd eden
```

Build the containers by executing:
```
docker-compose build
```

## Running EDEN
Once the containers are build you can start EDEN by executing
```
docker-compose up
```

You should now see a lot of debug information in your console which is normal.  
Wait until all services are started, which may take a minute or two.  
You should now be able to open `http://localhost:4284` in your browser to see a fresh D-BAS instance.

You can use `docker ps` to see if the containers are running. The following images should be up:


* aggregator_aggregator
* aggregator_dbas
* aggregator_dbas-db
* aggregator_search
* hhucn/dbas-notifications
* docker.elastic.co/kibana/kibana
* rabbitmq


Should any of the containers not start properly, consult the [troubleshooting-guide.](03_troubleshooting.html)

## Further Customization
The automatically used D-BAS configuration starts a minimal running instance. It is functional, but additional features like mail delivery, authentication through OAuth, etc. are not working. 
To activate them, customize the `dbas_development.env` by changing the placeholders in the corresponding places.
