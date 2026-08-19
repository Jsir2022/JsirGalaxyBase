package com.jsirgalaxybase.modules.core.vault.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Server-counted page of exceptional operations for one authenticated Vault account. */
public final class VaultOperationHistoryPage {
    private final List<VaultOperation> operations;
    private final int totalEntries;
    private final int pageIndex;
    private final int pageSize;

    public VaultOperationHistoryPage(List<VaultOperation> operations, int totalEntries, int pageIndex, int pageSize) {
        this.operations = Collections.unmodifiableList(new ArrayList<VaultOperation>(
            operations == null ? Collections.<VaultOperation>emptyList() : operations));
        this.totalEntries = Math.max(0, totalEntries);
        this.pageIndex = Math.max(0, pageIndex);
        this.pageSize = Math.max(1, pageSize);
    }

    public List<VaultOperation> getOperations() { return operations; }
    public int getTotalEntries() { return totalEntries; }
    public int getPageIndex() { return pageIndex; }
    public int getPageSize() { return pageSize; }
}
