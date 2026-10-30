package com.iso8583.server.security;

public class IsoSecurityMasker {

    /**
     * Enmascara el PAN (DE 2) según los estándares de seguridad PCI-DSS.
     * Muestra los primeros 6 dígitos (BIN) y los últimos 4 dígitos, ocultando el centro con asteriscos.
     */
    public static String maskPan(String pan) {
        if (pan == null || pan.length() < 10) {
            return "********"; // Si el PAN es inválido o muy corto, se oculta por completo
        }

        int length = pan.length();
        String bin = pan.substring(0, 6);                   // Primeros 6 dígitos (Banco/Franquicia)
        String last4 = pan.substring(length - 4);           // Últimos 4 dígitos
        String maskedMiddle = "*".repeat(length - 10);      // Centro enmascarado con asteriscos

        return bin + maskedMiddle + last4;
    }

    /**
     * Valida si un campo es sensible y está prohibido de registrarse en logs.
     */
    public static boolean isSensitiveField(int fieldNumber) {
        // DE 35: Pista 2, DE 45: Pista 1, DE 52: PIN Block
        return fieldNumber == 35 || fieldNumber == 45 || fieldNumber == 52;
    }
}