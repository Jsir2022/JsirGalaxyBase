package com.jsirgalaxybase.modules.core.vault.application;

import com.jsirgalaxybase.modules.core.vault.domain.VaultAccountType;
import com.jsirgalaxybase.modules.core.vault.domain.VaultPermission;
import com.jsirgalaxybase.modules.core.vault.port.VaultAuthorityPort;

/**
 * Authorization boundary for player-driven Vault access. Trusted deliveries
 * from markets and rewards continue to use {@link BaseVaultService} directly.
 */
public final class VaultAccessService {

    private static final VaultAuthorityPort DENY_ORGANIZATION_ACCESS = new VaultAuthorityPort() {
        @Override
        public boolean hasPermission(String actorRef, VaultAccountType accountType, String accountRef,
            VaultPermission permission) {
            return false;
        }
    };

    private final BaseVaultService vaultService;
    private final VaultAuthorityPort authorityPort;

    public VaultAccessService(BaseVaultService vaultService) {
        this(vaultService, DENY_ORGANIZATION_ACCESS);
    }

    public VaultAccessService(BaseVaultService vaultService, VaultAuthorityPort authorityPort) {
        if (vaultService == null) {
            throw new VaultException("base vault service is required");
        }
        this.vaultService = vaultService;
        this.authorityPort = authorityPort == null ? DENY_ORGANIZATION_ACCESS : authorityPort;
    }

    public boolean canAccess(String actorRef, VaultAccountType accountType, String accountRef,
        VaultPermission permission) {
        String actor = requireText(actorRef, "actorRef");
        String account = requireText(accountRef, "accountRef");
        if (accountType == null || permission == null) {
            return false;
        }
        if (accountType == VaultAccountType.PERSONAL) {
            return actor.equals(account);
        }
        return authorityPort.hasPermission(actor, accountType, account, permission);
    }

    public void requireAccess(String actorRef, VaultAccountType accountType, String accountRef,
        VaultPermission permission) {
        if (!canAccess(actorRef, accountType, accountRef, permission)) {
            throw new VaultAccessDeniedException("Vault access denied: " + accountType + " " + permission);
        }
    }

    public BaseVaultService.VaultView view(String actorRef, VaultAccountType accountType, String accountRef) {
        requireAccess(actorRef, accountType, accountRef, VaultPermission.VIEW);
        return vaultService.viewVault(accountType, accountRef);
    }

    public BaseVaultService.VaultSortResult sort(String requestId, String actorRef, VaultAccountType accountType,
        String accountRef) {
        requireAccess(actorRef, accountType, accountRef, VaultPermission.SORT);
        return vaultService.sortVault(requestId, accountType, accountRef);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new VaultException(field + " is required");
        }
        return value.trim();
    }
}
