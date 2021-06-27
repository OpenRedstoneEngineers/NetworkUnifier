package org.openredstone.managers;

import java.security.SecureRandom;
import java.util.HashMap;

public class TokenManager {

    private final SecureRandom secureRandom;
    private final HashMap<String, String> authenticationTokens;
    private final HashMap<String, Long> tokenLifespans;

    private final int tokenLength;
    private final int lifeSpan;

    public TokenManager(int tokenLength, int lifeSpan) {
        this.secureRandom = new SecureRandom();
        this.authenticationTokens = new HashMap<>();
        this.tokenLifespans = new HashMap<>();
        this.tokenLength = tokenLength/2;
        this.lifeSpan = lifeSpan;
    }

    public String registerTokenToUser(String userId) {
        String token = generateToken(tokenLength);
        if (authenticationTokens.containsValue(userId)) {
            String key = authenticationTokens.keySet().stream().filter(authenticationToken ->
                    authenticationTokens.get(authenticationToken).equals(userId)
            ).findFirst().get();
            authenticationTokens.remove(key);
            tokenLifespans.remove(key);
        }
        authenticationTokens.put(token, userId);
        tokenLifespans.put(token, System.currentTimeMillis() / 1000L);
        return token;
    }

    public String getUserFromToken(String token) {
        if (authenticationTokens.containsKey(token)) {
            return authenticationTokens.get(token);
        }
        return null;
    }

    public void removeToken(String token) {
        authenticationTokens.remove(token);
        tokenLifespans.remove(token);
    }

    public boolean hasToken(String token) {
        return authenticationTokens.containsKey(token);
    }

    public boolean tokenIsWithinLifespan(String token) {
        return (System.currentTimeMillis() / 1000L) < (tokenLifespans.get(token) + lifeSpan);
    }

    private String generateToken(int length) {
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
