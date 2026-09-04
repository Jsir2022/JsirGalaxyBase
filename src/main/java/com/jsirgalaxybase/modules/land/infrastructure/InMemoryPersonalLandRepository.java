package com.jsirgalaxybase.modules.land.infrastructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import com.jsirgalaxybase.modules.land.domain.LandChunkKey;
import com.jsirgalaxybase.modules.land.domain.PersonalLandActionReceipt;
import com.jsirgalaxybase.modules.land.domain.PersonalLandTitle;
import com.jsirgalaxybase.modules.land.port.PersonalLandRepository;

public final class InMemoryPersonalLandRepository implements PersonalLandRepository {

    private final AtomicLong titleIds = new AtomicLong(0L);
    private final Map<LandChunkKey, PersonalLandTitle> titles = new HashMap<LandChunkKey, PersonalLandTitle>();
    private final Map<String, PersonalLandActionReceipt> receipts =
        new HashMap<String, PersonalLandActionReceipt>();

    @Override
    public long nextTitleId() {
        return titleIds.incrementAndGet();
    }

    @Override
    public synchronized Optional<PersonalLandTitle> findByChunkKey(LandChunkKey chunkKey) {
        PersonalLandTitle title = titles.get(chunkKey);
        return title == null || title.getStatus() == com.jsirgalaxybase.modules.land.domain.LandTitleStatus.REVOKED
            ? Optional.<PersonalLandTitle>empty()
            : Optional.of(title);
    }

    @Override
    public synchronized Optional<PersonalLandTitle> lockByChunkKey(LandChunkKey chunkKey) {
        return findByChunkKey(chunkKey);
    }

    @Override
    public synchronized List<PersonalLandTitle> findByOwnerPlayerRef(String ownerPlayerRef) {
        List<PersonalLandTitle> result = new ArrayList<PersonalLandTitle>();
        for (PersonalLandTitle title : titles.values()) {
            if (title.getStatus() != com.jsirgalaxybase.modules.land.domain.LandTitleStatus.REVOKED
                && title.getOwnerPlayerRef().equals(ownerPlayerRef)) result.add(title);
        }
        Collections.sort(result, Comparator.comparing(PersonalLandTitle::getChunkKey));
        return result;
    }

    @Override
    public synchronized Collection<PersonalLandTitle> findAll() {
        List<PersonalLandTitle> result = new ArrayList<PersonalLandTitle>();
        for (PersonalLandTitle title : titles.values()) {
            if (title.getStatus() != com.jsirgalaxybase.modules.land.domain.LandTitleStatus.REVOKED) result.add(title);
        }
        return result;
    }

    @Override
    public synchronized Collection<PersonalLandTitle> findAllByServerId(String serverId) {
        List<PersonalLandTitle> result = new ArrayList<PersonalLandTitle>();
        for (PersonalLandTitle title : titles.values()) {
            if (title.getStatus() != com.jsirgalaxybase.modules.land.domain.LandTitleStatus.REVOKED
                && title.getChunkKey().getServerId().equals(serverId)) result.add(title);
        }
        return result;
    }

    @Override
    public synchronized void save(PersonalLandTitle title) {
        titles.put(title.getChunkKey(), title);
    }

    @Override
    public synchronized void remove(LandChunkKey chunkKey) {
        titles.remove(chunkKey);
    }

    @Override
    public void lockRequest(String requestId) {}

    @Override
    public void lockOwner(String ownerPlayerRef) {}

    @Override
    public synchronized Optional<PersonalLandActionReceipt> findReceiptByRequestId(String requestId) {
        return Optional.ofNullable(receipts.get(requestId));
    }

    @Override
    public synchronized void saveReceipt(PersonalLandActionReceipt receipt) {
        receipts.put(receipt.getRequestId(), receipt);
    }
}
