package com.test.tool;

import java.time.Instant;

// Approvazione in attesa di conferma dell'utente, legata a una chat e a una singola operazione git.
record PendingApproval(String id, String operation, Instant expiresAt) {}
