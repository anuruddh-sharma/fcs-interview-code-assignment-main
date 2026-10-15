package com.fulfilment.application.monolith.fulfilment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class FulfilmentRulesTest {

  @Test
  void allowsAssociationInsideAllLimits() {
    assertDoesNotThrow(() -> FulfilmentRules.validateNewAssociation(false, 1, false, 2, false, 4));
  }

  @Test
  void rejectsDuplicateAssociation() {
    FulfilmentException exception =
        assertThrows(
            FulfilmentException.class,
            () -> FulfilmentRules.validateNewAssociation(true, 0, false, 0, false, 0));
    assertEquals(409, exception.getStatus());
  }

  @Test
  void rejectsThirdWarehouseForTheSameProductAndStore() {
    FulfilmentException exception =
        assertThrows(
            FulfilmentException.class,
            () -> FulfilmentRules.validateNewAssociation(false, 2, false, 2, false, 1));
    assertEquals(400, exception.getStatus());
    assertEquals(
        "A product can be fulfilled by at most 2 different warehouses per store",
        exception.getMessage());
  }

  @Test
  void rejectsFourthWarehouseForAStore() {
    FulfilmentException exception =
        assertThrows(
            FulfilmentException.class,
            () -> FulfilmentRules.validateNewAssociation(false, 0, false, 3, false, 1));
    assertEquals(
        "A store can be fulfilled by at most 3 different warehouses", exception.getMessage());
  }

  @Test
  void allowsWarehouseAlreadyServingTheStoreAtTheStoreLimit() {
    assertDoesNotThrow(() -> FulfilmentRules.validateNewAssociation(false, 1, true, 3, true, 5));
  }

  @Test
  void rejectsSixthProductTypeForAWarehouse() {
    FulfilmentException exception =
        assertThrows(
            FulfilmentException.class,
            () -> FulfilmentRules.validateNewAssociation(false, 0, true, 1, false, 5));
    assertEquals("A warehouse can store at most 5 types of products", exception.getMessage());
  }
}
