# C1. Replacing Modules

EDEN allows the replacement of the four main modules described in  ["B01. Modular Design"](b01_modular_design.html). Following are some guidelines which interfaces need to be adhered to.

## DGEP
D-BAS as the default DGEP can be replaced by any other DGEP as long as the corresponding Clojure modules are modified as well. These are the modules concerned:  

### Database Listener
If your DGEP maintains its own database which is not the same as the EDEN database, it is recommended to add a listener, which listenes for all changes in said database. Those changes should be mirrored to the EDEN database at all times.
The `main` function in `aggregator.core` by default starts the listener via the `start-listeners` method in `aggregator.utils.pg-listener`. You are called to change the module as you see fit and start your listener in the `main` function. 

### DGEP API endpoint
The database listener is a passive connection. It is also recommended to have an active connection to actively pull data from the DGEP. The default module for D-BAS is `aggregator.graphql.dbas-connector` and utilizes the graphQL API of D-BAS. You should at least implement functions to retrieve all statements and relations and guarantee that both are called in order to initially update EDENs database in the `main` function. (Currently done by the private `bootstrap-dgep-data` function)

## Interface
The Interface does not have to adhere to specific standards as it reads and writes to EDEN at its own leasure. You can use any interface, as long as EDENs data can be consumed by it.

## Database
The default database used by EDEN is elasticsearch. You can of course use any other database. In that case you need to replace the `aggregator.query.db` module with an connector for your database. The functions that need to be present are all public functions of the ["default module"](aggregator.query.db.html) excluding `part-uri` and `unpack-elastic`. We recommend to use the namespace of `aggregator.query.db` for your module to minimize bugs. If you wish to change the name you need to update the module imports of the `aggregator.query.query` module as well. 


## Aggregator
The modifications of the aggregator can be split into multiple parts, which you also can replace separately. The public methods that need to be present, are all found in the code documentation.

### The Query Module
The query module is responsible for delivering data that EDEN posesses. Especially the `aggregator.query.query` module should be made to be an abstraction layer for the database module.  

`aggregator.query.update` is the module that concerns itself with updating the database with data that is received by the query module. The `update-statement` and `update-link` methods must be present.  

All other parts of the `aggregator.query` module are optional.

### Broker
The default broker used is RabbitMQ. It should currently not be switched out, as we did not yet develop a safe procedure to guarantee that any other module will work properly with the brokers of foreign instances.
