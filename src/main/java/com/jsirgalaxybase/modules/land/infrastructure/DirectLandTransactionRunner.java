package com.jsirgalaxybase.modules.land.infrastructure;

import java.util.function.Supplier;

import com.jsirgalaxybase.modules.land.port.LandTransactionRunner;

public final class DirectLandTransactionRunner implements LandTransactionRunner {

    @Override
    public <T> T inTransaction(Supplier<T> callback) {
        return callback.get();
    }
}
