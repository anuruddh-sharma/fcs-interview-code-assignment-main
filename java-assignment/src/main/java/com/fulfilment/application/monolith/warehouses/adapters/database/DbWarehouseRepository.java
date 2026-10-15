package com.fulfilment.application.monolith.warehouses.adapters.database;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DbWarehouseRepository implements PanacheRepository<DbWarehouse> {}
