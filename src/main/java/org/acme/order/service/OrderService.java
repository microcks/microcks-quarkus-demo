package org.acme.order.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

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
 * OrderService is responsible for checking business rules/constraints on Orders.
 * @author laurent
 */
@ApplicationScoped
public class OrderService {

   // This is a dumb implementation of an event sourcing repository. Don't use this in production!
   private final Map<String, List<OrderEvent>> orderEventsRepository = new HashMap<>();

   @Inject
   @RestClient
   PastryAPIClient pastryRepository;

   @Inject
   OrderEventPublisher eventPublisher;

   /**
    * This method will check that an Order can be actually placed and persisted. A full implementation
    * will probably check stocks, customer loyalty, payment methods, shipping details, etc... For sake
    * of simplicity, we'll just check that products (here pastries) are all available.
    * @param info The order information.
    * @return A created Order with incoming info, new unique identifier and created status.
    * @throws UnavailablePastryException
    * @throws Exception
    */
   public Order placeOrder(OrderInfo info) throws UnavailablePastryException, Exception {
      // For all products in order, check the availability calling the Pastry API.
      var availabilityFutures = info.productQuantities().stream()
          .map(ProductQuantity::productName)
          .collect(Collectors.toMap(this::checkPastryAvailability, Function.identity()));

      // Wait for all completable future to finish.
      CompletableFuture.allOf(availabilityFutures.keySet().toArray(new CompletableFuture[0])).join();

      try {
         // If one pastry is marked as unavailable, throw a business exception.
         for (CompletableFuture<Boolean> availabilityFuture : availabilityFutures.keySet()) {
            if (!availabilityFuture.get()) {
               String pastryName = availabilityFutures.get(availabilityFuture);
               throw new UnavailablePastryException(pastryName, "Pastry " + pastryName + " is not available");
            }
         }
      } catch (InterruptedException | ExecutionException e) {
         throw new Exception("Unexpected exception: " + e.getMessage());
      }

      // Everything is available! Create (and probably persist ;-) a new order.
      Order result = new Order();
      result.setCustomerId(info.customerId());
      result.setProductQuantities(info.productQuantities());
      //result.setProductQuantities(List.of(new ProductQuantity("Millefeuille", 1)));
      result.setTotalPrice(info.totalPrice());

      // Persist and publish creation event.
      OrderEvent orderCreated = new OrderEvent(System.currentTimeMillis(), result, "Creation");
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
      List<OrderEvent> orderEvents = orderEventsRepository.get(id);
      if (orderEvents != null) {
         return orderEvents.get(orderEvents.size() - 1).order();
      }
      throw new OrderNotFoundException(id);
   }

   /**
    *
    * @param id
    * @return
    * @throws OrderNotFoundException
    */
   public List<OrderEvent> getOrderEvents(String id) throws OrderNotFoundException {
      List<OrderEvent> orderEvents = orderEventsRepository.get(id);
      if (orderEvents == null) {
         throw new OrderNotFoundException(id);
      }
      return orderEvents;
   }

   private CompletableFuture<Boolean> checkPastryAvailability(String pastryName) {
      return CompletableFuture.supplyAsync(() -> {
             Log.infof("Checking pastry availability for pastry %s", pastryName);
             return "available".equals(pastryRepository.getPastry(pastryName).status());
          })
          .exceptionally(e -> {
             Log.errorf(e, "Got exception from Pastry client: %s", e.getMessage());
             return false;
          });
   }

   private void persistOrderEvent(OrderEvent event) {
      List<OrderEvent> orderEvents = orderEventsRepository.get(event.order().getId());
      if (orderEvents == null) {
         orderEvents = new ArrayList<>();
      }
      orderEvents.add(event);
      orderEventsRepository.put(event.order().getId(), orderEvents);
   }
}
