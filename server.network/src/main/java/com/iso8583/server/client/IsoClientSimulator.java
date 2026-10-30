package com.iso8583.server.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class IsoClientSimulator {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 8583;

        System.out.println(">>> Conectando al servidor ISO 8583 en " + host + ":" + port + "...");

        try (Socket socket = new Socket(host, port);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            System.out.println(">>> ¡Conectado con éxito al servidor!");

            // 1. Construimos una trama ISO de prueba simulada:
            // - MTI (4 bytes): "0200" (Petición de autorización financiera)
            // - Bitmap Primario (8 bytes)
            // - Datos simulados (ej. MTI + Bitmap + payload)
            String mti = "0200";
            byte[] bitmap = new byte[8]; // 8 bytes para el bitmap primario
            String payload = "DATOS_DE_PRUEBA_POS"; // Carga útil de ejemplo

            byte[] mtiBytes = mti.getBytes(StandardCharsets.US_ASCII);
            byte[] payloadBytes = payload.getBytes(StandardCharsets.US_ASCII);

            // Unimos todo en un solo arreglo de bytes que representa la trama ISO completa
            byte[] isoMessage = new byte[mtiBytes.length + bitmap.length + payloadBytes.length];
            System.arraycopy(mtiBytes, 0, isoMessage, 0, mtiBytes.length);
            System.arraycopy(bitmap, 0, isoMessage, mtiBytes.length, bitmap.length);
            System.arraycopy(payloadBytes, 0, isoMessage, mtiBytes.length + bitmap.length, payloadBytes.length);

            // 2. ENVIAR FRAMING Y TRAMA:
            // Primero escribimos los 2 bytes de longitud (Big-Endian) usando writeShort
            dos.writeShort(isoMessage.length);
            // Luego escribimos la trama ISO exacta
            dos.write(isoMessage);
            dos.flush();

            System.out.println(">>> Trama enviada al servidor (" + isoMessage.length + " bytes). Esperando respuesta...");

            // 3. LEER LA RESPUESTA DEL SERVIDOR:
            int responseLength = dis.readUnsignedShort();
            byte[] responseBytes = new byte[responseLength];
            dis.readFully(responseBytes);

            System.out.println(">>> ¡Respuesta del servidor recibida con éxito! Tamaño: " + responseLength + " bytes.");

        } catch (Exception e) {
            System.err.println(">>> Error en el cliente simulador: " + e.getMessage());
            e.printStackTrace();
        }
    }
}