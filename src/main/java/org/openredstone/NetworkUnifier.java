package org.openredstone;

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
import org.javacord.api.entity.user.UserStatus;
import org.openredstone.bots.IrcBot;
import org.openredstone.commands.NetworkUnifierCommand;
import org.openredstone.handlers.*;
import org.openredstone.manager.StatusManager;
import org.pircbotx.Configuration;

import java.io.File;
import java.io.IOException;
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

    @Override
    public void onEnable() {

        logger = getLogger();
        plugin = this;
        dataFolder = plugin.getDataFolder();
        proxy = getProxy();
        version = getDescription().getVersion();

        proxy.getPluginManager().registerCommand(this, new NetworkUnifierCommand("networkunifier", "networkunifier", "nu"));

        load();

    }

    @Override
    public void onDisable() {
        unload();
    }

    public static void load() {

        loadConfig();

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
        }

        if (config.getBoolean("discord_enabled")) {
            discordIrcBot = new DiscordApiBuilder().setToken(config.getString("discord_irc_bot_token")).login().join();
            discordNetworkBot = new DiscordApiBuilder().setToken(config.getString("discord_network_bot_token")).login().join();
            discordIrcBot.updateStatus(UserStatus.fromString(config.getString("discord_irc_bot_playing_message")));
            discordNetworkBot.updateStatus(UserStatus.fromString(config.getString("discord_network_bot_playing_message")));
            gameChannel = discordNetworkBot.getServerTextChannelById(config.getString("discord_channel_id")).get();
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
            new StatusManager(config, discordNetworkBot, plugin);
        }

        joinQuitEventListener = new JoinQuitEventHandler(config, logger, ircNetworkBot, gameChannel);
        gameToIrcListener = new GameToIrcHandler(config, ircNetworkBot);

        proxy.getPluginManager().registerListener(plugin, joinQuitEventListener);
        proxy.getPluginManager().registerListener(plugin, gameToIrcListener);
    }

    public static void unload() {
        if (config.getBoolean("discord_enabled")) {
            discordIrcBot.disconnect();
            discordNetworkBot.disconnect();
            discordToIrcHandler.stopBot();
            ircDiscordBot.stopBot();
        }

        if (config.getBoolean("irc_enabled")) {
            ircNetworkBot.stopBot();
        }

        proxy.getPluginManager().unregisterListener(joinQuitEventListener);
        proxy.getPluginManager().unregisterListener(gameToIrcListener);
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