package com.jsirgalaxybase.modules.warehouse.infrastructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.jsirgalaxybase.modules.warehouse.domain.TerminalWarehouseBay;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalWarehouseBayReceipt;
import com.jsirgalaxybase.modules.warehouse.port.TerminalWarehouseBayRepository;

/** Deterministic test repository; production always uses shared JDBC. */
public final class InMemoryTerminalWarehouseBayRepository implements TerminalWarehouseBayRepository {
    private final Map<String, TerminalWarehouseBay> bays = new HashMap<String, TerminalWarehouseBay>();
    private final Map<String, TerminalWarehouseBayReceipt> receipts = new HashMap<String, TerminalWarehouseBayReceipt>();
    @Override public void lockOwner(String serverId, String ownerPlayerRef) { }
    @Override public Optional<TerminalWarehouseBay> find(String serverId, String ownerPlayerRef) { return Optional.ofNullable(bays.get(key(serverId, ownerPlayerRef))); }
    @Override public void save(TerminalWarehouseBay bay) { bays.put(key(bay.getServerId(), bay.getOwnerPlayerRef()), bay); }
    @Override public Optional<TerminalWarehouseBayReceipt> findReceipt(String requestId) { return Optional.ofNullable(receipts.get(requestId)); }
    @Override public void saveReceipt(TerminalWarehouseBayReceipt receipt, String actorPlayerRef) { receipts.put(receipt.getRequestId(), receipt); }
    @Override public List<TerminalWarehouseBayReceipt> findRecent(String serverId, String ownerPlayerRef, int limit) {
        List<TerminalWarehouseBayReceipt> result = new ArrayList<TerminalWarehouseBayReceipt>();
        for (TerminalWarehouseBayReceipt receipt : receipts.values()) {
            TerminalWarehouseBay before = receipt.getBefore(); TerminalWarehouseBay after = receipt.getAfter();
            TerminalWarehouseBay value = after == null ? before : after;
            if (value != null && value.getServerId().equals(serverId) && value.getOwnerPlayerRef().equals(ownerPlayerRef)) result.add(receipt);
        }
        Collections.sort(result, (a, b) -> b.getRequestId().compareTo(a.getRequestId()));
        return result.size() > limit ? new ArrayList<TerminalWarehouseBayReceipt>(result.subList(0, limit)) : result;
    }
    private static String key(String server, String owner) { return server + "|" + owner; }
}
