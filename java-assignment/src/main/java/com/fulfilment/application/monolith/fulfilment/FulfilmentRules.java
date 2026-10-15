package com.fulfilment.application.monolith.fulfilment;

public final class FulfilmentRules {

  public static final int MAX_WAREHOUSES_PER_PRODUCT_AND_STORE = 2;
  public static final int MAX_WAREHOUSES_PER_STORE = 3;
  public static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;

  private FulfilmentRules() {}

  public static void validateNewAssociation(
      boolean duplicate,
      long warehousesForProductAtStore,
      boolean warehouseAlreadyServesStore,
      long warehousesForStore,
      boolean productAlreadyInWarehouse,
      long productsInWarehouse) {
    if (duplicate) {
      throw new FulfilmentException(409, "This fulfilment association already exists");
    }
    if (warehousesForProductAtStore >= MAX_WAREHOUSES_PER_PRODUCT_AND_STORE) {
      throw new FulfilmentException(
          400, "A product can be fulfilled by at most 2 different warehouses per store");
    }
    if (!warehouseAlreadyServesStore && warehousesForStore >= MAX_WAREHOUSES_PER_STORE) {
      throw new FulfilmentException(400, "A store can be fulfilled by at most 3 different warehouses");
    }
    if (!productAlreadyInWarehouse && productsInWarehouse >= MAX_PRODUCTS_PER_WAREHOUSE) {
      throw new FulfilmentException(400, "A warehouse can store at most 5 types of products");
    }
  }
}
