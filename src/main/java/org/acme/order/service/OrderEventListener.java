package org.acme.order.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.order.service.model.OrderEvent;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrderEventListener {

   private static final Logger logger = Logger.getLogger(OrderEventListener.class);

   private final OrderService orderService;

   public OrderEventListener(OrderService orderService) {
      this.orderService = orderService;
   }

   @Incoming("orders-reviewed")
   public void handleReviewedOrder(OrderEvent event) {
      logger.infof("Receiving a reviewed order with id '%s'", event.order().getId());
      orderService.updateReviewedOrder(event);
   }
}
