package com.jsirgalaxybase.quest.core;

import java.util.UUID;

/** Builds the mixed PLAYER/PARTY/TEAM/PUBLIC read model for an authenticated player. */
public interface AuthenticatedQuestCenterQuery extends QuestCenterQuery {
    QuestCenterSnapshot loadForPlayer(UUID authenticatedPlayerId);

    default QuestCenterPage loadPageForPlayer(UUID authenticatedPlayerId, QuestCenterPageRequest request) {
        return QuestCenterPaginator.page(loadForPlayer(authenticatedPlayerId), request);
    }
}
