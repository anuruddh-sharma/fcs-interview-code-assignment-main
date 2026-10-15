package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException.Kind;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import io.vertx.core.http.HttpServerResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  private final WarehouseStore warehouseStore;
  private final CreateWarehouseOperation createWarehouseOperation;
  private final ArchiveWarehouseOperation archiveWarehouseOperation;
  private final ReplaceWarehouseOperation replaceWarehouseOperation;

  @Inject HttpServerResponse httpServerResponse;

  @Inject
  public WarehouseResourceImpl(
      WarehouseStore warehouseStore,
      CreateWarehouseOperation createWarehouseOperation,
      ArchiveWarehouseOperation archiveWarehouseOperation,
      ReplaceWarehouseOperation replaceWarehouseOperation) {
    this.warehouseStore = warehouseStore;
    this.createWarehouseOperation = createWarehouseOperation;
    this.archiveWarehouseOperation = archiveWarehouseOperation;
    this.replaceWarehouseOperation = replaceWarehouseOperation;
  }

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseStore.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    var domain = toDomain(data);
    createWarehouseOperation.create(domain);
    httpServerResponse.setStatusCode(201);
    return toWarehouseResponse(domain);
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    var warehouse = warehouseStore.findById(parseId(id));
    if (warehouse == null) {
      throw new WarehouseException(Kind.NOT_FOUND, "Warehouse not found");
    }
    return toWarehouseResponse(warehouse);
  }

  @Override
  public void archiveAWarehouseUnitByID(String id) {
    var warehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.id = parseId(id);
    archiveWarehouseOperation.archive(warehouse);
  }

  @Override
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    if (data.getBusinessUnitCode() != null
        && !data.getBusinessUnitCode().isBlank()
        && !data.getBusinessUnitCode().equals(businessUnitCode)) {
      throw new WarehouseException(
          Kind.VALIDATION, "Replacement must reuse the business unit code");
    }
    var domain = toDomain(data);
    domain.businessUnitCode = businessUnitCode;
    replaceWarehouseOperation.replace(domain);
    return toWarehouseResponse(domain);
  }

  private Long parseId(String id) {
    if (id == null || id.isBlank()) {
      throw new WarehouseException(Kind.VALIDATION, "Warehouse id is required");
    }
    try {
      return Long.valueOf(id);
    } catch (NumberFormatException exception) {
      throw new WarehouseException(Kind.VALIDATION, "Warehouse id is invalid");
    }
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toDomain(
      Warehouse data) {
    if (data == null) {
      throw new WarehouseException(Kind.VALIDATION, "Warehouse is required");
    }
    var warehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.businessUnitCode = data.getBusinessUnitCode();
    warehouse.location = data.getLocation();
    warehouse.capacity = data.getCapacity();
    warehouse.stock = data.getStock();
    return warehouse;
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    if (warehouse.id != null) {
      response.setId(String.valueOf(warehouse.id));
    }
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);
    return response;
  }
}
