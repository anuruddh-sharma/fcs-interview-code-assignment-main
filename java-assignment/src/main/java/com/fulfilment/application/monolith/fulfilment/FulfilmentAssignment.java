package com.fulfilment.application.monolith.fulfilment;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "fulfilment_assignment",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"storeId", "productId", "warehouseId"}))
public class FulfilmentAssignment extends PanacheEntity {

  @Column(nullable = false)
  public Long storeId;

  @Column(nullable = false)
  public Long productId;

  @Column(nullable = false)
  public Long warehouseId;
}
