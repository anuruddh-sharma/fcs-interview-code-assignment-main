package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException.Kind;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    if (warehouse == null) {
      throw new WarehouseException(Kind.VALIDATION, "Warehouse is required");
    }
    if (isBlank(warehouse.businessUnitCode)) {
      throw new WarehouseException(Kind.VALIDATION, "Business unit code is required");
    }
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new WarehouseException(Kind.VALIDATION, "Business unit code already exists");
    }
    if (isBlank(warehouse.location)) {
      throw new WarehouseException(Kind.VALIDATION, "Location is required");
    }

    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new WarehouseException(Kind.VALIDATION, "Location is invalid");
    }
    if (warehouse.capacity == null || warehouse.capacity <= 0) {
      throw new WarehouseException(Kind.VALIDATION, "Capacity must be greater than zero");
    }
    if (warehouse.stock == null || warehouse.stock < 0) {
      throw new WarehouseException(Kind.VALIDATION, "Stock must be zero or greater");
    }
    if (warehouse.stock > warehouse.capacity) {
      throw new WarehouseException(
          Kind.VALIDATION, "Capacity cannot handle the informed stock");
    }

    List<Warehouse> activeAtLocation = warehouseStore.findActiveByLocation(warehouse.location);
    if (activeAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WarehouseException(
          Kind.VALIDATION, "Maximum number of warehouses reached for this location");
    }

    int usedCapacity =
        activeAtLocation.stream().mapToInt(existing -> existing.capacity == null ? 0 : existing.capacity).sum();
    if (usedCapacity + warehouse.capacity > location.maxCapacity) {
      throw new WarehouseException(
          Kind.VALIDATION, "Warehouse capacity exceeds the maximum capacity of the location");
    }

    warehouse.createdAt = LocalDateTime.now();
    warehouse.archivedAt = null;
    warehouseStore.create(warehouse);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
