package org.acme.order.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.acme.order.service.model.Order;
import org.acme.order.service.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.core.ConditionTimeoutException;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@QuarkusTest
public class OrderEventListenerTests {

   @Inject
   OrderService service;

   @Test
   void testEventIsConsumedAndProcessedByService() throws Exception {
      try {
         await().atMost(8, TimeUnit.SECONDS)
               .pollDelay(400, TimeUnit.MILLISECONDS)
               .pollInterval(400, TimeUnit.MILLISECONDS)
               .until(() -> {
                  try {
                     Order order = service.getOrder("123-456-789");
                     assertEquals("lbroudoux", order.getCustomerId());
                     assertEquals(OrderStatus.VALIDATED, order.getStatus());
                     assertEquals(2, order.getProductQuantities().size());
                     return true;
                  } catch (OrderNotFoundException onfe) {
                     // Continue until ConditionTimeoutException.
                  }
                  return false;
               });
      } catch (ConditionTimeoutException timeoutException) {
         fail("The expected Order was not received/processed in expected delay");
      }
   }
}
