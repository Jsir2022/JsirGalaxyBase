package com.jsirgalaxybase.modules.warehouse.port;

import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveKey;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveReceipt;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveRecord;

public interface WarehouseDriveRepository {
    long nextDriveId();
    void lockRequest(String requestId);
    void lockKey(WarehouseDriveKey key);
    Optional<WarehouseDriveRecord> findActiveByKey(WarehouseDriveKey key);
    Optional<WarehouseDriveReceipt> findReceipt(String requestId);
    List<WarehouseDriveRecord> findActiveByOwner(String ownerPlayerRef, String serverId);
    List<WarehouseDriveReceipt> findRecentByOwner(String ownerPlayerRef, String serverId, int limit);
    void save(WarehouseDriveRecord record);
    void saveReceipt(WarehouseDriveReceipt receipt, String actorPlayerRef);
}
