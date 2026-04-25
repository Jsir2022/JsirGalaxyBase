package com.jsirgalaxybase.modules.core.market.port;

import java.util.Optional;

import com.jsirgalaxybase.modules.core.market.domain.CustomMarketItemSnapshot;

public interface CustomMarketItemSnapshotRepository {

    CustomMarketItemSnapshot save(CustomMarketItemSnapshot snapshot);

    Optional<CustomMarketItemSnapshot> findByListingId(long listingId);
}