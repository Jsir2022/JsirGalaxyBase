package com.jsirgalaxybase.modules.warehouse.infrastructure;

import com.jsirgalaxybase.modules.warehouse.port.WarehouseDriveRepository;
import com.jsirgalaxybase.modules.warehouse.port.WarehouseTransactionRunner;

public final class WarehouseInfrastructure {
    private final WarehouseDriveRepository repository;
    private final WarehouseTransactionRunner transactionRunner;
    public WarehouseInfrastructure(WarehouseDriveRepository repository, WarehouseTransactionRunner transactionRunner) {
        this.repository = repository; this.transactionRunner = transactionRunner;
    }
    public WarehouseDriveRepository getRepository() { return repository; }
    public WarehouseTransactionRunner getTransactionRunner() { return transactionRunner; }
}
