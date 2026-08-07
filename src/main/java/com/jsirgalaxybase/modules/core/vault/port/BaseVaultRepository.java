package com.jsirgalaxybase.modules.core.vault.port;

import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.vault.domain.VaultAccount;
import com.jsirgalaxybase.modules.core.vault.domain.VaultAccountType;
import com.jsirgalaxybase.modules.core.vault.domain.VaultOperation;
import com.jsirgalaxybase.modules.core.vault.domain.VaultOperationSlotChange;
import com.jsirgalaxybase.modules.core.vault.domain.VaultSlot;

public interface BaseVaultRepository {

    VaultAccount ensureAccount(VaultAccountType accountType, String accountRef);

    VaultAccount lockAccount(VaultAccountType accountType, String accountRef);

    List<VaultSlot> findSlots(long accountId);

    void saveSlot(long accountId, VaultSlot slot);

    Optional<VaultOperation> findOperationByRequestId(String requestId);

    VaultOperation saveOperation(VaultOperation operation);

    VaultOperation updateOperation(VaultOperation operation);

    void saveOperationSlotChanges(long operationId, List<VaultOperationSlotChange> changes);
}
