package com.jsirgalaxybase.modules.itempolicy.port;

import java.util.List;
import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyAuditRecord;

public interface ItemPolicyAuditQuery {
    List<ItemPolicyAuditRecord> listRecentForActor(String sourceServerId, String actorRef, int limit);
}
