package com.iso8583.server.resilience;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class IsoRateLimiter {

    // Almacena las solicitudes por terminal en la ventana de tiempo actual
    private final Map<String, TokenBucket> terminalBuckets = new ConcurrentHashMap<>();
    private final int maxRequestsPerSecond;

    public IsoRateLimiter(int maxRequestsPerSecond) {
        this.maxRequestsPerSecond = maxRequestsPerSecond;
    }

    /**
     * Evalúa si el terminal (DE 41) excede su cuota permitida de transacciones por segundo.
     */
    public boolean allowRequest(String terminalId) {
        if (terminalId == null || terminalId.isEmpty()) {
            return true; // Si no trae ID de terminal por defecto se evalúa globalmente o se permite
        }

        TokenBucket bucket = terminalBuckets.computeIfAbsent(terminalId, k -> new TokenBucket(maxRequestsPerSecond));
        return bucket.tryConsume();
    }

    private static class TokenBucket {
        private final int capacity;
        private final AtomicInteger tokens;
        private long lastRefillTime;

        public TokenBucket(int capacity) {
            this.capacity = capacity;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTime = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            // Rellenar tokens cada 1 segundo
            if (now - lastRefillTime > 1000) {
                tokens.set(capacity);
                lastRefillTime = now;
            }

            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false; // Límite de TPS superado
        }
    }
}