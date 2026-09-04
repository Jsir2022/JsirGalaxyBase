package com.jsirgalaxybase.modules.warehouse.port;

import java.util.function.Supplier;

public interface WarehouseTransactionRunner {
    <T> T inTransaction(Supplier<T> callback);
}
