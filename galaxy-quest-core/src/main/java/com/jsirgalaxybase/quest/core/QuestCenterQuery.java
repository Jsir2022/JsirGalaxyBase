package com.jsirgalaxybase.quest.core;

public interface QuestCenterQuery {
    QuestCenterSnapshot load(ParticipantId participantId);

    /** Returns a bounded terminal read model; identity remains supplied by the authenticated server caller. */
    default QuestCenterPage loadPage(ParticipantId participantId, QuestCenterPageRequest request) {
        return QuestCenterPaginator.page(load(participantId), request);
    }
}
