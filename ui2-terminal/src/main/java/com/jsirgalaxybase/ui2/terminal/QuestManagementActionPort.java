package com.jsirgalaxybase.ui2.terminal;

public interface QuestManagementActionPort {
    QuestManagementActionPort NONE=new QuestManagementActionPort(){
        public void back(){}
        public void openChapters(){}
        public void filter(String lifecycle){}
        public void page(int page){}
        public void select(String id,int version){}
        public void closeDetail(){}
        public void createDraft(String template,String name){}
        public void publish(String id,int version,String hash){}
        public void retire(String id,int version){}
        public void saveBasics(String id,int version,String hash,String name,String description,String prerequisiteLogic,String taskLogic){}
        public void savePrerequisites(String id,int version,String hash,java.util.List<String> original,java.util.List<String> selected){}
        public void saveOptions(String id,int version,String hash,java.util.Map<String,String> original,java.util.Map<String,String> selected){}
        public void saveElement(String id,int version,String hash,boolean task,String originalKey,String key,String typeId,boolean optional,java.util.Map<String,String> parameters){}
        public void deleteElement(String id,int version,String hash,boolean task,String key){}
        public void moveElement(String id,int version,String hash,boolean task,String key,int index){}
    };
    void back();
    void openChapters();
    void filter(String lifecycle);
    void page(int page);
    void select(String id,int version);
    void closeDetail();
    void createDraft(String template,String name);
    void publish(String id,int version,String expectedHash);
    void retire(String id,int version);
    /** Sends one server-authoritative batch intent; entries are id/version pairs in parallel order. */
    default void batchRetire(java.util.List<String> ids,java.util.List<Integer> versions) {}
    void saveBasics(String id,int version,String expectedHash,String name,String description,String prerequisiteLogic,String taskLogic);
    void savePrerequisites(String id,int version,String expectedHash,java.util.List<String> original,java.util.List<String> selected);
    void saveOptions(String id,int version,String expectedHash,java.util.Map<String,String> original,java.util.Map<String,String> selected);
    void saveElement(String id,int version,String expectedHash,boolean task,String originalKey,String key,String typeId,boolean optional,java.util.Map<String,String> parameters);
    void deleteElement(String id,int version,String expectedHash,boolean task,String key);
    void moveElement(String id,int version,String expectedHash,boolean task,String key,int index);
}
