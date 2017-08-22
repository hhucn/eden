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
http://localhost:8080 with the user `groot` and the password `iamgroot`. This
can be configured in the [.env](.env)-file.

### aggregator

Clojure application containing the code to publish a new statement via the
message broker.

Exposes the port 7777 of the REPL so you can directly connect to the REPL in the
container.

### subscriber

Clojure test-application which acts as a consumer for RabbitMQ. You can read the
messages from a specified queue via this service. Just for testing purposes.
