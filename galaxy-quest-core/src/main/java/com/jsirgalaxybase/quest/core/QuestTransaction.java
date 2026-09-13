package com.jsirgalaxybase.quest.core;

public interface QuestTransaction {
    <T> T inTransaction(Work<T> work);

    interface Work<T> {
        T execute();
    }
}
