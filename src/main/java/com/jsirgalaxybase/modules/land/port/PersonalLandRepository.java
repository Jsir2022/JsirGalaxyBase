package com.jsirgalaxybase.modules.land.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.land.domain.LandChunkKey;
import com.jsirgalaxybase.modules.land.domain.PersonalLandActionReceipt;
import com.jsirgalaxybase.modules.land.domain.PersonalLandTitle;

public interface PersonalLandRepository {

    long nextTitleId();

    Optional<PersonalLandTitle> findByChunkKey(LandChunkKey chunkKey);

    Optional<PersonalLandTitle> lockByChunkKey(LandChunkKey chunkKey);

    List<PersonalLandTitle> findByOwnerPlayerRef(String ownerPlayerRef);

    Collection<PersonalLandTitle> findAll();

    Collection<PersonalLandTitle> findAllByServerId(String serverId);

    void save(PersonalLandTitle title);

    void remove(LandChunkKey chunkKey);

    void lockRequest(String requestId);

    void lockOwner(String ownerPlayerRef);

    Optional<PersonalLandActionReceipt> findReceiptByRequestId(String requestId);

    void saveReceipt(PersonalLandActionReceipt receipt);
}
