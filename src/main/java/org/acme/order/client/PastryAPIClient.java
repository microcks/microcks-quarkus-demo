package org.acme.order.client;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import org.acme.order.client.model.Pastry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * PastryAPIClient is responsible for requesting the product/stock management system (aka the Pastry registry)
 * using its REST API. It should take care of serializing entities and Http params as required by the 3rd party API.
 * @author laurent
 */
@Path("/pastries")
@RegisterRestClient(configKey = "pastries")
public interface PastryAPIClient {

   @GET
   @Path("/{name}")
   Pastry getPastry(@PathParam("name") String name);

   @GET
   List<Pastry> listPastries(@QueryParam("size") String size);
}
