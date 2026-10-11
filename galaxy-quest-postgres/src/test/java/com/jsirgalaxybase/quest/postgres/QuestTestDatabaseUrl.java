package com.jsirgalaxybase.quest.postgres;

import java.net.URI;

/** Fail-closed guard for integration tests that create and drop schemas. */
final class QuestTestDatabaseUrl {
    private QuestTestDatabaseUrl() { }

    static boolean isExplicitlyIsolated(String url, String isolationAcknowledged) {
        if (!"true".equalsIgnoreCase(isolationAcknowledged) || url == null) return false;
        try {
            if (!url.startsWith("jdbc:postgresql://")) return false;
            URI uri = URI.create(url.substring("jdbc:".length()));
            String host = uri.getHost();
            String path = uri.getPath();
            if (host == null || path == null || host.equalsIgnoreCase("galaxy-base")) return false;
            String database = path.substring(path.lastIndexOf('/') + 1);
            return database.matches("(?i).*(?:_test|_it)(?:_[a-z0-9]+)?$");
        } catch (RuntimeException invalid) {
            return false;
        }
    }
}
