package com.jsirgalaxybase.modules.core.market.infrastructure.jdbc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JdbcMarketOrderBookRepositoryTest {

    @Test
    public void likeSearchEscapesPostgresWildcardCharacters() {
        assertEquals("iron", JdbcMarketOrderBookRepository.escapeLike("iron"));
        assertEquals("100!%", JdbcMarketOrderBookRepository.escapeLike("100%"));
        assertEquals("steel!_plate", JdbcMarketOrderBookRepository.escapeLike("steel_plate"));
        assertEquals("alert!!", JdbcMarketOrderBookRepository.escapeLike("alert!"));
    }
}
