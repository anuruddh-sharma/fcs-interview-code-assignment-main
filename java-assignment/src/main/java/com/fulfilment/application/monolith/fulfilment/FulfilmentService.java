package com.fulfilment.application.monolith.fulfilment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class FulfilmentService {

  private final FulfilmentRepository fulfilmentRepository;
  private final ProductRepository productRepository;
  private final WarehouseStore warehouseStore;

  public FulfilmentService(
      FulfilmentRepository fulfilmentRepository,
      ProductRepository productRepository,
      WarehouseStore warehouseStore) {
    this.fulfilmentRepository = fulfilmentRepository;
    this.productRepository = productRepository;
    this.warehouseStore = warehouseStore;
  }

  @Transactional
  public FulfilmentAssignment associate(Long storeId, Long productId, Long warehouseId) {
    if (storeId == null || productId == null || warehouseId == null) {
      throw new FulfilmentException(400, "storeId, productId and warehouseId are required");
    }

    Store store = Store.findById(storeId);
    if (store == null) {
      throw new FulfilmentException(404, "Store not found");
    }
    Product product = productRepository.findById(productId);
    if (product == null) {
      throw new FulfilmentException(404, "Product not found");
    }
    Warehouse warehouse = warehouseStore.findById(warehouseId);
    if (warehouse == null || warehouse.archivedAt != null) {
      throw new FulfilmentException(404, "Warehouse not found");
    }

    FulfilmentRules.validateNewAssociation(
        fulfilmentRepository.exists(storeId, productId, warehouseId),
        fulfilmentRepository.countDistinctWarehousesForProduct(storeId, productId),
        fulfilmentRepository.storeUsesWarehouse(storeId, warehouseId),
        fulfilmentRepository.countDistinctWarehousesForStore(storeId),
        fulfilmentRepository.warehouseStoresProduct(warehouseId, productId),
        fulfilmentRepository.countDistinctProductsForWarehouse(warehouseId));

    FulfilmentAssignment assignment = new FulfilmentAssignment();
    assignment.storeId = storeId;
    assignment.productId = productId;
    assignment.warehouseId = warehouseId;
    fulfilmentRepository.persist(assignment);
    return assignment;
  }

  @Transactional
  public void remove(Long id) {
    if (id == null) {
      throw new FulfilmentException(404, "Fulfilment association not found");
    }
    FulfilmentAssignment assignment = fulfilmentRepository.findById(id);
    if (assignment == null) {
      throw new FulfilmentException(404, "Fulfilment association not found");
    }
    fulfilmentRepository.delete(assignment);
  }
}
