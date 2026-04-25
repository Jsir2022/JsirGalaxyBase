package com.jsirgalaxybase.modules.core.market.port;

import java.util.Optional;

import com.jsirgalaxybase.modules.core.market.domain.CustomMarketAuditLog;

public interface CustomMarketAuditLogRepository {

    CustomMarketAuditLog save(CustomMarketAuditLog auditLog);

    Optional<CustomMarketAuditLog> findByRequestId(String requestId);
}