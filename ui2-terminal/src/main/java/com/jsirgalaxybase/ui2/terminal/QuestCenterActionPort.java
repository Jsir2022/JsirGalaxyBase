package com.jsirgalaxybase.ui2.terminal;

public interface QuestCenterActionPort {
    QuestCenterActionPort NONE = new QuestCenterActionPort() {
        @Override public void selectChapter(String chapterId) {}
        @Override public void selectQuest(String questId) {}
        @Override public void changeFilter(String filter) {}
        @Override public void changeQuery(String query) {}
        @Override public void changeChapterPage(int pageIndex) {}
        @Override public void changeQuestPage(int pageIndex) {}
        @Override public void backToBrowse() {}
        @Override public void claim(String questId) {}
        @Override public void selectRewardChoice(String questId,String rewardKey,int choiceIndex) {}
        @Override public void toggleTracking(String questId) {}
        @Override public void openManagement() {}
        @Override public void retry() {}
    };

    void selectChapter(String chapterId);
    void selectQuest(String questId);
    void changeFilter(String filter);
    void changeQuery(String query);
    void changeChapterPage(int pageIndex);
    void changeQuestPage(int pageIndex);
    void backToBrowse();
    void claim(String questId);
    void selectRewardChoice(String questId,String rewardKey,int choiceIndex);
    void toggleTracking(String questId);
    void openManagement();
    void retry();
}
