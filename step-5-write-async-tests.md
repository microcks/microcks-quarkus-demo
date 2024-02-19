# Step 5: Let's write tests for the Async Events

Now that we address the REST/Synchronous part, let's have a look on the part related to Asynchronous Kafka events.
Testing of asynchronous or event-driven system is usually a pain for developers 🥲

## First Test - Verify our OrderService is publishing events

In this section, we'll focus on testing the `Order Service` + `Event Publisher` components of our application:

![Event Publisher Test](./assets/test-order-event-publisher.png)

Even if it may be easy to check that the creation of an event object has been triggered with frameworks like [Mockito](https://site.mockito.org/)
or others, it's far more complicated to check that this event is correctly serialized, sent to a broker and valid
regarding an Event definition...

Fortunately, Microcks and TestContainers make this thing easy!

Let's review the test class `OrderServiceTests` under `src/test/java/org/acme/order/service` and the well-named `testEventIsPublishedWhenOrderIsCreated()`
method:

```java

```

The sequence diagram below details the test sequence. You'll see 2 parallel blocks being executed:
* One that corresponds to Microcks test - where it connects and listen for Kafka messages,
* One that corresponds to the `OrderService` invokation that is expected to trigger a message on Kafka.

```mermaid
sequenceDiagram
    par Launch Microcks test
      OrderServiceTests->>Microcks: testEndpointAsync()
      participant Microcks
      Note right of Microcks: Initialized at test startup
      Microcks->>Kafka: poll()
      Kafka-->>Microcks: messages
      Microcks-->Microcks: validate messages
    and Invoke OrderService
      OrderServiceTests->>+OrderService: placeOrder(OrderInfo)
      OrderService->>+OrderEventPublisher: publishEvent(OrderEvent)
      OrderEventPublisher->>Kafka: send("orders-created")
      OrderEventPublisher-->-OrderService: done
      OrderService-->-OrderServiceTests: Order
    end
    OrderServiceTests->>+Microcks: get()
    Note over OrderServiceTests,Microcks: After at most 2 seconds
    Microcks-->OrderServiceTests: TestResult
```

Because the test is a success, it means that Microcks has received an `OrderEvent` on the specified topic and has validated the message
conformance with the AsyncAPI contract or this event-driven architecture. So you're sure that all your Spring Boot configuration, Kafka JSON serializer
configuration and network communication are actually correct!

## Second Test - Verify our OrderEventListener is processing events

In this section, we'll focus on testing the `Event Consumer` + `Order Service` components of our application:

![Event Publisher Test](./assets/test-order-event-consumer.png)

The final thing we want to test here is that our `OrderEventListener` component is actually correctly configured for connecting to Kafka,
for consuming messages, for de-serializing them into correct Java objects and for triggering the processing on the `OrderService`.
That's a lot to do and can be quite complex! But things remain very simple with Microcks 😉

Let's review the test class `OrderEventListenerTests` under `src/test/java/org/acme/order/service` and the well-named `testEventIsConsumedAndProcessedByService()`
method:

```java

```

To fully understand this test, remember that as soon as you're launching the test, we start Kafka and Microcks containers and that Microcks
is immediately starting publishing mock messages on this broker. So this test actually starts with a waiting loop, just checking that the
messages produced by Microcks are correctly received and processed on the application side.

The important things to get in this test are:
* We're waiting at most 4 seconds here because the default publication frequency of Microcks mocks is 3 seconds (this can be configured as you want of course),
* Within each polling iteration, we're checking for the order with id `123-456-789` because these are the values defined within the `order-events-asyncapi.yaml` AsyncAPI contract examples
* If we retrieve this order and get the correct information from the service, it means that is has been received and correctly processed!
* If no message is found before the end of 4 seconds, the loop exits with a `ConditionTimeoutException` and we mark our test as failed.

The sequence diagram below details the test sequence. You'll see 3 parallel blocks being executed:
* The first corresponds to Microcks mocks - where it connects to Kafka, creates a topic and publishes sample messages each 3 seconds,
* The second one corresponds to the `ORderEventListener` invocation that should be triggered when a message is found in the topic,
* The third one corresponds to the actual test - where we check that the specified order has been found and processed by the `OrderService`.

```mermaid
sequenceDiagram
  par On test startup
    loop Each 3 seconds
      participant Microcks
      Note right of Microcks: Initialized at test startup
      Microcks->>Kafka: send(microcks-orders-reviewed")
    end
  and Listener execution
    OrderEventListener->>Kafka: poll()
    Kafka-->>OrderEventListener: messages
    OrderEventListener->>+OrderService: updateOrder()
    OrderService-->OrderService: update order status
    OrderService->>-OrderEventListener: done
  and Test execution
    Note over OrderService,OrderEventListenerTests: At most 4 seconds
    loop Each 400ms
      OrderEventListenerTests->>+OrderService: getOrder("123-456-789")
      OrderService-->-OrderEventListenerTests: order or throw OrderNotFoundException
      alt Order "123-456-789" found
        OrderEventListenerTests-->OrderEventListenerTests: assert and break;
      else Order "123-456-789" not found
        OrderEventListenerTests-->OrderEventListenerTests: continue;
      end
    end
    Note over OrderService,OrderEventListenerTests: If here, it means that we never received expected message
    OrderEventListenerTests-->OrderEventListenerTests: fail();
  end
```

You did it and succeed in writing integration tests for all your application component with minimum boilerplate code! 🤩

## Wrap-up

Thanks a lot for being through this quite long demonstration. We hope you learned new techniques for integration tests with both REST and Async/Event-driven APIs. Cheers! 🍻