package com.fulfilment.application.monolith.fulfilment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("fulfilment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FulfilmentResource {

  @Inject FulfilmentService fulfilmentService;

  @Inject FulfilmentRepository fulfilmentRepository;

  @GET
  public List<FulfilmentAssignment> list(@QueryParam("storeId") Long storeId) {
    return fulfilmentRepository.listByStore(storeId);
  }

  @POST
  public Response create(FulfilmentRequest request) {
    if (request == null) {
      throw new FulfilmentException(400, "Request body is required");
    }
    FulfilmentAssignment created =
        fulfilmentService.associate(request.storeId, request.productId, request.warehouseId);
    return Response.status(Response.Status.CREATED).entity(created).build();
  }

  @DELETE
  @Path("{id}")
  public Response delete(@PathParam("id") Long id) {
    fulfilmentService.remove(id);
    return Response.status(Response.Status.NO_CONTENT).build();
  }
}
