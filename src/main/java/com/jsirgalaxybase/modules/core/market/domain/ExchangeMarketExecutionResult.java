package com.jsirgalaxybase.modules.core.market.domain;

import com.jsirgalaxybase.modules.core.banking.application.BankPostingResult;

public class ExchangeMarketExecutionResult {

    private final ExchangeMarketQuoteResult quoteResult;
    private final BankPostingResult postingResult;

    public ExchangeMarketExecutionResult(ExchangeMarketQuoteResult quoteResult, BankPostingResult postingResult) {
        this.quoteResult = quoteResult;
        this.postingResult = postingResult;
    }

    public ExchangeMarketQuoteResult getQuoteResult() {
        return quoteResult;
    }

    public BankPostingResult getPostingResult() {
        return postingResult;
    }
}