package org.openredstone.linking;

import java.sql.*;
import java.util.UUID;

public class UserDatabase {
    private final String CREATE_USERS_TABLE = "CREATE TABLE IF NOT EXISTS `nu_users` (" +
            "  `m_uuid` varchar(36) NOT NULL," +
            // pretend this is not null as well
            "  `discord_id` varchar(18)," +
            "  `ign` varchar(16) NOT NULL," +
            "  PRIMARY KEY (`m_uuid`)," +
            "  UNIQUE KEY `discord_id` (`discord_id`)," +
            "  UNIQUE KEY `ign` (`ign`)" +
            ");";

    private final Connection connection;

    public UserDatabase(String host, int port, String databaseName, String user, String password) throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + databaseName,
                user,
                password
        );
        connection.prepareStatement(CREATE_USERS_TABLE).execute();
    }

    public LinkedUser userByMinecraftUuid(UUID uuid) {
        return userQuery("SELECT * FROM `nu_users` WHERE `m_uuid`=?", uuid.toString());
    }

    public LinkedUser userByDiscordId(String discordId) {
        return userQuery("SELECT * FROM `nu_users` WHERE `discord_id`=?", discordId);
    }

    private LinkedUser userQuery(String query, String param) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, param);
            ResultSet rs = preparedStatement.executeQuery();
            if (!rs.next()) return null;
            String discordId = rs.getString("discord_id");
            // hack
            if (discordId == null) return null;
            return new LinkedUser(UUID.fromString(rs.getString("m_uuid")), discordId, rs.getString("ign"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createUser(LinkedUser user) {
        try {
            // idk figure out on duplicate key. i think it can just be removed here
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `nu_users` VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `discord_id` = NULL");
            preparedStatement.setString(1, user.uuid.toString());
            preparedStatement.setString(2, user.discordId);
            preparedStatement.setString(3, user.name);
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // idk do something
    public void updateUserName(UUID userId, String ign) {
        updateUser("UPDATE `nu_users` SET `ign`=? WHERE `m_uuid`=?", ign, userId.toString());
    }

    private void updateUser(String sql, String param1, String param2) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, param1);
            preparedStatement.setString(2, param2);
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
