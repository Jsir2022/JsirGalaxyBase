package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.quest.core.RewardDeliveryBatchResult;
import com.jsirgalaxybase.quest.core.RewardDeliveryWorker;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Runs the PostgreSQL lease worker on the server thread; handlers may safely touch player state. */
public final class QuestRewardDeliveryController {
    private static final int INTERVAL_TICKS=20;
    private final RewardDeliveryWorker worker;private final String workerId;private int remaining=INTERVAL_TICKS;
    public QuestRewardDeliveryController(RewardDeliveryWorker worker,String workerId){if(worker==null||workerId==null||workerId.trim().isEmpty())throw new IllegalArgumentException("worker and workerId are required");this.worker=worker;this.workerId=workerId;}
    @SubscribeEvent public void onServerTick(TickEvent.ServerTickEvent event){if(event.phase!=TickEvent.Phase.END||--remaining>0)return;remaining=INTERVAL_TICKS;
        try{RewardDeliveryBatchResult result=worker.runOnce(workerId,16);if(result.getLeased()>0)GalaxyBase.LOG.info("Quest reward delivery {} leased={}, delivered={}, retry={}, abandoned={}, stale={}",workerId,result.getLeased(),result.getDelivered(),result.getRetryScheduled(),result.getAbandoned(),result.getStaleConfirmations());}
        catch(RuntimeException failure){GalaxyBase.LOG.error("Quest reward delivery worker failed: "+workerId,failure);}}
}
