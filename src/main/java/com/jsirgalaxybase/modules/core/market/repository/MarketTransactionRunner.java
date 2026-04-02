package com.jsirgalaxybase.modules.core.market.repository;

import java.util.function.Supplier;

public interface MarketTransactionRunner {

    <T> T inTransaction(Supplier<T> callback);

    void inTransaction(Runnable callback);
}