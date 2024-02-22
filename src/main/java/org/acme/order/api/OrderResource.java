package org.acme.order.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.acme.order.service.OrderService;
import org.acme.order.service.UnavailablePastryException;
import org.acme.order.service.model.OrderInfo;
import org.acme.order.service.model.UnavailableProduct;

import io.quarkus.logging.Log;

/**
 * OrderResource is responsible for exposing the REST API for the Order Service. It should take
 * care of serialization, business rules mapping to model types and Http status codes.
 * @author laurent
 */
@Path("/api/orders")
public class OrderResource {
   @Inject
   OrderService service;

   @POST
   public Response order(OrderInfo info) {
      try {
         return Response.status(Status.CREATED)
             .entity(service.placeOrder(info))
             .build();
      } catch (UnavailablePastryException upe) {
         // We have to return a 422 (unprocessable) with correct expected type.
         return Response.status(422)
             .entity(new UnavailableProduct(upe.getProduct(), upe.getMessage()))
             .build();
      } catch (Exception e) {
         Log.errorf(e, "Unexpected runtime exception: %s", e.getMessage());
         return Response.serverError().build();
      }
   }
}
