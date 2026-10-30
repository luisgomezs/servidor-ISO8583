package com.iso8583.server.resilience;

import java.util.concurrent.ConcurrentHashMap;

public class IsoIdempotencyManager {

    // Almacenamiento en memoria thread-safe (en producción se usa Redis o una base de datos con TTL)
    private final ConcurrentHashMap<String, byte[]> transactionCache = new ConcurrentHashMap<>();

    /**
     * Genera la clave única de idempotencia combinando los campos críticos de la trama.
     */
    public String generateKey(String terminalId, String stan, String rrn, String amount, String transmissionDateTime) {
        return String.format("%s_%s_%s_%s_%s",
                nonNull(terminalId),
                nonNull(stan),
                nonNull(rrn),
                nonNull(amount),
                nonNull(transmissionDateTime)
        );
    }

    /**
     * Verifica si la transacción ya fue procesada anteriormente.
     * Si existe, devuelve la respuesta guardada sin volver a llamar al autorizador.
     */
    public byte[] getCachedResponse(String idempotencyKey) {
        return transactionCache.get(idempotencyKey);
    }

    /**
     * Almacena la respuesta original de la transacción para futuros reintentos.
     */
    public void saveResponse(String idempotencyKey, byte[] responseFrame) {
        if (idempotencyKey != null && responseFrame != null) {
            transactionCache.put(idempotencyKey, responseFrame);
        }
    }

    private String nonNull(String value) {
        return value != null ? value : "";
    }
}