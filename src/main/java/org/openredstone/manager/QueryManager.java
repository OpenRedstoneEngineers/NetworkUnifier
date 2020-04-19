package org.openredstone.manager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class QueryManager {

    private final String CREATE_USERS_TABLE = "CREATE TABLE IF NOT EXISTS `nu_users` (" +
            "  `m_uuid` varchar(36) NOT NULL," +
            "  `discord_id` varchar(18)," +
            "  `ign` varchar(16) NOT NULL," +
            "  PRIMARY KEY (`m_uuid`)," +
            "  UNIQUE KEY `discord_id` (`discord_id`)," +
            "  UNIQUE KEY `ign` (`ign`)" +
            ");";

    private Connection connection;

    public QueryManager(String host, int port, String databaseName, String user, String password) throws SQLException {
        this.connection = DriverManager.getConnection(
                getDatabaseUrl(host, port, databaseName),
                user,
                password
        );
        establishTables();
    }

    private String getDatabaseUrl(String host, int port, String database) {
        return "jdbc:mysql://" + host
                + ":" + port
                + "/" + database;
    }

    private void establishTables() throws SQLException {
        connection.prepareStatement(CREATE_USERS_TABLE).execute();
    }

    public String getUserIdByDiscordId(String discordId) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `nu_users` WHERE `discord_id`=?");
        preparedStatement.setString(1, discordId);
        ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        return resultSet.getString("m_uuid");
    }

    public String getIgnByDiscordId(String discordId) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `nu_users` WHERE `discord_id`=?");
        preparedStatement.setString(1, discordId);
        ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        return resultSet.getString("ign");
    }

    public String getDiscordIdByUserId(String userId) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `nu_users` WHERE `m_uuid`=?");
        preparedStatement.setString(1, userId);
        ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        return resultSet.getString("discord_id");
    }

    public boolean userIsLinked(String userId) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT EXISTS (SELECT * FROM `nu_users` WHERE `m_uuid`=? AND `discord_id` IS NOT NULL)");
        preparedStatement.setString(1, userId);
        ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        return resultSet.getBoolean(0);
    }

    public void createUnlinkedUser(String userId, String ign) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `nu_users` VALUES (?, NULL, ?)");
        preparedStatement.setString(1, userId);
        preparedStatement.setString(2, ign);
        preparedStatement.execute();
    }

    public void createNewUser(String userId, String discordId, String ign) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `nu_user` VALUES (?, ?, ?)");
        preparedStatement.setString(1, userId);
        preparedStatement.setString(2, discordId);
        preparedStatement.setString(3, ign);
        preparedStatement.execute();
    }

    public void updateDiscordId(String userId, String discordId) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `nu_users` SET `discord_id`=? WHERE `m_uuid`=?");
        preparedStatement.setString(1, discordId);
        preparedStatement.setString(2, userId);
        preparedStatement.execute();
    }

    public void updateUserName(String userId, String ign) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `nu_users` SET `ign`=? WHERE `m_uuid`=?");
        preparedStatement.setString(1, ign);
        preparedStatement.setString(2, userId);
        preparedStatement.execute();
    }

}
