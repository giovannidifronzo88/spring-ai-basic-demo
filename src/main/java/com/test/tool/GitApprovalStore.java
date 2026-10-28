package com.test.tool;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Approvazioni pendenti in memoria. Una sola per chat: chiave = chatId. Un riavvio le annulla.
class GitApprovalStore {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final Map<String, PendingApproval> approvals = new ConcurrentHashMap<>();

    synchronized void create(String chatId, String operation) {
        approvals.put(chatId, new PendingApproval(UUID.randomUUID().toString(), operation,
                Instant.now().plus(TTL)));
    }

    // Monouso: l'approvazione viene rimossa in ogni caso, anche se ID o operazione non corrispondono.
    synchronized boolean matches(String chatId, String id, String operation) {
        PendingApproval p = approvals.remove(chatId);
        return p != null
                && p.id().equals(id)
                && p.operation().equals(operation)
                && Instant.now().isBefore(p.expiresAt());
    }

    Optional<String> id(String chatId) {
        //l'approval id è associato alla memoria della chat
        PendingApproval p = approvals.get(chatId);
        if (p == null || Instant.now().isAfter(p.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(p.id());
    }
}
