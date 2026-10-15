package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException.Kind;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class WarehouseExceptionMapper implements ExceptionMapper<WarehouseException> {

  @Override
  public Response toResponse(WarehouseException exception) {
    int status = exception.getKind() == Kind.NOT_FOUND ? 404 : 400;
    return Response.status(status)
        .type(MediaType.APPLICATION_JSON)
        .entity(Map.of("error", exception.getMessage(), "code", status))
        .build();
  }
}
