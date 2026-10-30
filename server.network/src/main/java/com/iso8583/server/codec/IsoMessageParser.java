package com.iso8583.server.codec;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class IsoMessageParser {

    public static class IsoMessage {
        private String mti;
        private byte[] primaryBitmap;
        private final Map<Integer, String> fields = new HashMap<>();

        public String getMti() { return mti; }
        public void setMti(String mti) { this.mti = mti; }

        public void setField(int fieldNumber, String value) {
            fields.put(fieldNumber, value);
        }

        public String getField(int fieldNumber) {
            return fields.get(fieldNumber);
        }
    }

    public static IsoMessage parse(byte[] frame) {
        IsoMessage message = new IsoMessage();

        // 1. Leer el MTI (Primeros 4 bytes / caracteres)
        String mti = new String(frame, 0, 4, StandardCharsets.US_ASCII);
        message.setMti(mti);

        // 2. Leer el Mapa de Bits Primario (Siguientes 8 bytes, desde la posición 4 hasta la 12)
        // Nota: En ISO 8583 real, el bitmap puede ser primario (8 bytes) o secundario (16 bytes si el bit 1 está encendido).
        // Para simplificar esta base, asumiremos el bitmap primario de 8 bytes.
        byte[] bitmap = new byte[8];
        System.arraycopy(frame, 4, bitmap, 0, 8);
        message.primaryBitmap = bitmap;

        System.out.println(">>> MTI Analizado: " + mti);
        return message;
    }
}