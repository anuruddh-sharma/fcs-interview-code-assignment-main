package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseException.Kind;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore {

  private final DbWarehouseRepository dbWarehouseRepository;

  public WarehouseRepository(DbWarehouseRepository dbWarehouseRepository) {
    this.dbWarehouseRepository = dbWarehouseRepository;
  }

  @Override
  public List<Warehouse> getAll() {
    return dbWarehouseRepository.find("archivedAt is null").list().stream()
        .map(DbWarehouse::toWarehouse)
        .toList();
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    DbWarehouse dbWarehouse = new DbWarehouse();
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt =
        warehouse.createdAt == null ? LocalDateTime.now() : warehouse.createdAt;
    dbWarehouse.archivedAt = warehouse.archivedAt;
    dbWarehouseRepository.persist(dbWarehouse);
    dbWarehouseRepository.getEntityManager().flush();
    warehouse.id = dbWarehouse.id;
  }

  @Override
  @Transactional
  public void update(Warehouse warehouse) {
    if (warehouse == null || warehouse.id == null) {
      throw new WarehouseException(Kind.NOT_FOUND, "Warehouse not found");
    }
    DbWarehouse dbWarehouse = dbWarehouseRepository.findById(warehouse.id);
    if (dbWarehouse == null) {
      throw new WarehouseException(Kind.NOT_FOUND, "Warehouse not found");
    }
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = warehouse.createdAt;
    dbWarehouse.archivedAt = warehouse.archivedAt;
    dbWarehouseRepository.getEntityManager().flush();
  }

  @Override
  @Transactional
  public void remove(Warehouse warehouse) {
    if (warehouse == null || warehouse.id == null) {
      throw new WarehouseException(Kind.NOT_FOUND, "Warehouse not found");
    }
    DbWarehouse dbWarehouse = dbWarehouseRepository.findById(warehouse.id);
    if (dbWarehouse == null) {
      throw new WarehouseException(Kind.NOT_FOUND, "Warehouse not found");
    }
    dbWarehouseRepository.delete(dbWarehouse);
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse dbWarehouse =
        dbWarehouseRepository
            .find("businessUnitCode = ?1 and archivedAt is null", buCode)
            .firstResult();
    return dbWarehouse == null ? null : dbWarehouse.toWarehouse();
  }

  @Override
  public Warehouse findById(Long id) {
    if (id == null) {
      return null;
    }
    DbWarehouse dbWarehouse = dbWarehouseRepository.findById(id);
    return dbWarehouse == null ? null : dbWarehouse.toWarehouse();
  }

  @Override
  public List<Warehouse> findActiveByLocation(String location) {
    return dbWarehouseRepository.find("location = ?1 and archivedAt is null", location).list().stream()
        .map(DbWarehouse::toWarehouse)
        .toList();
  }
}
