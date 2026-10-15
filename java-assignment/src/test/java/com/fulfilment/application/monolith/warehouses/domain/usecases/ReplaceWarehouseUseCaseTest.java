package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException.Kind;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReplaceWarehouseUseCaseTest {

  @Mock WarehouseStore warehouseStore;
  @Mock ArchiveWarehouseOperation archiveWarehouseOperation;
  @Mock CreateWarehouseOperation createWarehouseOperation;

  ReplaceWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase =
        new ReplaceWarehouseUseCase(
            warehouseStore, archiveWarehouseOperation, createWarehouseOperation);
  }

  @Test
  void archivesCurrentWarehouseThenCreatesTheReplacement() {
    Warehouse current = current();
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(current);
    Warehouse replacement = replacement(5, 20);

    useCase.replace(replacement);

    InOrder order = inOrder(archiveWarehouseOperation, createWarehouseOperation);
    order.verify(archiveWarehouseOperation).archive(current);
    order.verify(createWarehouseOperation).create(replacement);
    assertNull(replacement.id);
  }

  @Test
  void acceptsCapacityEqualToPreviousStock() {
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(current());

    useCase.replace(replacement(5, 5));

    verify(createWarehouseOperation).create(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsMissingWarehouse() {
    WarehouseException exception = assertThrows(WarehouseException.class, () -> useCase.replace(null));
    assertEquals(Kind.VALIDATION, exception.getKind());
  }

  @Test
  void rejectsBlankBusinessUnitCode() {
    Warehouse replacement = replacement(5, 20);
    replacement.businessUnitCode = " ";

    WarehouseException exception =
        assertThrows(WarehouseException.class, () -> useCase.replace(replacement));
    assertEquals("Business unit code is required", exception.getMessage());
  }

  @Test
  void rejectsUnknownBusinessUnitCode() {
    when(warehouseStore.findByBusinessUnitCode("MWH.404")).thenReturn(null);
    Warehouse replacement = replacement(5, 20);
    replacement.businessUnitCode = "MWH.404";

    WarehouseException exception =
        assertThrows(WarehouseException.class, () -> useCase.replace(replacement));
    assertEquals(Kind.NOT_FOUND, exception.getKind());
    verify(archiveWarehouseOperation, never()).archive(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsStockThatDoesNotMatch() {
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(current());

    WarehouseException exception =
        assertThrows(WarehouseException.class, () -> useCase.replace(replacement(4, 20)));
    assertEquals(
        "Stock of the new warehouse must match the stock of the previous warehouse",
        exception.getMessage());
    verify(archiveWarehouseOperation, never()).archive(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsCapacityThatCannotHoldPreviousStock() {
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(current());

    WarehouseException exception =
        assertThrows(WarehouseException.class, () -> useCase.replace(replacement(5, 4)));
    assertEquals(
        "New warehouse capacity cannot accommodate the stock from the warehouse being replaced",
        exception.getMessage());
    verify(createWarehouseOperation, never()).create(org.mockito.ArgumentMatchers.any());
  }

  private static Warehouse current() {
    Warehouse warehouse = new Warehouse();
    warehouse.id = 1L;
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 30;
    warehouse.stock = 5;
    return warehouse;
  }

  private static Warehouse replacement(int stock, int capacity) {
    Warehouse warehouse = new Warehouse();
    warehouse.id = 99L;
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "HELMOND-001";
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }
}
