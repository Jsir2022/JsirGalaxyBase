package com.jsirgalaxybase.terminal;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the server-side half of the exchange market's second confirmation.
 * Client payloads select a target only; they never assert that a quote was confirmed.
 */
final class TerminalExchangeQuoteConfirmationGate {

    private static final long CONFIRMATION_TTL_MILLIS = 120000L;

    private final Map<String, PendingQuote> pendingBySession = new ConcurrentHashMap<String, PendingQuote>();

    void register(String playerRef, String sessionToken, TerminalExchangeMarketActionPayload payload,
        TerminalExchangeMarketSectionSnapshot quote) {
        cleanupExpired(System.currentTimeMillis());
        String key = key(playerRef, sessionToken);
        if (key == null || !isExecutableSelection(payload, quote)) {
            if (key != null) {
                pendingBySession.remove(key);
            }
            return;
        }
        pendingBySession.put(key, PendingQuote.from(payload, quote, System.currentTimeMillis() + CONFIRMATION_TTL_MILLIS));
    }

    boolean consumeIfCurrent(String playerRef, String sessionToken, TerminalExchangeMarketActionPayload payload,
        TerminalExchangeMarketSectionSnapshot currentQuote) {
        cleanupExpired(System.currentTimeMillis());
        String key = key(playerRef, sessionToken);
        if (key == null || !isExecutableSelection(payload, currentQuote)) {
            return false;
        }
        PendingQuote pending = pendingBySession.remove(key);
        return pending != null && pending.matches(payload, currentQuote);
    }

    void clear(String playerRef, String sessionToken) {
        String key = key(playerRef, sessionToken);
        if (key != null) {
            pendingBySession.remove(key);
        }
    }

    private boolean isExecutableSelection(TerminalExchangeMarketActionPayload payload,
        TerminalExchangeMarketSectionSnapshot quote) {
        return payload != null && payload.hasSelectedTarget() && quote != null && quote.isExecutable()
            && payload.getSelectedTargetCode().equals(quote.getSelectedTargetCode());
    }

    private String key(String playerRef, String sessionToken) {
        if (blank(playerRef) || blank(sessionToken)) {
            return null;
        }
        return playerRef.trim() + "|" + sessionToken.trim();
    }

    private void cleanupExpired(long now) {
        Iterator<Map.Entry<String, PendingQuote>> iterator = pendingBySession.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, PendingQuote> entry = iterator.next();
            if (entry.getValue().expiresAtMillis <= now) {
                pendingBySession.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class PendingQuote {

        private final String targetCode;
        private final String inputRegistryName;
        private final String inputQuantity;
        private final String pairCode;
        private final String ruleVersion;
        private final String limitStatus;
        private final String effectiveExchangeValue;
        private final String rateDisplay;
        private final long expiresAtMillis;

        private PendingQuote(String targetCode, String inputRegistryName, String inputQuantity, String pairCode,
            String ruleVersion, String limitStatus, String effectiveExchangeValue, String rateDisplay,
            long expiresAtMillis) {
            this.targetCode = targetCode;
            this.inputRegistryName = inputRegistryName;
            this.inputQuantity = inputQuantity;
            this.pairCode = pairCode;
            this.ruleVersion = ruleVersion;
            this.limitStatus = limitStatus;
            this.effectiveExchangeValue = effectiveExchangeValue;
            this.rateDisplay = rateDisplay;
            this.expiresAtMillis = expiresAtMillis;
        }

        static PendingQuote from(TerminalExchangeMarketActionPayload payload,
            TerminalExchangeMarketSectionSnapshot quote, long expiresAtMillis) {
            return new PendingQuote(payload.getSelectedTargetCode(), quote.getInputRegistryName(),
                quote.getInputQuantity(), quote.getPairCode(), quote.getRuleVersion(), quote.getLimitStatus(),
                quote.getEffectiveExchangeValue(), quote.getRateDisplay(), expiresAtMillis);
        }

        boolean matches(TerminalExchangeMarketActionPayload payload, TerminalExchangeMarketSectionSnapshot quote) {
            return targetCode.equals(payload.getSelectedTargetCode())
                && inputRegistryName.equals(quote.getInputRegistryName())
                && inputQuantity.equals(quote.getInputQuantity())
                && pairCode.equals(quote.getPairCode())
                && ruleVersion.equals(quote.getRuleVersion())
                && limitStatus.equals(quote.getLimitStatus())
                && effectiveExchangeValue.equals(quote.getEffectiveExchangeValue())
                && rateDisplay.equals(quote.getRateDisplay());
        }
    }
}
