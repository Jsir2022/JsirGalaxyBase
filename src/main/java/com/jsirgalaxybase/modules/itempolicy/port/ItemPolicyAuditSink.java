package com.jsirgalaxybase.modules.itempolicy.port;

import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyScope;

public interface ItemPolicyAuditSink {

    void recordDenied(String sourceServerId, String actorRef, ItemPolicyScope scope, String operation,
        String registryName, int meta, String ruleId);
}
