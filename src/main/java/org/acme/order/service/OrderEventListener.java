package org.acme.order.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.order.service.model.OrderEvent;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class OrderEventListener {

   private final OrderService orderService;

   public OrderEventListener(OrderService orderService) {
      this.orderService = orderService;
   }

   @Incoming("orders-reviewed")
   public void handleReviewedOrder(OrderEvent event) {
      orderService.updateReviewedOrder(event);
   }
}
