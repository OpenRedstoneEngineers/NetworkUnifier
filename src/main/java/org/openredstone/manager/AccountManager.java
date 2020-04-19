package org.openredstone.manager;

import java.sql.SQLException;
import java.util.HashMap;

public class AccountManager {

    private QueryManager queryManager;
    private HashMap<String, String> authenticationTokens;
    private TokenManager tokenManager;

    public AccountManager(QueryManager queryManager) {
        this.queryManager = queryManager;
        this.tokenManager = new TokenManager();
        this.authenticationTokens = new HashMap<>();
    }

    public boolean tokenExists(String token) {
        return authenticationTokens.containsKey(token);
    }

    public boolean linkDiscordAccount(String token, String discordId) {
        String userId = authenticationTokens.get(token);
        authenticationTokens.remove(token);
        return updateAccountDiscordId(userId, discordId);
    }

    public String createAccount(String userId, String ign) {
        try {
            queryManager.createUnlinkedUser(userId, ign);
            String token = tokenManager.generateToken(2);
            authenticationTokens.put(token, userId);
            return token;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean updateAccountDiscordId(String userId, String discordId) {
        try {
            queryManager.updateDiscordId(userId, discordId);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getIgnFromDiscordId(String discordId) {
        try {
            return queryManager.getIgnByDiscordId(discordId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateAccountIgn(String userId, String ign) {
        try {
            queryManager.updateUserName(userId, ign);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean userIsLinked(String userId) {
        try {
            return queryManager.userIsLinked(userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getDiscordId(String userId) {
        try {
            return queryManager.getDiscordIdByUserId(userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


}
