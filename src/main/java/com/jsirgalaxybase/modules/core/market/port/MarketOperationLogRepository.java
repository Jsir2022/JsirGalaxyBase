package com.jsirgalaxybase.modules.core.market.port;

import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.market.domain.MarketOperationStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationLog;

public interface MarketOperationLogRepository {

	MarketOperationLog save(MarketOperationLog operationLog);

	MarketOperationLog update(MarketOperationLog operationLog);

	Optional<MarketOperationLog> findById(long operationId);

	Optional<MarketOperationLog> findByRequestId(String requestId);

	List<MarketOperationLog> findByStatuses(List<MarketOperationStatus> statuses, int limit);
}