package com.jsirgalaxybase.modules.land.port;

import java.util.function.Supplier;

public interface LandTransactionRunner {

    <T> T inTransaction(Supplier<T> callback);
}
