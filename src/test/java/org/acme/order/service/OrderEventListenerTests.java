package org.acme.order.service;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.acme.order.service.model.Order;
import org.acme.order.service.model.OrderStatus;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class OrderEventListenerTests {

   @Inject
   OrderService service;

   @Test
   void testEventIsConsumedAndProcessedByService() {
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
