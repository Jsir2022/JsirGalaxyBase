package com.jsirgalaxybase.modules.core.banking.repository;

import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;

public interface BankAccountRepository {

    Optional<BankAccount> findById(long accountId);

    Optional<BankAccount> findByOwner(String ownerType, String ownerRef, String currencyCode);

    BankAccount save(BankAccount account);

    Optional<BankAccount> saveIfOwnerAbsent(BankAccount account);

    BankAccount lockById(long accountId);

    List<BankAccount> lockByIdsInOrder(List<Long> accountIds);

    void updateBalances(long accountId, long availableBalance, long frozenBalance, long expectedVersion);
}