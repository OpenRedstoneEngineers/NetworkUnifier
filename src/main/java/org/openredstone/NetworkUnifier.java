package org.openredstone;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.api.plugin.Listener;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.Channel;
import org.openredstone.bots.IrcBot;
import org.openredstone.commands.minecraft.DiscordCommand;
import org.openredstone.commands.minecraft.NetworkUnifierCommand;
import org.openredstone.handlers.*;
import org.openredstone.listeners.UserUpdateListener;
import org.openredstone.managers.AccountManager;
import org.openredstone.managers.DiscordCommandManager;
import org.openredstone.managers.NicknameManager;
import org.openredstone.managers.QueryManager;
import org.openredstone.managers.RoleManager;
import org.openredstone.managers.StatusManager;
import org.pircbotx.Configuration;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkUnifier extends Plugin implements Listener {

    static net.md_5.bungee.config.Configuration config;
    static Logger logger;
    static Plugin plugin;
    static File dataFolder;
    static ProxyServer proxy;
    static String version;

    static DiscordApi discordIrcBot;
    static DiscordApi discordNetworkBot;
    static Channel gameChannel;

    static IrcBot ircNetworkBot;
    static IrcBot ircDiscordBot;

    static Listener joinQuitEventListener;
    static Listener gameToIrcListener;

    static DiscordToIrcHandler discordToIrcHandler;

    static LuckPerms luckPerms;

    static AccountManager accountManager;
    static DiscordCommandManager discordCommandManager;
    static NicknameManager nicknameManager;
    static QueryManager queryManager;
    static RoleManager roleManager;
    static StatusManager statusManager;

    @Override
    public void onEnable() {

        logger = getLogger();
        plugin = this;
        dataFolder = plugin.getDataFolder();
        proxy = getProxy();
        version = getDescription().getVersion();

        luckPerms = LuckPermsProvider.get();

        proxy.getPluginManager().registerCommand(this, new NetworkUnifierCommand("networkunifier", "networkunifier", "nu"));

        load();

    }

    @Override
    public void onDisable() {
        unload();
    }

    public static void load() {

        loadConfig();

        try {
            queryManager = new QueryManager(
                    config.getString("database_host"),
                    config.getInt("database_port"),
                    config.getString("database_name"),
                    config.getString("database_user"),
                    config.getString("database_pass")
            );
            accountManager = new AccountManager(queryManager, config.getInt("discord_token_size"), config.getInt("discord_token_lifespan"));
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        if (config.getBoolean("irc_enabled")) {
            ircNetworkBot = new IrcBot(new Configuration.Builder()
                    .setName(config.getString("irc_network_bot_name"))
                    .addServer(config.getString("irc_host"))
                    .addAutoJoinChannel(config.getString("irc_channel"))
                    .setNickservPassword(config.getString("irc_network_bot_pass"))
                    .addListener(new IrcToGameHandler(proxy, plugin, config))
                    .setAutoReconnect(true)
                    .buildConfiguration(),
                    logger);
            ircNetworkBot.startBot();
            gameToIrcListener = new GameToIrcHandler(config, ircNetworkBot);
            proxy.getPluginManager().registerListener(plugin, gameToIrcListener);
        }

        if (config.getBoolean("discord_enabled")) {
            discordNetworkBot = new DiscordApiBuilder().setToken(config.getString("discord_network_bot_token")).login().join();
            discordNetworkBot.updateActivity(config.getString("discord_network_bot_playing_message"));
            gameChannel = discordNetworkBot.getServerTextChannelById(config.getString("discord_channel_id")).get();
            statusManager = new StatusManager(config, discordNetworkBot, plugin);
            nicknameManager = new NicknameManager(discordNetworkBot, accountManager, config.getString("discord_server_id"));
            roleManager = new RoleManager(accountManager, discordNetworkBot, luckPerms, config.getString("discord_server_id"), config.getStringList("discord_tracked_tracks"));
            if (!roleManager.groupsExistInTrackOnDiscordAlsoThisMethodIsReallyLongButIAmKeepingItToAnnoyPeople()) {
                logger.log(Level.SEVERE, "Cannot validate that the roles from the specified tracks exist on Discord or LuckPerms!");
                return;
            } else {
                logger.log(Level.INFO, "Validated that all listened tracks have related groups on discord.");
            }
            discordNetworkBot.addServerMemberJoinListener(event -> {
                String id = event.getUser().getIdAsString();
                if (!accountManager.userIsLinkedByDiscordId(id)) {
                    return;
                }
                String uuid = accountManager.getUserIdByDiscordId(id);
                nicknameManager.setNickname(uuid, accountManager.getSavedIgnFromDiscordId(id));
                User user = luckPerms.getUserManager().getUser(uuid);
                if (user == null) {
                    return;
                }
                String primaryGroup = user.getPrimaryGroup();
                if (!roleManager.isGroupTracked(primaryGroup)) {
                    return;
                }
                Group luckPrimaryGroup = luckPerms.getGroupManager().getGroup(primaryGroup);
                if (luckPrimaryGroup == null) {
                    return;
                }
                roleManager.setTrackedDiscordGroup(id, luckPrimaryGroup.getDisplayName());
            });
            discordCommandManager = new DiscordCommandManager(discordNetworkBot, accountManager, roleManager, luckPerms, config.getString("discord_command_character").charAt(0));
            new UserUpdateListener(roleManager, accountManager, luckPerms);
            proxy.getPluginManager().registerListener(plugin, new OnJoinHandler(accountManager, nicknameManager));
            proxy.getPluginManager().registerCommand(plugin, new DiscordCommand(accountManager,"discord", "networkunifier.discord", "discord"));
        }

        if (config.getBoolean("irc_enabled") && config.getBoolean("discord_enabled")) {
            discordIrcBot = new DiscordApiBuilder().setToken(config.getString("discord_irc_bot_token")).login().join();
            discordIrcBot.updateActivity(config.getString("discord_irc_bot_playing_message"));
            ircDiscordBot = new IrcBot(new Configuration.Builder()
                    .setName(config.getString("irc_discord_bot_name"))
                    .addServer(config.getString("irc_host"))
                    .addAutoJoinChannel(config.getString("irc_channel"))
                    .setNickservPassword(config.getString("irc_discord_bot_pass"))
                    .addListener(new IrcToDiscordHandler(config, discordIrcBot))
                    .setAutoReconnect(true)
                    .buildConfiguration(),
                    logger);
            ircDiscordBot.startBot();
            discordToIrcHandler = new DiscordToIrcHandler(config, logger, ircDiscordBot);
            discordToIrcHandler.startBot();
        }

        joinQuitEventListener = new JoinQuitEventHandler(config, logger, ircNetworkBot, gameChannel);

        proxy.getPluginManager().registerListener(plugin, joinQuitEventListener);

    }

    public static void unload() {
        if (config.getBoolean("irc_enabled")) {
            ircNetworkBot.stopBot();
            proxy.getPluginManager().unregisterListener(gameToIrcListener);
        }

        if (config.getBoolean("discord_enabled")) {
            discordNetworkBot.disconnect();
        }

        if (config.getBoolean("irc_enabled") && config.getBoolean("discord_enabled")) {
            discordIrcBot.disconnect();
            discordToIrcHandler.stopBot();
            ircDiscordBot.stopBot();
        }

        proxy.getPluginManager().unregisterListener(joinQuitEventListener);
    }

    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(new TextComponent(
                ChatColor.DARK_GRAY + "[" +
                ChatColor.GRAY + "NetworkUnifier" +
                ChatColor.DARK_GRAY + "]" +
                " " + ChatColor.GRAY + message));
    }

    public static String getVersion() {
        return version;
    }

    private static void loadConfig() {
        try {
            if (!dataFolder.exists()) {
                dataFolder.mkdir();
            }
            File file = new File(dataFolder, "config.yml");
            if (!file.exists()) {
                file.createNewFile();
            }

            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(dataFolder, "config.yml"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}