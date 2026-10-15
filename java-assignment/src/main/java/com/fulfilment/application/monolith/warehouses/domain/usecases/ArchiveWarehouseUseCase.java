package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException.Kind;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private final WarehouseStore warehouseStore;

  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  @Transactional
  public void archive(Warehouse warehouse) {
    if (warehouse == null || warehouse.id == null) {
      throw new WarehouseException(Kind.NOT_FOUND, "Warehouse not found");
    }

    Warehouse existing = warehouseStore.findById(warehouse.id);
    if (existing == null || existing.archivedAt != null) {
      throw new WarehouseException(Kind.NOT_FOUND, "Warehouse not found");
    }

    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);
    warehouse.archivedAt = existing.archivedAt;
  }
}
