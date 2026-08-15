package com.jsirgalaxybase.modules.core.vault.port;

import com.jsirgalaxybase.modules.core.vault.domain.VaultAccountType;
import com.jsirgalaxybase.modules.core.vault.domain.VaultPermission;

/** Resolves organization/public Vault membership without coupling storage to the role system. */
public interface VaultAuthorityPort {

    boolean hasPermission(String actorRef, VaultAccountType accountType, String accountRef,
        VaultPermission permission);
}
