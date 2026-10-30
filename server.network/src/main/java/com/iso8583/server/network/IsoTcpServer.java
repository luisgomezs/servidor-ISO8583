package com.iso8583.server.network;

import com.iso8583.server.codec.IsoMessageParser;
import com.iso8583.server.model.TransactionEntity;
import com.iso8583.server.repository.TransactionRepository;
import com.iso8583.server.resilience.IsoCircuitBreaker;
import com.iso8583.server.resilience.IsoRateLimiter;
import com.iso8583.server.security.IsoSecurityMasker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

@Component
public class IsoTcpServer implements CommandLineRunner {
    private final int port = 8583;
    private volatile boolean running = true;

    @Autowired
    private TransactionRepository transactionRepository;

    private final IsoRateLimiter rateLimiter = new IsoRateLimiter(5);
    private final IsoCircuitBreaker circuitBreaker = new IsoCircuitBreaker(3, 10000);

    @Override
    public void run(String... args) {
        Thread.ofVirtual().start(this::startServerLoop);
    }

    private void startServerLoop() {
        System.out.println(">>> Servidor ISO 8583 intentando escuchar en el puerto " + port + "...");
        try (ServerSocket serverSocket = new ServerSocket(port);
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            System.out.println(">>> ¡Servidor ISO 8583 escuchando con éxito en el puerto " + port + " con Virtual Threads!");

            while (running) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            System.err.println(">>> Error en el ServerSocket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            while (!socket.isClosed() && socket.isConnected()) {
                int length = dis.readUnsignedShort();
                if (length <= 0) break;

                byte[] isoFrame = new byte[length];
                dis.readFully(isoFrame);

                System.out.println("\n>>> Trama recibida del POS. Tamaño: " + length + " bytes.");

                IsoMessageParser.IsoMessage parsedMsg = IsoMessageParser.parse(isoFrame);
                System.out.println(">>> MTI Procesado: " + parsedMsg.getMti());

                String rawPan = parsedMsg.getField(2);
                String securePan = "********";
                if (rawPan != null) {
                    securePan = IsoSecurityMasker.maskPan(rawPan);
                    System.out.println("[AUDIT LOG] DE 2 (PAN Enmascarado): " + securePan);
                }

                byte[] responseFrame = processPipeline(isoFrame, parsedMsg, securePan);

                dos.writeShort(responseFrame.length);
                dos.write(responseFrame);
                dos.flush();
            }
        } catch (IOException e) {
            System.out.println("Conexión cerrada con el terminal: " + e.getMessage());
        }
    }

    private byte[] processPipeline(byte[] requestFrame, IsoMessageParser.IsoMessage parsedMsg, String securePan) {
        String terminalId = parsedMsg.getField(41);
        if (terminalId == null) {
            terminalId = "POS_UNKNOWN";
        }

        // Extracción dinámica del monto (DE 4)
        Double amount = 0.0;
        String rawAmount = parsedMsg.getField(4);
        if (rawAmount != null) {
            try {
                amount = Double.parseDouble(rawAmount) / 100.0;
            } catch (NumberFormatException e) {
                amount = 0.0;
            }
        }

        // --- FILTRO 1: RATE LIMITING ---
        if (!rateLimiter.allowRequest(terminalId)) {
            System.out.println("[PIPELINE] Rate limit excedido para " + terminalId + ". Rechazando (DE 39 = 92).");
            guardarTransaccionDB(terminalId, securePan, amount, "92");
            return buildErrorResponse(requestFrame, "92");
        }

        // --- FILTRO 2: CIRCUIT BREAKER ---
        if (!circuitBreaker.allowRequest()) {
            System.out.println("[PIPELINE] Circuito abierto. Fallback instantáneo (DE 39 = 91).");
            guardarTransaccionDB(terminalId, securePan, amount, "91");
            return buildErrorResponse(requestFrame, "91");
        } else {
            try {
                circuitBreaker.recordSuccess();
                guardarTransaccionDB(terminalId, securePan, amount, "00");
                return requestFrame;
            } catch (Exception e) {
                circuitBreaker.recordFailure();
                guardarTransaccionDB(terminalId, securePan, amount, "91");
                return buildErrorResponse(requestFrame, "91");
            }
        }
    }

    private void guardarTransaccionDB(String terminalId, String maskedPan, Double amount, String responseCode) {
        try {
            if (transactionRepository != null) {
                TransactionEntity tx = new TransactionEntity();
                tx.setTerminalId(terminalId);
                tx.setMaskedPan(maskedPan);
                tx.setAmount(amount);
                tx.setResponseCode(responseCode);
                transactionRepository.save(tx);
                System.out.println(">>> [DB] Transacción TCP guardada en PostgreSQL con ID: " + tx.getId() + " [Monto: " + amount + ", Respuesta: " + responseCode + "]");
            }
        } catch (Exception e) {
            System.err.println(">>> [DB ERROR] No se pudo guardar la transacción en PostgreSQL: " + e.getMessage());
        }
    }

    private byte[] buildErrorResponse(byte[] originalRequest, String responseCode) {
        return originalRequest;
    }
}