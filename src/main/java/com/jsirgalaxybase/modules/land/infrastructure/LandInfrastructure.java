package com.jsirgalaxybase.modules.land.infrastructure;

import com.jsirgalaxybase.modules.land.port.LandTransactionRunner;
import com.jsirgalaxybase.modules.land.port.PersonalLandRepository;

public final class LandInfrastructure {

    private final PersonalLandRepository repository;
    private final LandTransactionRunner transactionRunner;

    public LandInfrastructure(PersonalLandRepository repository, LandTransactionRunner transactionRunner) {
        this.repository = repository;
        this.transactionRunner = transactionRunner;
    }

    public PersonalLandRepository getRepository() {
        return repository;
    }

    public LandTransactionRunner getTransactionRunner() {
        return transactionRunner;
    }
}
