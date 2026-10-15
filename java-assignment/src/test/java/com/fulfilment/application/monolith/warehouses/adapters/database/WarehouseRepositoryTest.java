package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.support.EmbeddedPostgresTestResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithTestResource(EmbeddedPostgresTestResource.class)
public class WarehouseRepositoryTest {

  @Inject WarehouseStore warehouseStore;
  @Inject CreateWarehouseOperation createWarehouseOperation;
  @Inject ArchiveWarehouseOperation archiveWarehouseOperation;
  @Inject EntityManager entityManager;

  @Test
  public void activeQueriesHideArchivedWarehousesAndRemoveDeletesThem() {
    String code = "MWH-REPO-" + UUID.randomUUID().toString().substring(0, 8);
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = code;
    warehouse.location = "AMSTERDAM-002";
    warehouse.capacity = 10;
    warehouse.stock = 1;

    createWarehouseOperation.create(warehouse);

    assertNotNull(warehouse.id);
    assertNotNull(warehouseStore.findByBusinessUnitCode(code));
    assertTrue(
        warehouseStore.findActiveByLocation("AMSTERDAM-002").stream()
            .anyMatch(existing -> code.equals(existing.businessUnitCode)));

    archiveWarehouseOperation.archive(warehouse);
    entityManager.clear();

    assertNull(warehouseStore.findByBusinessUnitCode(code));
    Warehouse archived = warehouseStore.findById(warehouse.id);
    assertNotNull(archived);
    assertNotNull(archived.archivedAt);
    assertTrue(warehouseStore.getAll().stream().noneMatch(existing -> code.equals(existing.businessUnitCode)));

    warehouseStore.remove(archived);
    entityManager.clear();
    assertNull(warehouseStore.findById(warehouse.id));
    assertNull(warehouseStore.findById(null));

    Warehouse missing = new Warehouse();
    assertThrows(WarehouseException.class, () -> warehouseStore.remove(missing));
    missing.id = 99999L;
    assertThrows(WarehouseException.class, () -> warehouseStore.remove(missing));
    assertThrows(WarehouseException.class, () -> warehouseStore.update(missing));
  }
}
