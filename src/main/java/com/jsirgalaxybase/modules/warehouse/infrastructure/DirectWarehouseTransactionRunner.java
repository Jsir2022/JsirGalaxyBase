package com.jsirgalaxybase.modules.warehouse.infrastructure;

import java.util.function.Supplier;

import com.jsirgalaxybase.modules.warehouse.port.WarehouseTransactionRunner;

public final class DirectWarehouseTransactionRunner implements WarehouseTransactionRunner {
    @Override
    public <T> T inTransaction(Supplier<T> callback) { return callback.get(); }
}
