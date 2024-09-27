package org.acme.order.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import jakarta.inject.Inject;

import org.acme.order.BaseTest;
import org.acme.order.service.model.Order;
import org.acme.order.service.model.OrderInfo;
import org.acme.order.service.model.ProductQuantity;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import io.github.microcks.quarkus.test.InjectKafkaInternalEndpoint;
import io.github.microcks.quarkus.test.MicrocksTestCompanion;
import io.github.microcks.testcontainers.MicrocksContainer;
import io.github.microcks.testcontainers.model.TestRequest;
import io.github.microcks.testcontainers.model.TestResult;
import io.github.microcks.testcontainers.model.TestRunnerType;

@QuarkusTest
@QuarkusTestResource(MicrocksTestCompanion.class)
public class OrderServiceTests extends BaseTest {

   @Inject
   OrderService service;

   @InjectKafkaInternalEndpoint
   String kafkaInternalEndpoint;

   @Test
   void testEventIsPublishedWhenOrderIsCreated() {
      // Prepare a Microcks test.
      TestRequest kafkaTest = new TestRequest.Builder()
            .serviceId("Order Events API:0.1.0")
            .filteredOperations(List.of("SUBSCRIBE orders-created"))
            .runnerType(TestRunnerType.ASYNC_API_SCHEMA.name())
            .testEndpoint("kafka://%s/orders-created".formatted(kafkaInternalEndpoint))
            .timeout(5000L)
            .build();

      // Prepare an application Order.
      OrderInfo info = new OrderInfo("123-456-789", List.of(
            new ProductQuantity("Millefeuille", 1),
            new ProductQuantity("Eclair Cafe", 1)
      ), 8.4);

      try {
         // Launch the Microcks test and wait a bit to be sure it actually connects to Kafka.
         // Because of Redpanda, it must be >3 sec to ensure the consumer get a refresh of metadata and actually receive messages.
         // Update: with Redpanda > 24, this is no longer needeed as metadata are refreshed on consumer creation.
         CompletableFuture<TestResult> testRequestFuture = MicrocksContainer.testEndpointAsync(microcksContainerUrl, kafkaTest);

         TimeUnit.MILLISECONDS.sleep(500L);

         // Invoke the application to create an order.
         Order createdOrder = service.placeOrder(info);

         // You may check additional stuff on createdOrder...

         // Get the Microcks test result.
         TestResult testResult = testRequestFuture.get();

         // Check success and that we read 1 valid message on the topic.
         assertTrue(testResult.isSuccess());
         assertFalse(testResult.getTestCaseResults().isEmpty());
         assertEquals(1, testResult.getTestCaseResults().get(0).getTestStepResults().size());
      } catch (Exception e) {
         fail("No exception should be thrown when testing Kafka publication", e);
      }
   }
}
