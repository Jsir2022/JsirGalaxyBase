package com.jsirgalaxybase.quest.bqimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.jsirgalaxybase.quest.postgres.QuestContentMigrationItem;

public final class BqMigrationDryRun {
    private final String batchId; private final boolean applicable; private final List<QuestContentMigrationItem> items;
    BqMigrationDryRun(String batchId,boolean applicable,List<QuestContentMigrationItem> items){this.batchId=batchId;this.applicable=applicable;this.items=Collections.unmodifiableList(new ArrayList<QuestContentMigrationItem>(items));}
    public String getBatchId(){return batchId;} public boolean isApplicable(){return applicable;} public List<QuestContentMigrationItem> getItems(){return items;}
    public int count(QuestContentMigrationItem.Action action){int count=0;for(QuestContentMigrationItem item:items)if(item.getAction()==action)count++;return count;}
    public String toText(){StringBuilder out=new StringBuilder().append("batchId=").append(batchId).append('\n').append("applicable=").append(applicable).append('\n').append("created=").append(count(QuestContentMigrationItem.Action.CREATED)).append('\n').append("versioned=").append(count(QuestContentMigrationItem.Action.VERSIONED)).append('\n').append("unchanged=").append(count(QuestContentMigrationItem.Action.UNCHANGED)).append('\n');for(QuestContentMigrationItem item:items)out.append(item.getKind()).append('|').append(item.getId()).append('|').append(item.getVersion()).append('|').append(item.getAction()).append('|').append(item.getHash()).append('\n');return out.toString();}
}
