package com.iso8583.server.resilience;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class IsoCircuitBreaker {

    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    private volatile State state = State.CLOSED;
    private final int failureThreshold;
    private final long openTimeoutMillis;

    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong stateChangedTime = new AtomicLong(System.currentTimeMillis());

    public IsoCircuitBreaker(int failureThreshold, long openTimeoutMillis) {
        this.failureThreshold = failureThreshold;
        this.openTimeoutMillis = openTimeoutMillis;
    }

    /**
     * Evalúa si el circuito permite la ejecución hacia el autorizador.
     */
    public boolean allowRequest() {
        if (state == State.OPEN) {
            // Verifica si ya pasó el tiempo de espera para pasar a HALF_OPEN
            if (System.currentTimeMillis() - stateChangedTime.get() > openTimeoutMillis) {
                synchronized (this) {
                    if (state == State.OPEN) {
                        state = State.HALF_OPEN;
                        System.out.println("[CIRCUIT BREAKER] Estado cambiado a: HALF_OPEN (Probando recuperación...)");
                    }
                }
            } else {
                return false; // El circuito sigue abierto; se rechaza la llamada de inmediato (Fallback)
            }
        }
        return true;
    }

    /**
     * Registra un fallo en la comunicación con el autorizador.
     */
    public void recordFailure() {
        if (state == State.HALF_OPEN) {
            tripOpen();
            return;
        }

        int currentFailures = failureCount.incrementAndGet();
        if (currentFailures >= failureThreshold) {
            tripOpen();
        }
    }

    /**
     * Registra un éxito y reinicia los contadores.
     */
    public void recordSuccess() {
        failureCount.set(0);
        if (state == State.HALF_OPEN) {
            state = State.CLOSED;
            stateChangedTime.set(System.currentTimeMillis());
            System.out.println("[CIRCUIT BREAKER] Estado recuperado: CLOSED (Autorizador estable).");
        }
    }

    private void tripOpen() {
        state = State.OPEN;
        stateChangedTime.set(System.currentTimeMillis());
        System.err.println("[CIRCUIT BREAKER] ¡ALERTA! Umbral de fallos superado. Circuito abierto (OPEN). Se activará fallback (DE 39 = 91).");
    }

    public State getState() {
        return state;
    }
}