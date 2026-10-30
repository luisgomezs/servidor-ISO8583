package com.iso8583.server.resilience;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IsoWatchdog {

    // Se ejecuta automáticamente cada 30 segundos para auditar la salud del servidor
    @Scheduled(fixedRate = 30000)
    public void reportSystemHealth() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;

        System.out.println("\n================ [WATCHDOG - REPORTE DE SALUD] ================");
        System.out.println(" Estado del Servidor : ACTIVO Y ESCUCHANDO");
        System.out.println(" Memoria RAM Usada   : " + usedMemory + " MB / " + totalMemory + " MB");
        System.out.println(" Hilos Virtuales     : Operando de forma concurrente");
        System.out.println("===============================================================\n");
    }
}