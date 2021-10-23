package org.openredstone.linking;

import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Objects;

public class Tokens {
    private final SecureRandom secureRandom;
    private final HashMap<String, Token> authenticationTokens;
    private final int tokenLength;
    private final Duration lifeSpan;

    public Tokens(int tokenLength, int lifeSpan) {
        this.secureRandom = new SecureRandom();
        this.authenticationTokens = new HashMap<>();
        this.tokenLength = tokenLength / 2;
        this.lifeSpan = Duration.ofSeconds(lifeSpan);
    }

    public String createFor(UnlinkedUser user) {
        String token = generateToken(tokenLength);
        authenticationTokens.put(token, new Token(user, Instant.now()));
        return token;
    }

    public @Nullable UnlinkedUser tryConsume(String token) {
        Token removed = authenticationTokens.remove(token);
        return removed == null || Instant.now().isAfter(removed.createdAt.plus(lifeSpan)) ? null : removed.user;
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

    private static class Token {
        public final UnlinkedUser user;
        public final Instant createdAt;

        private Token(UnlinkedUser user, Instant createdAt) {
            this.user = user;
            this.createdAt = createdAt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Token token = (Token) o;
            return user.equals(token.user) && createdAt.equals(token.createdAt);
        }

        @Override
        public int hashCode() {
            return Objects.hash(user, createdAt);
        }
    }
}
