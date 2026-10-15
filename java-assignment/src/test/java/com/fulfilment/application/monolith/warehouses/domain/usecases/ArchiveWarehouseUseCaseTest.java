package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException.Kind;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ArchiveWarehouseUseCaseTest {

  @Mock WarehouseStore warehouseStore;

  ArchiveWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  @Test
  void archivesActiveWarehouse() {
    Warehouse existing = warehouse(4L);
    when(warehouseStore.findById(4L)).thenReturn(existing);

    Warehouse request = new Warehouse();
    request.id = 4L;
    useCase.archive(request);

    ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).update(captor.capture());
    assertNotNull(captor.getValue().archivedAt);
    assertEquals(4L, captor.getValue().id);
  }

  @Test
  void rejectsMissingWarehouse() {
    WarehouseException exception = assertThrows(WarehouseException.class, () -> useCase.archive(null));
    assertEquals(Kind.NOT_FOUND, exception.getKind());
    verify(warehouseStore, never()).update(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsMissingId() {
    WarehouseException exception =
        assertThrows(WarehouseException.class, () -> useCase.archive(new Warehouse()));
    assertEquals(Kind.NOT_FOUND, exception.getKind());
  }

  @Test
  void rejectsUnknownId() {
    when(warehouseStore.findById(99L)).thenReturn(null);
    Warehouse request = new Warehouse();
    request.id = 99L;

    WarehouseException exception = assertThrows(WarehouseException.class, () -> useCase.archive(request));
    assertEquals("Warehouse not found", exception.getMessage());
  }

  @Test
  void rejectsAlreadyArchivedWarehouse() {
    Warehouse existing = warehouse(4L);
    existing.archivedAt = LocalDateTime.now();
    when(warehouseStore.findById(4L)).thenReturn(existing);
    Warehouse request = new Warehouse();
    request.id = 4L;

    WarehouseException exception = assertThrows(WarehouseException.class, () -> useCase.archive(request));
    assertEquals(Kind.NOT_FOUND, exception.getKind());
    verify(warehouseStore, never()).update(org.mockito.ArgumentMatchers.any());
  }

  private static Warehouse warehouse(Long id) {
    Warehouse warehouse = new Warehouse();
    warehouse.id = id;
    warehouse.businessUnitCode = "MWH.001";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 10;
    warehouse.stock = 1;
    return warehouse;
  }
}
