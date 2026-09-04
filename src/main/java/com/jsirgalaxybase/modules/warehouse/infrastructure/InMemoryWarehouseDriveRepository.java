package com.jsirgalaxybase.modules.warehouse.infrastructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveKey;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveReceipt;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveRecord;
import com.jsirgalaxybase.modules.warehouse.port.WarehouseDriveRepository;

/** Test double; production uses the PostgreSQL repository. */
public final class InMemoryWarehouseDriveRepository implements WarehouseDriveRepository {
    private long sequence = 1L;
    private final Map<WarehouseDriveKey, WarehouseDriveRecord> active = new HashMap<WarehouseDriveKey, WarehouseDriveRecord>();
    private final Map<String, WarehouseDriveReceipt> receipts = new HashMap<String, WarehouseDriveReceipt>();
    private final Map<String, String> actors = new HashMap<String, String>();

    @Override public long nextDriveId() { return sequence++; }
    @Override public void lockRequest(String requestId) {}
    @Override public void lockKey(WarehouseDriveKey key) {}
    @Override public Optional<WarehouseDriveRecord> findActiveByKey(WarehouseDriveKey key) { return Optional.ofNullable(active.get(key)); }
    @Override public Optional<WarehouseDriveReceipt> findReceipt(String requestId) { return Optional.ofNullable(receipts.get(requestId)); }
    @Override public void save(WarehouseDriveRecord record) {
        if (record.getStatus().name().equals("ACTIVE")) active.put(record.getKey(), record);
        else active.remove(record.getKey());
    }
    @Override public void saveReceipt(WarehouseDriveReceipt receipt, String actorPlayerRef) {
        receipts.put(receipt.getRequestId(), receipt); actors.put(receipt.getRequestId(), actorPlayerRef);
    }
    @Override public List<WarehouseDriveRecord> findActiveByOwner(String owner, String serverId) {
        List<WarehouseDriveRecord> found = new ArrayList<WarehouseDriveRecord>();
        for (WarehouseDriveRecord record : active.values()) if (record.getOwnerPlayerRef().equals(owner)
            && record.getKey().getServerId().equals(serverId)) found.add(record);
        Collections.sort(found, Comparator.comparing(WarehouseDriveRecord::getKey));
        return found;
    }
    @Override public List<WarehouseDriveReceipt> findRecentByOwner(String owner, String serverId, int limit) {
        List<WarehouseDriveReceipt> found = new ArrayList<WarehouseDriveReceipt>();
        for (Map.Entry<String, WarehouseDriveReceipt> entry : receipts.entrySet()) {
            WarehouseDriveReceipt receipt = entry.getValue();
            if (owner.equals(actors.get(entry.getKey())) && serverId.equals(receipt.getKey().getServerId())) found.add(receipt);
        }
        Collections.sort(found, (a, b) -> b.getRequestId().compareTo(a.getRequestId()));
        return found.size() > limit ? new ArrayList<WarehouseDriveReceipt>(found.subList(0, limit)) : found;
    }
}
