package org.acme.order.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.order.service.model.OrderEvent;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@ApplicationScoped
public class OrderEventPublisher {

   @Inject
   @Channel("orders-created")
   Emitter<OrderEvent> eventEmitter;

   public void publishOrderCreated(OrderEvent event) {
      eventEmitter.send(event);
   }
}
