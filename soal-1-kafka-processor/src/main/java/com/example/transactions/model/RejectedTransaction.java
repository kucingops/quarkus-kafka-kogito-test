package com.example.transactions.model;

import java.time.Instant;

public record RejectedTransaction(String rawPayload, String reason, Instant rejectedAt) {
}
