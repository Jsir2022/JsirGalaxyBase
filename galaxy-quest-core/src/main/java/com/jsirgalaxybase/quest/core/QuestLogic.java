package com.jsirgalaxybase.quest.core;

public enum QuestLogic {
    AND,
    NAND,
    OR,
    NOR,
    XOR,
    XNOR;

    public boolean satisfied(int completed, int total) {
        if (total <= 0) {
            return true;
        }
        switch (this) {
            case AND: return completed >= total;
            case NAND: return completed < total;
            case OR: return completed > 0;
            case NOR: return completed == 0;
            case XOR: return completed == 1;
            case XNOR: return completed == total - 1;
            default: throw new IllegalStateException("unsupported quest logic " + this);
        }
    }
}
