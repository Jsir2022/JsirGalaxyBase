package com.jsirgalaxybase.quest.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Aggregated entitlement state for one quest version and progress cycle. */
public final class QuestRewardSummary {
    public static final QuestRewardSummary EMPTY = new QuestRewardSummary(0, 0, 0, 0, 0, 0);
    private final int total, claimable, pending, delivering, delivered, failed;
    private final Map<String,Integer> selectedChoices;

    public QuestRewardSummary(int total,int claimable,int pending,int delivering,int delivered,int failed){
        this(total,claimable,pending,delivering,delivered,failed,Collections.<String,Integer>emptyMap());
    }
    public QuestRewardSummary(int total,int claimable,int pending,int delivering,int delivered,int failed,
        Map<String,Integer> selectedChoices){
        this.total=nonNegative(total);this.claimable=nonNegative(claimable);this.pending=nonNegative(pending);
        this.delivering=nonNegative(delivering);this.delivered=nonNegative(delivered);this.failed=nonNegative(failed);
        this.selectedChoices=Collections.unmodifiableMap(new LinkedHashMap<String,Integer>(selectedChoices==null
            ?Collections.<String,Integer>emptyMap():selectedChoices));
        if(this.claimable+this.pending+this.delivering+this.delivered+this.failed>this.total)
            throw new IllegalArgumentException("reward status counts exceed total");
    }
    private static int nonNegative(int value){if(value<0)throw new IllegalArgumentException("counts must be non-negative");return value;}
    public int getTotal(){return total;} public int getClaimable(){return claimable;} public int getPending(){return pending;}
    public int getDelivering(){return delivering;} public int getDelivered(){return delivered;} public int getFailed(){return failed;}
    public boolean hasClaimable(){return claimable>0;} public boolean isFullyDelivered(){return total>0&&delivered==total;}
    public int getSelectedChoice(String rewardKey){Integer value=selectedChoices.get(rewardKey);return value==null?-1:value.intValue();}
    public Map<String,Integer> getSelectedChoices(){return selectedChoices;}
}
