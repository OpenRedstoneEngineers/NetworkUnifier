package org.openredstone.managers;

import java.sql.SQLException;

public class AccountManager {

    private QueryManager queryManager;
    private TokenManager tokenManager;

    public AccountManager(QueryManager queryManager, int tokenLength, int lifeSpan) {
        this.queryManager = queryManager;
        this.tokenManager = new TokenManager(tokenLength, lifeSpan);
    }

    public boolean validToken(String token) {
        if (!tokenManager.hasToken(token)) {
            return false;
        }

        if (!tokenManager.tokenIsWithinLifespan(token)) {
            try {
                queryManager.deleteUser(tokenManager.getUserFromToken(token));
            } catch (SQLException e) {
                e.printStackTrace();
            }
            tokenManager.removeToken(token);
            return false;
        }

        return true;
    }

    public boolean linkDiscordAccount(String token, String discordId) {
        String userId = tokenManager.getUserFromToken(token);
        tokenManager.removeToken(token);
        return updateAccountDiscordId(userId, discordId);
    }

    public String createAccount(String userId, String ign) {
        try {
            queryManager.createUnlinkedUser(userId, ign);
            return tokenManager.registerTokenToUser(userId);
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

    public String getSavedIgnFromUserId(String userId) {
        try {
            return queryManager.getIgnFromUserId(userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getSavedIgnFromDiscordId(String discordId) {
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

    public boolean userIsLinkedByDiscordId(String discordId) {
        try {
            return queryManager.userIsLinkedByDiscordId(discordId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean userIsLinkedById(String userId) {
        try {
            return queryManager.userIsLinkedById(userId);
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
