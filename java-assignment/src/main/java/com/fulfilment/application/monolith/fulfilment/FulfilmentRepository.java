package com.fulfilment.application.monolith.fulfilment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class FulfilmentRepository implements PanacheRepository<FulfilmentAssignment> {

  public List<FulfilmentAssignment> listByStore(Long storeId) {
    if (storeId == null) {
      return listAll();
    }
    return list("storeId", storeId);
  }

  public boolean exists(Long storeId, Long productId, Long warehouseId) {
    return count(
            "storeId = ?1 and productId = ?2 and warehouseId = ?3",
            storeId,
            productId,
            warehouseId)
        > 0;
  }

  public long countDistinctWarehousesForProduct(Long storeId, Long productId) {
    Long count =
        getEntityManager()
            .createQuery(
                "select count(distinct assignment.warehouseId) from FulfilmentAssignment assignment "
                    + "where assignment.storeId = ?1 and assignment.productId = ?2",
                Long.class)
            .setParameter(1, storeId)
            .setParameter(2, productId)
            .getSingleResult();
    return count == null ? 0 : count;
  }

  public long countDistinctWarehousesForStore(Long storeId) {
    Long count =
        getEntityManager()
            .createQuery(
                "select count(distinct assignment.warehouseId) from FulfilmentAssignment assignment "
                    + "where assignment.storeId = ?1",
                Long.class)
            .setParameter(1, storeId)
            .getSingleResult();
    return count == null ? 0 : count;
  }

  public boolean storeUsesWarehouse(Long storeId, Long warehouseId) {
    return count("storeId = ?1 and warehouseId = ?2", storeId, warehouseId) > 0;
  }

  public long countDistinctProductsForWarehouse(Long warehouseId) {
    Long count =
        getEntityManager()
            .createQuery(
                "select count(distinct assignment.productId) from FulfilmentAssignment assignment "
                    + "where assignment.warehouseId = ?1",
                Long.class)
            .setParameter(1, warehouseId)
            .getSingleResult();
    return count == null ? 0 : count;
  }

  public boolean warehouseStoresProduct(Long warehouseId, Long productId) {
    return count("warehouseId = ?1 and productId = ?2", warehouseId, productId) > 0;
  }
}
