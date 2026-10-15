package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException.Kind;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateWarehouseUseCaseTest {

  @Mock WarehouseStore warehouseStore;
  @Mock LocationResolver locationResolver;

  CreateWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
  }

  @Test
  void createsWarehouseWhenAllRulesPass() {
    when(locationResolver.resolveByIdentifier("HELMOND-001"))
        .thenReturn(new Location("HELMOND-001", 1, 45));
    when(warehouseStore.findActiveByLocation("HELMOND-001")).thenReturn(List.of());

    Warehouse warehouse = warehouse("MWH.NEW", "HELMOND-001", 40, 10);

    useCase.create(warehouse);

    ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
    verify(warehouseStore).create(captor.capture());
    assertEquals("MWH.NEW", captor.getValue().businessUnitCode);
    assertNotNull(captor.getValue().createdAt);
    assertNull(captor.getValue().archivedAt);
  }

  @Test
  void rejectsMissingWarehouse() {
    WarehouseException exception = assertThrows(WarehouseException.class, () -> useCase.create(null));
    assertEquals(Kind.VALIDATION, exception.getKind());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsMissingBusinessUnitCode() {
    Warehouse warehouse = warehouse(" ", "HELMOND-001", 10, 1);
    WarehouseException exception =
        assertThrows(WarehouseException.class, () -> useCase.create(warehouse));
    assertEquals("Business unit code is required", exception.getMessage());
  }

  @Test
  void rejectsDuplicateBusinessUnitCode() {
    Warehouse existing = warehouse("MWH.001", "ZWOLLE-001", 10, 1);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

    WarehouseException exception =
        assertThrows(WarehouseException.class, () -> useCase.create(warehouse("MWH.001", "HELMOND-001", 10, 1)));
    assertEquals("Business unit code already exists", exception.getMessage());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsMissingLocation() {
    WarehouseException exception =
        assertThrows(WarehouseException.class, () -> useCase.create(warehouse("MWH.NEW", null, 10, 1)));
    assertEquals("Location is required", exception.getMessage());
  }

  @Test
  void rejectsUnknownLocation() {
    when(locationResolver.resolveByIdentifier("NOWHERE")).thenReturn(null);

    WarehouseException exception =
        assertThrows(WarehouseException.class, () -> useCase.create(warehouse("MWH.NEW", "NOWHERE", 10, 1)));
    assertEquals("Location is invalid", exception.getMessage());
  }

  @Test
  void rejectsNonPositiveCapacity() {
    when(locationResolver.resolveByIdentifier("HELMOND-001"))
        .thenReturn(new Location("HELMOND-001", 1, 45));

    WarehouseException exception =
        assertThrows(WarehouseException.class, () -> useCase.create(warehouse("MWH.NEW", "HELMOND-001", 0, 0)));
    assertEquals("Capacity must be greater than zero", exception.getMessage());
  }

  @Test
  void rejectsNegativeStock() {
    when(locationResolver.resolveByIdentifier("HELMOND-001"))
        .thenReturn(new Location("HELMOND-001", 1, 45));

    WarehouseException exception =
        assertThrows(
            WarehouseException.class, () -> useCase.create(warehouse("MWH.NEW", "HELMOND-001", 10, -1)));
    assertEquals("Stock must be zero or greater", exception.getMessage());
  }

  @Test
  void rejectsStockAboveCapacity() {
    when(locationResolver.resolveByIdentifier("HELMOND-001"))
        .thenReturn(new Location("HELMOND-001", 1, 45));

    WarehouseException exception =
        assertThrows(
            WarehouseException.class, () -> useCase.create(warehouse("MWH.NEW", "HELMOND-001", 10, 11)));
    assertEquals("Capacity cannot handle the informed stock", exception.getMessage());
  }

  @Test
  void rejectsWhenLocationWarehouseLimitIsReached() {
    when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
        .thenReturn(new Location("ZWOLLE-001", 1, 40));
    when(warehouseStore.findActiveByLocation("ZWOLLE-001"))
        .thenReturn(List.of(warehouse("MWH.001", "ZWOLLE-001", 10, 1)));

    WarehouseException exception =
        assertThrows(
            WarehouseException.class, () -> useCase.create(warehouse("MWH.NEW", "ZWOLLE-001", 10, 1)));
    assertEquals("Maximum number of warehouses reached for this location", exception.getMessage());
  }

  @Test
  void rejectsWhenLocationCapacityWouldBeExceeded() {
    when(locationResolver.resolveByIdentifier("VETSBY-001"))
        .thenReturn(new Location("VETSBY-001", 2, 90));
    when(warehouseStore.findActiveByLocation("VETSBY-001"))
        .thenReturn(List.of(warehouse("MWH.EXISTING", "VETSBY-001", 50, 1)));

    WarehouseException exception =
        assertThrows(
            WarehouseException.class, () -> useCase.create(warehouse("MWH.NEW", "VETSBY-001", 41, 1)));
    assertEquals(
        "Warehouse capacity exceeds the maximum capacity of the location", exception.getMessage());
  }

  @Test
  void allowsStockEqualToCapacityWhenLocationHasRoom() {
    when(locationResolver.resolveByIdentifier("HELMOND-001"))
        .thenReturn(new Location("HELMOND-001", 1, 45));
    when(warehouseStore.findActiveByLocation("HELMOND-001")).thenReturn(List.of());

    useCase.create(warehouse("MWH.FULL", "HELMOND-001", 45, 45));

    verify(warehouseStore).create(any());
  }

  private static Warehouse warehouse(String code, String location, Integer capacity, Integer stock) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = code;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }
}
