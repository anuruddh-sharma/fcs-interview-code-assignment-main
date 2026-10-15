package com.fulfilment.application.monolith.fulfilment;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class FulfilmentExceptionMapper implements ExceptionMapper<FulfilmentException> {

  @Override
  public Response toResponse(FulfilmentException exception) {
    return Response.status(exception.getStatus())
        .type(MediaType.APPLICATION_JSON)
        .entity(Map.of("error", exception.getMessage(), "code", exception.getStatus()))
        .build();
  }
}
