package com.jsirgalaxybase.modules.core.market.port;

import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyInventory;

public interface MarketCustodyInventoryRepository {

	MarketCustodyInventory save(MarketCustodyInventory custodyInventory);

	MarketCustodyInventory update(MarketCustodyInventory custodyInventory);

	Optional<MarketCustodyInventory> findById(long custodyId);

	MarketCustodyInventory lockById(long custodyId);

	Optional<MarketCustodyInventory> findEscrowSellByOrderId(long orderId);

	List<MarketCustodyInventory> findByOwnerAndStatus(String ownerPlayerRef, MarketCustodyStatus status);
}