package com.expensetracker.security;

import com.expensetracker.common.BadRequestException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiterService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_SECONDS = 900; // 15 minutes

    private static class AttemptRecord {
        int attempts;
        Instant firstAttemptAt;
        Instant lockedUntil;

        AttemptRecord() {
            this.attempts = 1;
            this.firstAttemptAt = Instant.now();
            this.lockedUntil = null;
        }
    }

    private final Map<String, AttemptRecord> attemptCache = new ConcurrentHashMap<>();

    public void checkRateLimit(String key) {
        String safeKey = key.toLowerCase().trim();
        AttemptRecord record = attemptCache.get(safeKey);
        if (record != null && record.lockedUntil != null) {
            if (Instant.now().isBefore(record.lockedUntil)) {
                long remainingMinutes = Math.max(1, (record.lockedUntil.getEpochSecond() - Instant.now().getEpochSecond()) / 60);
                throw new BadRequestException("Account temporarily locked due to excessive failed login attempts. Please try again in " + remainingMinutes + " minute(s).");
            } else {
                // Lock expired
                attemptCache.remove(safeKey);
            }
        }
    }

    public void recordFailedAttempt(String key) {
        String safeKey = key.toLowerCase().trim();
        attemptCache.compute(safeKey, (k, record) -> {
            if (record == null) {
                return new AttemptRecord();
            }

            // If existing record was created more than lockout duration ago, reset
            if (Instant.now().isAfter(record.firstAttemptAt.plusSeconds(LOCKOUT_DURATION_SECONDS))) {
                return new AttemptRecord();
            }

            record.attempts++;
            if (record.attempts >= MAX_ATTEMPTS) {
                record.lockedUntil = Instant.now().plusSeconds(LOCKOUT_DURATION_SECONDS);
            }
            return record;
        });
    }

    public void recordSuccessfulLogin(String key) {
        String safeKey = key.toLowerCase().trim();
        attemptCache.remove(safeKey);
    }
}
