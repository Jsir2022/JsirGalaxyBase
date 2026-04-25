package com.jsirgalaxybase.modules.core.market.application;

import java.util.Optional;
import java.util.UUID;

import com.jsirgalaxybase.modules.core.banking.application.BankPostingResult;
import com.jsirgalaxybase.modules.core.banking.application.BankingApplicationService;
import com.jsirgalaxybase.modules.core.banking.application.BankingConstants;
import com.jsirgalaxybase.modules.core.banking.application.command.CoinExchangeSettlementCommand;
import com.jsirgalaxybase.modules.core.banking.domain.BankAccount;
import com.jsirgalaxybase.modules.core.banking.infrastructure.BankingInfrastructure;
import com.jsirgalaxybase.modules.core.banking.infrastructure.ManagedBankAccounts;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketExecutionRequest;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketExecutionResult;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketLimitPolicy;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketLimitStatus;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketPairDefinition;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketQuoteRequest;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketQuoteResult;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketRuleVersion;
import com.jsirgalaxybase.modules.core.market.domain.TaskCoinDescriptor;
import com.jsirgalaxybase.modules.core.market.domain.TaskCoinExchangeQuote;

public class ExchangeMarketService {

    public static final String INPUT_ASSET_CODE_TASK_COIN = "TASK_COIN";
    public static final String OUTPUT_ASSET_CODE_STARCOIN = BankingConstants.DEFAULT_CURRENCY_CODE;
    public static final ExchangeMarketPairDefinition TASK_COIN_TO_STARCOIN = new ExchangeMarketPairDefinition(
        "task-coin-to-starcoin", INPUT_ASSET_CODE_TASK_COIN, OUTPUT_ASSET_CODE_STARCOIN, "任务书硬币", "星光币");
    private static final String DEFAULT_NOTES = "当前为汇率市场 v1 固定规则报价源；兼容旧 market quote/exchange hand 入口。";

    private final BankingInfrastructure bankingInfrastructure;
    private final BankingApplicationService bankingService;
    private final TaskCoinExchangePlanner taskCoinPlanner;

    public ExchangeMarketService(BankingInfrastructure bankingInfrastructure, String sourceServerId) {
        this(bankingInfrastructure, new TaskCoinExchangePlanner(), sourceServerId);
    }

    public ExchangeMarketService(BankingInfrastructure bankingInfrastructure, TaskCoinExchangePlanner taskCoinPlanner,
        String sourceServerId) {
        this.bankingInfrastructure = bankingInfrastructure;
        this.bankingService = bankingInfrastructure.getBankingApplicationService();
        this.taskCoinPlanner = taskCoinPlanner;
    }

    public Optional<ExchangeMarketQuoteResult> quoteTaskCoinToStarcoin(ExchangeMarketQuoteRequest request) {
        if (request == null || request.getInputQuantity() <= 0L) {
            return Optional.empty();
        }

        Optional<TaskCoinExchangeQuote> legacyQuote = taskCoinPlanner.quote(request.getInputRegistryName(),
            normalizeQuantity(request.getInputQuantity()));
        if (!legacyQuote.isPresent() && taskCoinPlanner.isUnsupportedTaskCoinTier(request.getInputRegistryName())) {
            return Optional.of(buildDisallowedQuote(request, "TASK_COIN_TIER_DISALLOWED",
                "当前汇率市场 v1 只支持 BASE / I / II / III / IV 任务书硬币。"));
        }
        if (!legacyQuote.isPresent()) {
            return Optional.of(buildDisallowedQuote(request, "TASK_COIN_ASSET_UNSUPPORTED",
                "当前手持物品不属于汇率市场支持的任务书硬币资产对。"));
        }

        return Optional.of(toFormalQuote(legacyQuote.get(), request));
    }

    public ExchangeMarketExecutionResult executeTaskCoinToStarcoin(ExchangeMarketExecutionRequest request) {
        requireText(request == null ? null : request.getRequestId(), "requestId");
        requireText(request == null ? null : request.getPlayerRef(), "playerRef");
        requireText(request == null ? null : request.getSourceServerId(), "sourceServerId");
        requireText(request == null ? null : request.getOperatorRef(), "operatorRef");
        if (request == null || request.getInputQuantity() <= 0L) {
            throw new MarketExchangeException("兑换数量必须大于 0");
        }

        ExchangeMarketQuoteResult quote = quoteTaskCoinToStarcoin(new ExchangeMarketQuoteRequest(
            request.getRequestId(), request.getPlayerRef(), request.getSourceServerId(), request.getInputRegistryName(),
            request.getInputQuantity())).orElseThrow(new java.util.function.Supplier<MarketExchangeException>() {

                @Override
                public MarketExchangeException get() {
                    return new MarketExchangeException("当前物品不属于汇率市场支持的任务书硬币资产对");
                }
            });
        if (!quote.getLimitPolicy().isExecutable()) {
            throw new MarketExchangeException(quote.getLimitPolicy().getNote());
        }

        Optional<BankAccount> playerAccount = bankingService.findAccount(BankingConstants.OWNER_TYPE_PLAYER_UUID,
            request.getPlayerRef(), BankingConstants.DEFAULT_CURRENCY_CODE);
        if (!playerAccount.isPresent()) {
            throw new MarketExchangeException("请先开户后再进入汇率市场兑换入口");
        }

        BankAccount reserveAccount = ManagedBankAccounts.ensureManagedAccount(bankingInfrastructure,
            ManagedBankAccounts.EXCHANGE_RESERVE_ACCOUNT);
        if (reserveAccount.getAvailableBalance() < quote.getEffectiveExchangeValue()) {
            throw new MarketExchangeException("兑换储备余额不足，请稍后再试");
        }

        BankPostingResult postingResult = bankingService.settleCoinExchange(new CoinExchangeSettlementCommand(
            request.getRequestId(), reserveAccount.getAccountId(), playerAccount.get().getAccountId(),
            request.getPlayerRef(), request.getSourceServerId(), request.getOperatorRef(), quote.getInputFamily(),
            quote.getInputTier(), quote.getInputUnitFaceValue(), quote.getInputQuantity(),
            quote.getEffectiveExchangeValue(), quote.getContributionValue(), quote.getRuleVersion().getRuleKey(),
            buildBusinessRef(quote), "exchange market task coin settlement", buildAuditJson(quote)));
        return new ExchangeMarketExecutionResult(quote, postingResult);
    }

    public String newRequestId() {
        return "exchange-market-" + UUID.randomUUID().toString();
    }

    private ExchangeMarketQuoteResult toFormalQuote(TaskCoinExchangeQuote legacyQuote,
        ExchangeMarketQuoteRequest request) {
        TaskCoinDescriptor descriptor = legacyQuote.getDescriptor();
        ExchangeMarketLimitStatus limitStatus = legacyQuote.getEffectiveExchangeValue() == legacyQuote.getInputTotalFaceValue()
            ? ExchangeMarketLimitStatus.ALLOWED
            : ExchangeMarketLimitStatus.DISCOUNTED;
        ExchangeMarketLimitPolicy limitPolicy = new ExchangeMarketLimitPolicy(limitStatus, "TASK_COIN_RULE_APPLIED",
            DEFAULT_NOTES);
        return new ExchangeMarketQuoteResult(TASK_COIN_TO_STARCOIN,
            new ExchangeMarketRuleVersion(legacyQuote.getExchangeRuleVersion(), "汇率市场任务书硬币固定规则 v1"),
            limitPolicy, request.getRequestId(), request.getPlayerRef(), request.getSourceServerId(),
            descriptor.getRegistryName(), descriptor.getFamily(), descriptor.getTier(), descriptor.getFaceValue(),
            legacyQuote.getQuantity(), legacyQuote.getInputTotalFaceValue(), legacyQuote.getEffectiveExchangeValue(),
            legacyQuote.getContributionValue(), legacyQuote.getEffectiveExchangeValue(), legacyQuote.getInputQuantity(),
            legacyQuote.getDiscountBasisPoints(), DEFAULT_NOTES);
    }

    private ExchangeMarketQuoteResult buildDisallowedQuote(ExchangeMarketQuoteRequest request, String reasonCode,
        String note) {
        return new ExchangeMarketQuoteResult(TASK_COIN_TO_STARCOIN,
            new ExchangeMarketRuleVersion(TaskCoinExchangePlanner.RULE_VERSION, "汇率市场任务书硬币固定规则 v1"),
            new ExchangeMarketLimitPolicy(ExchangeMarketLimitStatus.DISALLOWED, reasonCode, note),
            request.getRequestId(), request.getPlayerRef(), request.getSourceServerId(), request.getInputRegistryName(),
            "UNRESOLVED", "UNRESOLVED", 0L, request.getInputQuantity(), 0L, 0L, 0L, 0L, 1L, 10000, note);
    }

    private int normalizeQuantity(long quantity) {
        if (quantity > Integer.MAX_VALUE) {
            throw new MarketExchangeException("当前汇率市场单次兑换数量超出支持范围");
        }
        return (int) quantity;
    }

    private String buildBusinessRef(ExchangeMarketQuoteResult quote) {
        return "exchange-market:" + quote.getPairDefinition().getPairCode() + ":" + quote.getPlayerRef() + ":"
            + quote.getInputFamily() + ":" + quote.getInputTier();
    }

    private String buildAuditJson(ExchangeMarketQuoteResult quote) {
        return "{"
            + jsonField("marketType", "exchange") + ","
            + jsonField("pairCode", quote.getPairDefinition().getPairCode()) + ","
            + jsonField("inputAssetCode", quote.getPairDefinition().getInputAssetCode()) + ","
            + jsonField("outputAssetCode", quote.getPairDefinition().getOutputAssetCode()) + ","
            + jsonField("requestId", quote.getRequestId()) + ","
            + jsonField("playerRef", quote.getPlayerRef()) + ","
            + jsonField("sourceServerId", quote.getSourceServerId()) + ","
            + jsonField("ruleVersion", quote.getRuleVersion().getRuleKey()) + ","
            + jsonField("reasonCode", quote.getLimitPolicy().getReasonCode()) + ","
            + jsonField("notes", quote.getNotes()) + ","
            + jsonField("inputRegistryName", quote.getInputRegistryName()) + ","
            + jsonField("limitStatus", quote.getLimitPolicy().getStatus().name()) + ","
            + "\"discountBasisPoints\":" + quote.getDiscountBasisPoints() + ","
            + "\"effectiveExchangeValue\":" + quote.getEffectiveExchangeValue() + ","
            + "\"contributionValue\":" + quote.getContributionValue()
            + "}";
    }

    private String jsonField(String name, String value) {
        return "\"" + escapeJson(name) + "\":\"" + escapeJson(value) + "\"";
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String requireText(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            throw new MarketExchangeException(fieldName + " must not be blank");
        }
        return text.trim();
    }
}