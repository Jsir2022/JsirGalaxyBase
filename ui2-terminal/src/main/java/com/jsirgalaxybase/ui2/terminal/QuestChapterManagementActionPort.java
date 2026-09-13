package com.jsirgalaxybase.ui2.terminal;

public interface QuestChapterManagementActionPort {
    QuestChapterManagementActionPort NONE = new QuestChapterManagementActionPort() {
        public void back() {} public void filter(String value) {} public void page(int value) {}
        public void select(String id, int version) {} public void closeDetail() {} public void create(String name) {}
        public void publish(String id, int version, String hash) {} public void retire(String id, int version) {}
        public void saveBasics(String id, int version, String hash, String name, String description, String icon,
            String background, int backgroundSize, String visibility) {}
        public void place(String id, int version, String hash, String questId, int x, int y, int width, int height) {}
        public void move(String id, int version, String hash, String questId, int x, int y) {}
        public void resize(String id, int version, String hash, String questId, int width, int height) {}
        public void remove(String id, int version, String hash, String questId) {}
        public void removeEntries(String id,int version,String hash,java.util.List<String> questIds) {}
        public void cloneEntries(String id, int version, String hash, java.util.List<String> questIds, int anchorX, int anchorY) {}
        public void align(String id,int version,String hash,java.util.List<String> questIds,String alignment) {}
        public void moveBefore(String chapterId, String beforeChapterId) {}
    };
    void back(); void filter(String value); void page(int value); void select(String id, int version);
    void closeDetail(); void create(String name); void publish(String id, int version, String hash);
    void retire(String id, int version);
    void saveBasics(String id, int version, String hash, String name, String description, String icon,
        String background, int backgroundSize, String visibility);
    void place(String id, int version, String hash, String questId, int x, int y, int width, int height);
    void move(String id, int version, String hash, String questId, int x, int y);
    void resize(String id, int version, String hash, String questId, int width, int height);
    void remove(String id, int version, String hash, String questId);
    void removeEntries(String id,int version,String hash,java.util.List<String> questIds);
    void cloneEntries(String id, int version, String hash, java.util.List<String> questIds, int anchorX, int anchorY);
    void align(String id,int version,String hash,java.util.List<String> questIds,String alignment);
    void moveBefore(String chapterId, String beforeChapterId);
}
