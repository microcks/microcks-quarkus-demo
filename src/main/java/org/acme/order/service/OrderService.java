package org.acme.order.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.acme.order.client.PastryAPIClient;
import org.acme.order.service.model.Order;
import org.acme.order.service.model.OrderEvent;
import org.acme.order.service.model.OrderInfo;
import org.acme.order.service.model.ProductQuantity;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.logging.Log;

/**
 * OrderService is responsible for checking business rules/constraints on
 * Orders.
 * 
 * @author laurent
 */
@ApplicationScoped
public class OrderService {
   private record PastryAvailability(String pastryName, boolean available) {
   }

   // This is a dumb implementation of an event sourcing repository. Don't use this
   // in production!
   private final Map<String, List<OrderEvent>> orderEventsRepository = new HashMap<>();

   @Inject
   @RestClient
   PastryAPIClient pastryRepository;

   @Inject
   OrderEventPublisher eventPublisher;

   /**
    * This method will check that an Order can be actually placed and persisted. A
    * full implementation
    * will probably check stocks, customer loyalty, payment methods, shipping
    * details, etc... For sake
    * of simplicity, we'll just check that products (here pastries) are all
    * available.
    * 
    * @param info The order information.
    * @return A created Order with incoming info, new unique identifier and created
    *         status.
    * @throws UnavailablePastryException
    */
   public Order placeOrder(OrderInfo info) throws UnavailablePastryException {
      // For all products in order, check the availability calling the Pastry API.
      var availabilityFutures = info.productQuantities().stream()
            .map(ProductQuantity::productName)
            .map(this::checkPastryAvailability)
            .toList();

      // Wait for all completable futures to finish.
      CompletableFuture.allOf(availabilityFutures.toArray(new CompletableFuture[availabilityFutures.size()])).join();

      var unavailablePastry = availabilityFutures.stream()
            .map(this::getAvailability)
            .filter(availability -> !availability.available())
            .findAny();

      if (unavailablePastry.isPresent()) {
         var pastryName = unavailablePastry.get().pastryName();
         throw new UnavailablePastryException(pastryName, "Pastry %s is not available".formatted(pastryName));
      }

      // Everything is available! Create (and probably persist ;-) a new order.
      var result = new Order();
      result.setCustomerId(info.customerId());
      result.setProductQuantities(info.productQuantities());
      // result.setProductQuantities(List.of(new ProductQuantity("Millefeuille", 1)));
      result.setTotalPrice(info.totalPrice());

      // Persist and publish creation event.
      var orderCreated = new OrderEvent(System.currentTimeMillis(), result, "Creation");
      persistOrderEvent(orderCreated);
      eventPublisher.publishOrderCreated(orderCreated);

      return result;
   }

   /**
    *
    * @param reviewedOrderEvent
    */
   public void updateReviewedOrder(OrderEvent reviewedOrderEvent) {
      persistOrderEvent(reviewedOrderEvent);
   }

   /**
    *
    * @param id
    * @return
    */
   public Order getOrder(String id) throws OrderNotFoundException {
      var orderEvents = getOrderEvents(id);
      return orderEvents.get(orderEvents.size() - 1).order();
   }

   /**
    *
    * @param id
    * @return
    * @throws OrderNotFoundException
    */
   public List<OrderEvent> getOrderEvents(String id) throws OrderNotFoundException {
      return Optional.ofNullable(orderEventsRepository.get(id))
            .orElseThrow(() -> new OrderNotFoundException(id));
   }

   private CompletableFuture<PastryAvailability> checkPastryAvailability(String pastryName) {
      return CompletableFuture.supplyAsync(() -> {
         Log.infof("Checking pastry availability for pastry %s", pastryName);
         return new PastryAvailability(pastryName, "available".equals(pastryRepository.getPastry(pastryName).status()));
      })
            .exceptionally(e -> {
               Log.errorf(e, "Got exception from Pastry client: %s", e.getMessage());
               return new PastryAvailability(pastryName, false);
            });
   }

   private void persistOrderEvent(OrderEvent event) {
      var orderEvents = Optional.ofNullable(orderEventsRepository.get(event.order().getId()))
            .orElseGet(ArrayList::new);

      orderEvents.add(event);
      orderEventsRepository.put(event.order().getId(), orderEvents);
   }

   private PastryAvailability getAvailability(CompletableFuture<PastryAvailability> availabilityFuture) {
      try {
         return availabilityFuture.get();
      } catch (InterruptedException | ExecutionException e) {
         throw new RuntimeException("Unexpected exception: " + e.getMessage());
      }
   }
}
