package com.jsirgalaxybase.modules.warehouse.port;

import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.warehouse.domain.TerminalWarehouseBay;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalWarehouseBayReceipt;

public interface TerminalWarehouseBayRepository {
    void lockOwner(String serverId, String ownerPlayerRef);
    Optional<TerminalWarehouseBay> find(String serverId, String ownerPlayerRef);
    void save(TerminalWarehouseBay bay);
    Optional<TerminalWarehouseBayReceipt> findReceipt(String requestId);
    void saveReceipt(TerminalWarehouseBayReceipt receipt, String actorPlayerRef);
    List<TerminalWarehouseBayReceipt> findRecent(String serverId, String ownerPlayerRef, int limit);
}
