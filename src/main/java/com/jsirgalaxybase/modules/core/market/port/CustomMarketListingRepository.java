package com.jsirgalaxybase.modules.core.market.port;

import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.market.domain.CustomMarketDeliveryStatus;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketListing;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketListingStatus;

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
}