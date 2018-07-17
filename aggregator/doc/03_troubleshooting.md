# A3. Troubleshooting

This section lists the most common pitfalls. If this does not help you, please open a new [issue.](https://github.com/hhucn/eden/issues)

## D-BAS / Web container not found / could not be started
Sometimes, when the container is started for the first time after the build stage, the `dbas` distribution can not be found by python.  
This error can be solved by executing the following command, while the `aggregator_dbas` container is running:
```
docker exec aggregator_dbas_1 ./build_assets.sh
```

If the container is not running you can try to start the container on its own for the purpose of running the command:
```
docker run aggregator_dbas ./build_assets.sh
```

## Could not bind port XXXX
This error usually means, that one of the ports that are usually used by EDEN is already bound on your machine. The ports EDEN uses per default are:

- 4284 - The port of D-BAS web view
- 5222 - Used by D-BAS Notifications
- 5432 - The default port of the DBAS database
- 5601 - Kibana interface to control elasticsearch
- 5671 - RabbitMQ debugging
- 5672 - RabbitMQ debugging
- 7777 - A Clojure REPL
- 8888 - Ring Development Server
- 9200 - The elasticsearch service
- 15672 - RabbitMQ management console

The ports exposed by EDEN are defined in the `docker-compose.yml` file. You can try to change which ports are used for the mappings, but this may lead to side effects with some of the services.
