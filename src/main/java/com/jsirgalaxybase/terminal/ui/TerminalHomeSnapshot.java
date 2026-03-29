package com.jsirgalaxybase.terminal.ui;

public class TerminalHomeSnapshot {

    private final String career;
    private final int contribution;
    private final String reputation;
    private final String publicTasks;
    private final String marketSummary;

    public TerminalHomeSnapshot(String career, int contribution, String reputation, String publicTasks,
        String marketSummary) {
        this.career = career;
        this.contribution = contribution;
        this.reputation = reputation;
        this.publicTasks = publicTasks;
        this.marketSummary = marketSummary;
    }

    public String getCareer() {
        return career;
    }

    public int getContribution() {
        return contribution;
    }

    public String getReputation() {
        return reputation;
    }

    public String getPublicTasks() {
        return publicTasks;
    }

    public String getMarketSummary() {
        return marketSummary;
    }
}