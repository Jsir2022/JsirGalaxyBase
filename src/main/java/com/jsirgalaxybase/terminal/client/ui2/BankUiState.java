package com.jsirgalaxybase.terminal.client.ui2;

import com.jsirgalaxybase.terminal.TerminalBankActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalBankSectionModel;

/** Immutable local draft for the bank page. Account data remains server authoritative. */
public final class BankUiState {
    public enum Field { NONE, TARGET, AMOUNT, COMMENT }

    private final String target;
    private final String amount;
    private final String comment;
    private final Field field;
    private final boolean helpOpen;
    private final boolean confirmOpen;

    public BankUiState(String target, String amount, String comment, Field field,
        boolean helpOpen, boolean confirmOpen) {
        this.target = player(target);
        this.amount = amount(amount);
        this.comment = comment(comment);
        this.field = field == null ? Field.NONE : field;
        this.helpOpen = helpOpen;
        this.confirmOpen = confirmOpen;
    }

    public static BankUiState initial(TerminalBankSectionModel.TransferFormModel form) {
        TerminalBankSectionModel.TransferFormModel value = form == null
            ? TerminalBankSectionModel.TransferFormModel.placeholder() : form;
        return new BankUiState(value.getTargetPlayerName(), value.getAmountText(), value.getComment(),
            Field.NONE, false, false);
    }

    public String getTarget() { return target; }
    public String getAmount() { return amount; }
    public String getComment() { return comment; }
    public Field getField() { return field; }
    public boolean isHelpOpen() { return helpOpen; }
    public boolean isConfirmOpen() { return confirmOpen; }
    public boolean isComplete() { return !target.isEmpty() && payload().parseAmount() > 0L; }
    public TerminalBankActionPayload payload() { return new TerminalBankActionPayload(target, amount, comment); }

    public BankUiState withField(Field value) {
        return new BankUiState(target, amount, comment, value, helpOpen, confirmOpen);
    }
    public BankUiState withValue(Field targetField, String value) {
        return new BankUiState(targetField == Field.TARGET ? value : target,
            targetField == Field.AMOUNT ? value : amount,
            targetField == Field.COMMENT ? value : comment, targetField, helpOpen, confirmOpen);
    }
    public BankUiState withHelp(boolean value) {
        return new BankUiState(target, amount, comment, field, value, false);
    }
    public BankUiState withConfirm(boolean value) {
        return new BankUiState(target, amount, comment, field, false, value);
    }

    private static String player(String value) {
        String source = value == null ? "" : value.trim();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < source.length() && result.length() < 16; i++) {
            char c = source.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9') || c == '_') result.append(c);
        }
        return result.toString();
    }
    private static String amount(String value) {
        String source = value == null ? "" : value.trim();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < source.length() && result.length() < 18; i++) {
            char c = source.charAt(i);
            if (c >= '0' && c <= '9') result.append(c);
        }
        return result.toString();
    }
    private static String comment(String value) {
        String source = value == null ? "" : value.trim();
        return source.length() > 96 ? source.substring(0, 96) : source;
    }
}
