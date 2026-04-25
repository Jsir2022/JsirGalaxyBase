package com.jsirgalaxybase.modules.core.market.port;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.market.domain.MarketOrder;

public interface MarketOrderBookRepository {

	MarketOrder save(MarketOrder order);

	MarketOrder update(MarketOrder order);

	Optional<MarketOrder> findById(long orderId);

	MarketOrder lockById(long orderId);

	List<MarketOrder> findOpenSellOrdersByProductKey(String productKey);

	List<MarketOrder> findOpenBuyOrdersByProductKey(String productKey);

	List<MarketOrder> findMatchingSellOrders(String productKey, long maxUnitPrice);

	List<MarketOrder> findMatchingBuyOrders(String productKey, long minUnitPrice);

	default List<MarketOrder> findOrdersByOwnerAndProductKey(String ownerPlayerRef, String productKey, int limit) {
		return Collections.emptyList();
	}

	default List<String> findActiveProductKeys(int limit) {
		return Collections.emptyList();
	}

	default List<String> findDistinctProductKeysByOwner(String ownerPlayerRef, int limit) {
		return Collections.emptyList();
	}
}