package com.jsirgalaxybase.modules.core.market.port;

import java.util.Optional;

import com.jsirgalaxybase.modules.core.market.domain.CustomMarketAuditLog;

public interface CustomMarketAuditLogRepository {

    CustomMarketAuditLog save(CustomMarketAuditLog auditLog);

    default CustomMarketAuditLog update(CustomMarketAuditLog auditLog) {
        throw new UnsupportedOperationException("custom market audit update is not supported");
    }

    Optional<CustomMarketAuditLog> findByRequestId(String requestId);
}
