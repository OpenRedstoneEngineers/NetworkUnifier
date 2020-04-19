package org.openredstone.manager;

import java.security.SecureRandom;

public class TokenManager {

    private SecureRandom secureRandom;

    public TokenManager() {
        this.secureRandom = new SecureRandom();
    }

    public String generateToken(int length) {
        byte[] rawToken = new byte[length];
        secureRandom.nextBytes(rawToken);
        return encodeHexString(rawToken);
    }

    private String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    private String encodeHexString(byte[] byteArray) {
        StringBuilder hexStringBuffer = new StringBuilder();
        for (byte b : byteArray) {
            hexStringBuffer.append(byteToHex(b));
        }
        return hexStringBuffer.toString();
    }

}
