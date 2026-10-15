package com.fulfilment.application.monolith.warehouses.domain.exceptions;

public class WarehouseException extends RuntimeException {

  public enum Kind {
    NOT_FOUND,
    VALIDATION
  }

  private final Kind kind;

  public WarehouseException(Kind kind, String message) {
    super(message);
    this.kind = kind;
  }

  public Kind getKind() {
    return kind;
  }
}
