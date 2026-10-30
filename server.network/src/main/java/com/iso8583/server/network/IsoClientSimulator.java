package com.iso8583.server.network;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class IsoClientSimulator {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 8583;

        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream();
             DataOutputStream dos = new DataOutputStream(out)) {

            System.out.println(">>> Conectando al servidor ISO 8583 en el puerto " + port + "...");

            // Trama simulada con formato básico que incluye MTI, PAN (DE 2), Monto (DE 4) y Terminal (DE 41)
            String tramaSimulada = "080020200000000000004111111234567890000000050000POS_TEST_01";
            byte[] dataBytes = tramaSimulada.getBytes();

            // Envío con el framing de longitud de 2 bytes (Big-Endian)
            dos.writeShort(dataBytes.length);
            dos.write(dataBytes);
            dos.flush();

            System.out.println(">>> Trama enviada con éxito desde el POS simulado.");

        } catch (Exception e) {
            System.err.println(">>> Error en el cliente simulador: " + e.getMessage());
        }
    }
}