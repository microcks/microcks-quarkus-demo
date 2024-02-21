# Step 3: Local development experience with Microcks

Our application uses Kafka and external dependencies.

At first, if you run the application from your terminal without integrating Microcks, you would see the following traces:

```shell
./mvnw quarkus:dev 

[INFO] Scanning for projects...
[INFO] 
[INFO] -----------------------< org.acme:order-service >-----------------------
[INFO] Building order-service 0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- quarkus:3.2.3.Final:dev (default-cli) @ order-service ---
[INFO] Invoking resources:3.3.1:resources (default-resources) @ order-service
[INFO] Copying 5 resources from src/main/resources to target/classes
[INFO] Invoking quarkus:3.2.3.Final:generate-code (default) @ order-service
[INFO] Invoking compiler:3.11.0:compile (default-compile) @ order-service
[INFO] Nothing to compile - all classes are up to date
[INFO] Invoking resources:3.3.1:testResources (default-testResources) @ order-service
[INFO] Copying 5 resources from src/test/resources to target/test-classes
[INFO] Invoking quarkus:3.2.3.Final:generate-code-tests (default) @ order-service
[INFO] Invoking compiler:3.11.0:testCompile (default-testCompile) @ order-service
[INFO] Nothing to compile - all classes are up to date
Listening for transport dt_socket at address: 5005




2024-02-21 10:10:39,513 INFO  [io.qua.sma.dep.processor] (build-58) Generating Jackson deserializer for type org.acme.order.service.model.OrderEvent
2024-02-21 10:10:39,517 INFO  [io.qua.sma.dep.processor] (build-58) Generating Jackson serializer for type org.acme.order.service.model.OrderEvent
2024-02-21 10:10:41,905 INFO  [io.qua.kaf.cli.dep.DevServicesKafkaProcessor] (build-4) Dev Services for Kafka started. Other Quarkus applications in dev mode will find the broker automatically. For Quarkus applications in production mode, you can connect to this by starting your application with -Dkafka.bootstrap.servers=OUTSIDE://localhost:32902
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2024-02-21 10:10:42,271 WARN  [io.qua.config] (Quarkus Main Thread) Unrecognized configuration key "quarkus.microcks.devservices.enabled" was provided; it will be ignored; verify that the dependency extension for this configuration is set or that you did not make a typo

2024-02-21 10:10:42,271 WARN  [io.qua.config] (Quarkus Main Thread) Unrecognized configuration key "quarkus.microcks.devservices.image-name" was provided; it will be ignored; verify that the dependency extension for this configuration is set or that you did not make a typo
2024-02-21 10:10:42,450 INFO  [io.sma.rea.mes.kafka] (Quarkus Main Thread) SRMSG18229: Configured topics for channel 'orders-reviewed': [OrderEventsAPI-0.1.0-orders-reviewed]
2024-02-21 10:10:42,455 INFO  [io.sma.rea.mes.kafka] (Quarkus Main Thread) SRMSG18214: Key deserializer omitted, using String as default
2024-02-21 10:10:42,677 INFO  [io.sma.rea.mes.kafka] (smallrye-kafka-producer-thread-0) SRMSG18258: Kafka producer kafka-producer-orders-created, connected to Kafka brokers 'OUTSIDE://localhost:32902', is configured to write records to 'orders-created'
2024-02-21 10:10:42,694 INFO  [io.sma.rea.mes.kafka] (smallrye-kafka-consumer-thread-0) SRMSG18257: Kafka consumer kafka-consumer-orders-reviewed, connected to Kafka brokers 'OUTSIDE://localhost:32902', belongs to the 'order-service' consumer group and is configured to poll records from [OrderEventsAPI-0.1.0-orders-reviewed]
2024-02-21 10:10:42,734 INFO  [io.quarkus] (Quarkus Main Thread) order-service 0.0.1-SNAPSHOT on JVM (powered by Quarkus 3.2.3.Final) started in 3.845s. Listening on: http://localhost:8080
2024-02-21 10:10:42,734 INFO  [io.quarkus] (Quarkus Main Thread) Profile dev activated. Live Coding activated.
2024-02-21 10:10:42,735 INFO  [io.quarkus] (Quarkus Main Thread) Installed features: [cdi, kafka-client, rest-client-reactive, rest-client-reactive-jackson, resteasy-reactive, resteasy-reactive-jackson, smallrye-context-propagation, smallrye-reactive-messaging, smallrye-reactive-messaging-kafka, vertx]

--
Tests paused
Press [e] to edit command line args (currently ''), [r] to resume testing, [o] Toggle test output, [:] for the terminal, [h] for more options>
```

You can see that [Dev Services for Kafka](https://quarkus.io/guides/kafka-dev-services) are started automatically because we included a Kafka client
into our project dependencies. However, there's no such default Dev Services when using the Rest client to call our external Pastry API provider!

Just issue a simple API request using CURL or Postman or any of your favourite HTTP Client tools. 

```shell
curl -XPOST localhost:8080/api/orders -H 'Content-type: application/json' \                                                                                                                                                                                                                      ─╯
    -d '{"customerId": "lbroudoux", "productQuantities": [{"productName": "Eclair Cafe", "quantity": 1}], "totalPrice": 4.1}'
```

You should get a response similar to the following:

```shell
< HTTP/1.1 500 Internal Server Error
< content-length: 0
< 
* Connection #0 to host localhost left intact
```

and on the terminal where application is running, something like this:

```shell
2024-02-21 10:12:49,863 ERROR [org.acm.ord.api.OrderResource] (executor-thread-1) Unexpected runtime exception: Error injecting org.acme.order.client.PastryAPIClient org.acme.order.service.OrderService.pastryRepository
2024-02-21 10:12:49,863 ERROR [org.acm.ord.api.OrderResource] (executor-thread-1) Root cause: Unable to determine the proper baseUrl/baseUri. Consider registering using @RegisterRestClient(baseUri="someuri"), @RegisterRestClient(configKey="orkey"), or by adding 'quarkus.rest-client."org.acme.order.client.PastryAPIClient".url' or 'quarkus.rest-client."org.acme.order.client.PastryAPIClient".uri' to your Quarkus configuration
```

To run this application locally, we need Microcks Dev Services 🪄 to have our Pastry API mocked!

In order to see what's needed to run this, you may check the `pom.xml` and `application.properties` files.


## Review pom.xml and application.properties

First, the `quarkus-microcks` dependency must be added to your project dependencies like illustrated below:

```xml
<dependency>
  <groupId>io.github.microcks.quarkus</groupId>
  <artifactId>quarkus-microcks</artifactId>
  <version>${quarkus-microcks.version}</version>
  <scope>provided</scope>
</dependency>
```

The dependency scope can be set to `provided` as this dependency will only be used during development mode or unit tests. You don't need to package them into
your application.

In order to wire the application to Microcks, we also need to update the `src/main/resources/application.properties` file so that we specify to Microcks
provided endpoints for both the Pastry API REST endpoint and the Kafka topic for receiving reviewed orders:

```properties
quarkus.rest-client."org.acme.order.client.PastryAPIClient".url=${quarkus.microcks.default.http}/rest/API+Pastries/0.0.1

mp.messaging.incoming.orders-reviewed.topic=OrderEventsAPI-0.1.0-orders-reviewed
```

You'll notice that we use `${quarkus.microcks.default.http}` expression to specify the dynamically assigned Microcks HTTP endpoint.

## Application startup with Microcks DevServices

Now that we've got everything configured, we can start again the application. In the traces below, we only kept the significant parts related to Microcks DevServices usage:

```shell
./mvnw quarkus:dev 

[INFO] Scanning for projects...
[INFO] 
[INFO] -----------------------< org.acme:order-service >-----------------------
[INFO] Building order-service 0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[...]
2024-02-21 10:43:07,849 INFO  [io.qua.kaf.cli.dep.DevServicesKafkaProcessor] (build-29) Dev Services for Kafka started. Other Quarkus applications in dev mode will find the broker automatically. For Quarkus applications in production mode, you can connect to this by starting your application with -Dkafka.bootstrap.servers=PLAINTEXT://kafka-8fvsj:29092,OUTSIDE://localhost:32909
2024-02-21 10:43:12,091 INFO  [io.git.mic.qua.dep.DevServicesMicrocksProcessor] (build-54) The 'default' microcks container is ready on http://localhost:61705
2024-02-21 10:43:12,093 INFO  [tc.qua.io/microcks/microcks-postman-runtime:latest] (build-104) Creating container for image: quay.io/microcks/microcks-postman-runtime:latest
2024-02-21 10:43:12,130 INFO  [tc.qua.io/microcks/microcks-postman-runtime:latest] (build-104) Container quay.io/microcks/microcks-postman-runtime:latest is starting: 23599df5ad1028f1146c4bd51985333d367fc4c686c51c832c58c26020038fa0
2024-02-21 10:43:12,663 INFO  [tc.qua.io/microcks/microcks-postman-runtime:latest] (build-104) Container quay.io/microcks/microcks-postman-runtime:latest started in PT0.570199S
2024-02-21 10:43:12,665 INFO  [tc.qua.io/microcks/microcks-uber-async-minion:latest] (build-104) Creating container for image: quay.io/microcks/microcks-uber-async-minion:latest
2024-02-21 10:43:12,688 INFO  [tc.qua.io/microcks/microcks-uber-async-minion:latest] (build-104) Container quay.io/microcks/microcks-uber-async-minion:latest is starting: a014d70ed6cf9f1d3b3ee1464811bc2808100381d9991844f30a2e7b3b475548
2024-02-21 10:43:14,135 INFO  [tc.qua.io/microcks/microcks-uber-async-minion:latest] (build-104) Container quay.io/microcks/microcks-uber-async-minion:latest started in PT1.469394S
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2024-02-21 10:43:14,282 WARN  [io.qua.config] (Quarkus Main Thread) Unrecognized configuration key "quarkus.microcks.default.grpc.host" was provided; it will be ignored; verify that the dependency extension for this configuration is set or that you did not make a typo

2024-02-21 10:43:14,282 WARN  [io.qua.config] (Quarkus Main Thread) Unrecognized configuration key "quarkus.microcks.default.http.port" was provided; it will be ignored; verify that the dependency extension for this configuration is set or that you did not make a typo
2024-02-21 10:43:14,282 WARN  [io.qua.config] (Quarkus Main Thread) Unrecognized configuration key "quarkus.microcks.default.grpc" was provided; it will be ignored; verify that the dependency extension for this configuration is set or that you did not make a typo
2024-02-21 10:43:14,282 WARN  [io.qua.config] (Quarkus Main Thread) Unrecognized configuration key "quarkus.microcks.default.http" was provided; it will be ignored; verify that the dependency extension for this configuration is set or that you did not make a typo
2024-02-21 10:43:14,282 WARN  [io.qua.config] (Quarkus Main Thread) Unrecognized configuration key "quarkus.microcks.default.grpc.port" was provided; it will be ignored; verify that the dependency extension for this configuration is set or that you did not make a typo
2024-02-21 10:43:14,282 WARN  [io.qua.config] (Quarkus Main Thread) Unrecognized configuration key "quarkus.microcks.default.http.host" was provided; it will be ignored; verify that the dependency extension for this configuration is set or that you did not make a typo
2024-02-21 10:43:14,448 INFO  [io.sma.rea.mes.kafka] (Quarkus Main Thread) SRMSG18229: Configured topics for channel 'orders-reviewed': [OrderEventsAPI-0.1.0-orders-reviewed]
2024-02-21 10:43:14,452 INFO  [io.sma.rea.mes.kafka] (Quarkus Main Thread) SRMSG18214: Key deserializer omitted, using String as default
2024-02-21 10:43:14,560 WARN  [org.apa.kaf.cli.ClientUtils] (smallrye-kafka-consumer-thread-0) Couldn't resolve server PLAINTEXT://kafka-8fvsj:29092 from bootstrap.servers as DNS resolution failed for kafka-8fvsj
2024-02-21 10:43:14,603 WARN  [org.apa.kaf.cli.ClientUtils] (smallrye-kafka-producer-thread-0) Couldn't resolve server PLAINTEXT://kafka-8fvsj:29092 from bootstrap.servers as DNS resolution failed for kafka-8fvsj
2024-02-21 10:43:14,606 INFO  [io.sma.rea.mes.kafka] (smallrye-kafka-producer-thread-0) SRMSG18258: Kafka producer kafka-producer-orders-created, connected to Kafka brokers 'PLAINTEXT://kafka-8fvsj:29092,OUTSIDE://localhost:32909', is configured to write records to 'orders-created'
2024-02-21 10:43:14,622 INFO  [io.sma.rea.mes.kafka] (smallrye-kafka-consumer-thread-0) SRMSG18257: Kafka consumer kafka-consumer-orders-reviewed, connected to Kafka brokers 'PLAINTEXT://kafka-8fvsj:29092,OUTSIDE://localhost:32909', belongs to the 'order-service' consumer group and is configured to poll records from [OrderEventsAPI-0.1.0-orders-reviewed]
Microcks DevServices has been initialized
[...]
2024-02-21 10:43:21,170 INFO  [io.sma.rea.mes.kafka] (vert.x-eventloop-thread-3) SRMSG18256: Initialize record store for topic-partition 'OrderEventsAPI-0.1.0-orders-reviewed-0' at position -1.
2024-02-21 10:43:21,198 INFO  [org.acm.ord.ser.OrderEventListener] (vert.x-eventloop-thread-3) Receiving a reviewed order with id '123-456-789'
2024-02-21 10:43:23,115 INFO  [org.acm.ord.ser.OrderEventListener] (vert.x-eventloop-thread-3) Receiving a reviewed order with id '123-456-789'
[...]
```

Let's understand why we got those messages at startup:

* First, you can see that the `DevServicesKafkaProcess` output is a bit different... This time we have a `PLAINTEXT://kafka-8fvsj:29092` endpoint and this one will be used by Microcks DevServices.
* Second, you can see that the `DevServicesMicrocksProcessor` is starting a Microcks container that will be available at `http://localhost:61705`.
* Then, you can see that this processor is also starting a some other containers like `microcks-postman-runtime` and `microcks-uber-async-minion`.
  This is not always the case and depends on your project configuration. Those services have been started here because we have Postman Collection files in our resources and because we have a Kafka broker running.
* Just after the Quarkus logo, you can see that Microcks DevServices as also contributed a bunch of `quarkus.microcks.*` properties
* Finally, after the application has started, you'll see messages about `Receiving a reviewed order with id...` that means that Microcks has started producing mock messages and the application is processing them.

And that's it! 🎉 You don't need to download and install extra-things, or clone other repositories and figure out how to start your dependant services.

Now, you can invoke the APIs using CURL or Postman or any of your favourite HTTP Client tools.


## Create an order

```shell
curl -XPOST localhost:8080/api/orders -H 'Content-type: application/json' \                                                                                                                                      ─╯
    -d '{"customerId": "lbroudoux", "productQuantities": [{"productName": "Millefeuille", "quantity": 1}], "totalPrice": 5.1}'
```

You should get a response similar to the following:

```shell
< HTTP/1.1 201 
< Content-Type: application/json;charset=UTF-8
< content-length: 172
< 
* Connection #0 to host localhost left intact
{"id":"0ddb2ad0-a74d-4b1c-84bc-f272349ef091","status":"CREATED","customerId":"lbroudoux","productQuantities":[{"productName":"Millefeuille","quantity":1}],"totalPrice":5.1}%
```

Now test with something else, requesting for another Pastry:

```shell
curl -XPOST localhost:8080/api/orders -H 'Content-type: application/json' \
    -d '{"customerId": "lbroudoux", "productQuantities": [{"productName": "Eclair Chocolat", "quantity": 1}], "totalPrice": 4.1}' -v
```

This time you get another "exception" response:

```shell
< HTTP/1.1 422 Unprocessable Entity
< Content-Type: application/json;charset=UTF-8
< content-length: 85
< 
* Connection #0 to host localhost left intact
{"productName":"Eclair Chocolat","details":"Pastry Eclair Chocolat is not available"}%
```

and this is because Microcks has created different simulations for the Pastry API 3rd party API based on API artifacts we loaded.
Check the `src/test/resources/third-parties/apipastries-openapi.yaml` and `src/test/resources/third-parties/apipastries-postman-collection.json` files to get details.

### 
[Next](step-4-write-rest-tests.md)