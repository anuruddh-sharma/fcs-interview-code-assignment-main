package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException.Kind;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final ArchiveWarehouseOperation archiveWarehouseOperation;
  private final CreateWarehouseOperation createWarehouseOperation;

  public ReplaceWarehouseUseCase(
      WarehouseStore warehouseStore,
      ArchiveWarehouseOperation archiveWarehouseOperation,
      CreateWarehouseOperation createWarehouseOperation) {
    this.warehouseStore = warehouseStore;
    this.archiveWarehouseOperation = archiveWarehouseOperation;
    this.createWarehouseOperation = createWarehouseOperation;
  }

  @Override
  @Transactional
  public void replace(Warehouse newWarehouse) {
    if (newWarehouse == null || isBlank(newWarehouse.businessUnitCode)) {
      throw new WarehouseException(Kind.VALIDATION, "Business unit code is required");
    }

    Warehouse current = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (current == null) {
      throw new WarehouseException(Kind.NOT_FOUND, "Warehouse not found");
    }
    if (newWarehouse.stock == null || !newWarehouse.stock.equals(current.stock)) {
      throw new WarehouseException(
          Kind.VALIDATION,
          "Stock of the new warehouse must match the stock of the previous warehouse");
    }
    if (newWarehouse.capacity == null || current.stock == null || newWarehouse.capacity < current.stock) {
      throw new WarehouseException(
          Kind.VALIDATION,
          "New warehouse capacity cannot accommodate the stock from the warehouse being replaced");
    }

    archiveWarehouseOperation.archive(current);
    newWarehouse.id = null;
    createWarehouseOperation.create(newWarehouse);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
