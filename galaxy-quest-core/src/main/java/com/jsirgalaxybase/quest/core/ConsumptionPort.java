package com.jsirgalaxybase.quest.core;

/** Must apply each stable submission key at most once, including after process restart. */
public interface ConsumptionPort {
    ConsumptionApplyResult apply(ConsumptionRequest request);
}
