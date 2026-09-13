package com.jsirgalaxybase.quest.bqimport;

/** Stable relationship between an imported BQ progress record and Base progress. */
public enum BqShadowRelation {
    IDENTICAL,
    BASE_AHEAD,
    BQ_AHEAD,
    DIVERGENT,
    BQ_ONLY,
    BASE_ONLY,
    INCOMPARABLE
}
