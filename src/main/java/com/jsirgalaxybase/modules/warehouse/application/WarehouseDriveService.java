package com.jsirgalaxybase.modules.warehouse.application;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveKey;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveOperation;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveReceipt;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveRecord;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveResult;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveStatus;
import com.jsirgalaxybase.modules.warehouse.port.WarehouseDriveRepository;
import com.jsirgalaxybase.modules.warehouse.port.WarehouseTransactionRunner;

/** Transactional ownership and audit boundary for physical personal Drives. */
public final class WarehouseDriveService {

    private final WarehouseDriveRepository repository;
    private final WarehouseTransactionRunner transactionRunner;
    private final String localServerId;

    public WarehouseDriveService(WarehouseDriveRepository repository, WarehouseTransactionRunner transactionRunner,
        String localServerId) {
        if (repository == null || transactionRunner == null) throw new IllegalArgumentException("warehouse dependencies are required");
        this.repository = repository;
        this.transactionRunner = transactionRunner;
        this.localServerId = require(localServerId, "localServerId");
    }

    public WarehouseDriveReceipt register(String requestId, String playerRef, boolean fakePlayer, WarehouseDriveKey key) {
        return mutate(requestId, playerRef, fakePlayer, key, WarehouseDriveOperation.REGISTER, "", record -> {
            if (record != null) return WarehouseDriveResult.ALREADY_REGISTERED;
            return WarehouseDriveResult.SUCCESS;
        }, true);
    }

    public WarehouseDriveReceipt authorizeInstall(String requestId, String playerRef, boolean fakePlayer,
        WarehouseDriveKey key, boolean cellValid, boolean bayOccupied) {
        final WarehouseDriveResult requested = !cellValid ? WarehouseDriveResult.INVALID_CELL
            : bayOccupied ? WarehouseDriveResult.BAY_OCCUPIED : WarehouseDriveResult.SUCCESS;
        return mutate(requestId, playerRef, fakePlayer, key, WarehouseDriveOperation.INSTALL_CELL, "", record -> requested, false);
    }

    public WarehouseDriveReceipt authorizeRemoveCell(String requestId, String playerRef, boolean fakePlayer,
        WarehouseDriveKey key, boolean hasCell, boolean cellEmpty) {
        final WarehouseDriveResult requested = !hasCell ? WarehouseDriveResult.NO_CELL
            : !cellEmpty ? WarehouseDriveResult.CELL_NOT_EMPTY : WarehouseDriveResult.SUCCESS;
        return mutate(requestId, playerRef, fakePlayer, key, WarehouseDriveOperation.REMOVE_CELL, "", record -> requested, false);
    }

    public WarehouseDriveReceipt authorizeBreak(String requestId, String playerRef, boolean fakePlayer,
        WarehouseDriveKey key, boolean cellInstalled, boolean cellEmpty) {
        final WarehouseDriveResult requested = cellInstalled && !cellEmpty ? WarehouseDriveResult.CELL_NOT_EMPTY
            : WarehouseDriveResult.SUCCESS;
        return mutate(requestId, playerRef, fakePlayer, key, WarehouseDriveOperation.BREAK, "", record -> requested, true);
    }

    public Optional<WarehouseDriveRecord> findActive(WarehouseDriveKey key) { return repository.findActiveByKey(key); }

    public List<WarehouseDriveRecord> listOwned(String playerRef) {
        if (playerRef == null || playerRef.trim().isEmpty()) return Collections.emptyList();
        return repository.findActiveByOwner(playerRef.trim(), localServerId);
    }

    public List<WarehouseDriveReceipt> listRecent(String playerRef, int limit) {
        if (playerRef == null || playerRef.trim().isEmpty()) return Collections.emptyList();
        return repository.findRecentByOwner(playerRef.trim(), localServerId, Math.max(1, Math.min(20, limit)));
    }

    private WarehouseDriveReceipt mutate(String requestId, String playerRef, boolean fakePlayer, WarehouseDriveKey key,
        WarehouseDriveOperation operation, String detail, ResultEvaluator evaluator, boolean removeOnSuccess) {
        final String normalizedRequest = require(requestId, "requestId");
        final String actor = require(playerRef, "playerRef");
        final WarehouseDriveKey target = requireLocal(key);
        final String semantics = operation.name() + "|" + actor + "|" + target;
        return transactionRunner.inTransaction(() -> {
            repository.lockRequest(normalizedRequest);
            Optional<WarehouseDriveReceipt> replay = repository.findReceipt(normalizedRequest);
            if (replay.isPresent()) {
                WarehouseDriveReceipt prior = replay.get();
                return prior.getSemanticsKey().equals(semantics) ? prior
                    : new WarehouseDriveReceipt(normalizedRequest, semantics, operation, WarehouseDriveResult.REQUEST_CONFLICT,
                        target, prior.getBefore(), prior.getAfter(), "request id was already used");
            }
            repository.lockKey(target);
            WarehouseDriveRecord before = repository.findActiveByKey(target).orElse(null);
            WarehouseDriveResult result;
            WarehouseDriveRecord after = before;
            if (fakePlayer) result = WarehouseDriveResult.FAKE_PLAYER_DENIED;
            else if (operation != WarehouseDriveOperation.REGISTER && before == null) result = WarehouseDriveResult.NOT_REGISTERED;
            else if (operation != WarehouseDriveOperation.REGISTER && !actor.equals(before.getOwnerPlayerRef())) result = WarehouseDriveResult.NOT_OWNER;
            else {
                result = evaluator.evaluate(before);
                if (result == WarehouseDriveResult.SUCCESS && operation == WarehouseDriveOperation.REGISTER) {
                    after = new WarehouseDriveRecord(repository.nextDriveId(), target, actor, WarehouseDriveStatus.ACTIVE, 1L);
                    repository.save(after);
                } else if (result == WarehouseDriveResult.SUCCESS && removeOnSuccess) {
                    after = before.remove();
                    repository.save(after);
                }
            }
            WarehouseDriveReceipt receipt = new WarehouseDriveReceipt(normalizedRequest, semantics, operation, result,
                target, before, after, detail);
            repository.saveReceipt(receipt, actor);
            return receipt;
        });
    }

    private WarehouseDriveKey requireLocal(WarehouseDriveKey key) {
        if (key == null) throw new IllegalArgumentException("key is required");
        if (!localServerId.equals(key.getServerId())) throw new IllegalArgumentException("warehouse drive belongs to another server");
        return key;
    }

    private static String require(String value, String field) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    private interface ResultEvaluator { WarehouseDriveResult evaluate(WarehouseDriveRecord record); }
}
