package com.jsirgalaxybase.modules.land.application;

import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.land.domain.LandActionResult;
import com.jsirgalaxybase.modules.land.domain.LandActionType;
import com.jsirgalaxybase.modules.land.domain.LandChunkKey;
import com.jsirgalaxybase.modules.land.domain.LandTitleStatus;
import com.jsirgalaxybase.modules.land.domain.PersonalLandActionReceipt;
import com.jsirgalaxybase.modules.land.domain.PersonalLandTitle;
import com.jsirgalaxybase.modules.land.infrastructure.DirectLandTransactionRunner;
import com.jsirgalaxybase.modules.land.port.LandTransactionRunner;
import com.jsirgalaxybase.modules.land.port.PersonalLandRepository;

public final class PersonalLandService {

    private final PersonalLandRepository repository;
    private final PersonalLandRules rules;
    private final LandProtectionIndex protectionIndex;
    private final LandTransactionRunner transactionRunner;
    private final String localServerId;

    public PersonalLandService(PersonalLandRepository repository, PersonalLandRules rules,
        LandProtectionIndex protectionIndex) {
        this(repository, rules, protectionIndex, new DirectLandTransactionRunner(), null);
    }

    public PersonalLandService(PersonalLandRepository repository, PersonalLandRules rules,
        LandProtectionIndex protectionIndex, LandTransactionRunner transactionRunner, String localServerId) {
        if (repository == null || rules == null || protectionIndex == null) {
            throw new IllegalArgumentException("personal land dependencies must not be null");
        }
        if (transactionRunner == null) throw new IllegalArgumentException("transactionRunner must not be null");
        this.repository = repository;
        this.rules = rules;
        this.protectionIndex = protectionIndex;
        this.transactionRunner = transactionRunner;
        this.localServerId = localServerId == null ? null : requireText(localServerId, "localServerId");
        refreshProtectionIndex();
    }

    public PersonalLandActionReceipt claim(String requestId, String playerRef, LandChunkKey chunkKey) {
        final String normalizedRequestId = requireText(requestId, "requestId");
        final String normalizedPlayerRef = requireText(playerRef, "playerRef");
        if (chunkKey == null) {
            return receipt(normalizedRequestId, "CLAIM|" + normalizedPlayerRef + "|null", LandActionType.CLAIM,
                LandActionResult.INVALID_REQUEST, null, null, false);
        }
        if (!isLocal(chunkKey)) {
            return receipt(normalizedRequestId, "CLAIM|" + normalizedPlayerRef + "|" + chunkKey,
                LandActionType.CLAIM, LandActionResult.SERVER_MISMATCH, null, null, false);
        }
        final LandChunkKey target = chunkKey;
        final String semanticsKey = "CLAIM|" + normalizedPlayerRef + "|" + target;
        PersonalLandActionReceipt result = transactionRunner.inTransaction(() -> {
            repository.lockRequest(normalizedRequestId);
            PersonalLandActionReceipt replay = replayOrConflict(normalizedRequestId, semanticsKey,
                LandActionType.CLAIM);
            if (replay != null) return replay;
            repository.lockOwner(normalizedPlayerRef);

            Optional<PersonalLandTitle> existing = repository.lockByChunkKey(target);
            if (existing.isPresent()) {
                return receipt(normalizedRequestId, semanticsKey, LandActionType.CLAIM,
                    LandActionResult.ALREADY_CLAIMED, existing.get(), existing.get(), true);
            }

            List<PersonalLandTitle> owned = repository.findByOwnerPlayerRef(normalizedPlayerRef);
            int localOwned = 0;
            for (PersonalLandTitle title : owned) {
                if (isLocal(title.getChunkKey())) localOwned++;
            }
            LandActionResult decision = rules.evaluateClaim(target, localOwned);
            if (decision != LandActionResult.SUCCESS) {
                return receipt(normalizedRequestId, semanticsKey, LandActionType.CLAIM, decision, null, null, true);
            }

            PersonalLandTitle title = new PersonalLandTitle(repository.nextTitleId(), target, normalizedPlayerRef,
                LandTitleStatus.ACTIVE, 1L);
            repository.save(title);
            return receipt(normalizedRequestId, semanticsKey, LandActionType.CLAIM, LandActionResult.SUCCESS, null,
                title, true);
        });
        synchronizeIndex(target);
        return result;
    }

    public PersonalLandActionReceipt unclaim(String requestId, String playerRef, LandChunkKey chunkKey,
        long expectedVersion) {
        final String normalizedRequestId = requireText(requestId, "requestId");
        final String normalizedPlayerRef = requireText(playerRef, "playerRef");
        final String semanticsKey = "UNCLAIM|" + normalizedPlayerRef + "|" + chunkKey + "|" + expectedVersion;
        if (chunkKey == null || expectedVersion <= 0L) {
            return receipt(normalizedRequestId, semanticsKey, LandActionType.UNCLAIM,
                LandActionResult.INVALID_REQUEST, null, null, false);
        }
        if (!isLocal(chunkKey)) {
            return receipt(normalizedRequestId, semanticsKey, LandActionType.UNCLAIM,
                LandActionResult.SERVER_MISMATCH, null, null, false);
        }
        final LandChunkKey target = chunkKey;
        PersonalLandActionReceipt result = transactionRunner.inTransaction(() -> {
            repository.lockRequest(normalizedRequestId);
            PersonalLandActionReceipt replay = replayOrConflict(normalizedRequestId, semanticsKey,
                LandActionType.UNCLAIM);
            if (replay != null) return replay;

            Optional<PersonalLandTitle> found = repository.lockByChunkKey(target);
            if (!found.isPresent()) {
                return receipt(normalizedRequestId, semanticsKey, LandActionType.UNCLAIM,
                    LandActionResult.NOT_CLAIMED, null, null, true);
            }
            PersonalLandTitle title = found.get();
            if (!title.getOwnerPlayerRef().equals(normalizedPlayerRef)) {
                return receipt(normalizedRequestId, semanticsKey, LandActionType.UNCLAIM,
                    LandActionResult.NOT_OWNER, title, title, true);
            }
            if (title.getVersion() != expectedVersion) {
                return receipt(normalizedRequestId, semanticsKey, LandActionType.UNCLAIM,
                    LandActionResult.VERSION_CONFLICT, title, title, true);
            }
            if (title.getStatus() != LandTitleStatus.ACTIVE) {
                return receipt(normalizedRequestId, semanticsKey, LandActionType.UNCLAIM,
                    LandActionResult.TITLE_LOCKED, title, title, true);
            }

            PersonalLandTitle revoked = title.withStatus(LandTitleStatus.REVOKED);
            repository.save(revoked);
            return receipt(normalizedRequestId, semanticsKey, LandActionType.UNCLAIM, LandActionResult.SUCCESS, title,
                revoked, true);
        });
        synchronizeIndex(target);
        return result;
    }

    public Optional<PersonalLandTitle> find(LandChunkKey chunkKey) {
        return repository.findByChunkKey(chunkKey);
    }

    public List<PersonalLandTitle> listOwned(String playerRef) {
        return repository.findByOwnerPlayerRef(requireText(playerRef, "playerRef"));
    }

    public LandProtectionIndex getProtectionIndex() {
        return protectionIndex;
    }

    public boolean isReserved(LandChunkKey chunkKey) {
        return rules.isReserved(chunkKey);
    }

    public boolean isDimensionBlocked(int dimensionId) {
        return rules.isDimensionBlocked(dimensionId);
    }

    public void refreshProtectionIndex() {
        protectionIndex.replace(localServerId == null ? repository.findAll()
            : repository.findAllByServerId(localServerId));
    }

    private void synchronizeIndex(LandChunkKey chunkKey) {
        Optional<PersonalLandTitle> current = repository.findByChunkKey(chunkKey);
        if (current.isPresent()) protectionIndex.put(current.get());
        else protectionIndex.remove(chunkKey);
    }

    private boolean isLocal(LandChunkKey chunkKey) {
        return localServerId == null || localServerId.equals(chunkKey.getServerId());
    }

    private PersonalLandActionReceipt replayOrConflict(String requestId, String semanticsKey,
        LandActionType actionType) {
        Optional<PersonalLandActionReceipt> existing = repository.findReceiptByRequestId(requestId);
        if (!existing.isPresent()) return null;
        PersonalLandActionReceipt receipt = existing.get();
        if (receipt.getSemanticsKey().equals(semanticsKey) && receipt.getActionType() == actionType) return receipt;
        return new PersonalLandActionReceipt(requestId, semanticsKey, actionType, LandActionResult.REQUEST_CONFLICT,
            receipt.getBeforeTitle(), receipt.getAfterTitle());
    }

    private PersonalLandActionReceipt receipt(String requestId, String semanticsKey, LandActionType actionType,
        LandActionResult result, PersonalLandTitle before, PersonalLandTitle after, boolean persist) {
        PersonalLandActionReceipt receipt = new PersonalLandActionReceipt(requestId, semanticsKey, actionType, result,
            before, after);
        if (persist) repository.saveReceipt(receipt);
        return receipt;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
