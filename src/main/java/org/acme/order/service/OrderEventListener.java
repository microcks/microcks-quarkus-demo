package org.acme.order.service;

import jakarta.enterprise.context.ApplicationScoped;

import org.acme.order.service.model.OrderEvent;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.quarkus.logging.Log;

@ApplicationScoped
public class OrderEventListener {
   private final OrderService orderService;

   public OrderEventListener(OrderService orderService) {
      this.orderService = orderService;
   }

   @Incoming("orders-reviewed")
   public void handleReviewedOrder(OrderEvent event) {
      Log.infof("Receiving a reviewed order with id '%s' for '%s'", event.order().getId(), event.order().getCustomerId());
      orderService.updateReviewedOrder(event);
   }
}
