# Aggregator


## Setup

As usual, you only need one command:

    docker-compose up --build


## Services

### broker -- RabbitMQ

Currently we are using RabbitMQ. Starting the container exposes some ports where
you can access the message broker. But this is only for debugging purposes,
since you can directly connect to RabbitMQ when you are inside a container.

After you started the containers, you can access the management console at
http://localhost:15672/ with the user `groot` and the password `iamgroot`. This
user can be configured in the [.env](.env)-file.

### aggregator

Clojure application containing the code to publish a new statement via the
message broker.

Exposes the port 7777 of the REPL so you can directly connect to the REPL in the
container.

### query

The core module of the repository, which coordinates the flow of information. All
internal and external calls for arguments and other information flow trough this
module which coordinates the cache usage and retrieves the information if needed.
Retrieval works as a tiered process:
1. Cache
2. Local DB
3. External Queries and Search

The query also uses the pub/sub system to trigger publishes of new information and
subscriptions to updates to aquired information from external sources.

Dev-Status: 90%


## Testing

