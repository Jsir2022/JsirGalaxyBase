package com.jsirgalaxybase.modules.core.banking.repository;

import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.banking.domain.CoinExchangeRecord;

public interface CoinExchangeRecordRepository {

    CoinExchangeRecord save(CoinExchangeRecord record);

    Optional<CoinExchangeRecord> findByTransactionId(long transactionId);

    List<CoinExchangeRecord> findRecentByPlayerRef(String playerRef, int limit);
}