package com.jsirgalaxybase.modules.core.market.port;

import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.market.domain.CustomMarketDeliveryStatus;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketListing;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketListingStatus;
import com.jsirgalaxybase.modules.core.market.application.CustomMarketBrowsePage;

public interface CustomMarketListingRepository {

    CustomMarketListing save(CustomMarketListing listing);

    CustomMarketListing update(CustomMarketListing listing);

    Optional<CustomMarketListing> findById(long listingId);

    CustomMarketListing lockById(long listingId);

    List<CustomMarketListing> findByStatus(CustomMarketListingStatus status, int limit);

    List<CustomMarketListing> findBySellerAndDeliveryStatus(String sellerPlayerRef,
        CustomMarketDeliveryStatus deliveryStatus, int limit);

    List<CustomMarketListing> findByBuyerAndDeliveryStatus(String buyerPlayerRef,
        CustomMarketDeliveryStatus deliveryStatus, int limit);

    /**
     * Returns a real database page for the terminal browser. Scope is one of
     * active, selling or pending; implementations must apply query before
     * computing the total.
     */
    CustomMarketBrowsePage findBrowsePage(String scope, String playerRef, String query, int offset, int limit);
}
