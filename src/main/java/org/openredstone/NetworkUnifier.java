package org.openredstone;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
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
import org.javacord.api.entity.message.Message;
import org.openredstone.bots.IrcBot;
import org.openredstone.linking.*;
import org.openredstone.commands.minecraft.NetworkUnifierCommand;
import org.openredstone.handlers.*;
import org.openredstone.managers.*;
import org.pircbotx.Configuration;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    static StatusManager statusManager;

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
            gameToIrcListener = new GameToIrcHandler(config, ircNetworkBot);
            proxy.getPluginManager().registerListener(plugin, gameToIrcListener);
        }

        if (config.getBoolean("discord_enabled")) {
            discordNetworkBot = new DiscordApiBuilder().setToken(config.getString("discord_network_bot_token")).login().join();
            discordNetworkBot.updateActivity(config.getString("discord_network_bot_playing_message"));
            gameChannel = discordNetworkBot.getServerTextChannelById(config.getString("discord_channel_id")).get();
            statusManager = new StatusManager(config, discordNetworkBot, plugin);

            // below this are linking thinking
            LuckPerms luckPerms = LuckPermsProvider.get();
            DiscordOperations discordOperations = new DiscordOperations(discordNetworkBot, luckPerms, config.getString("discord_server_id"), config.getStringList("discord_tracked_tracks"));
            Tokens tokens = new Tokens(config.getInt("discord_token_size"), config.getInt("discord_token_lifespan"));
            UserDatabase userDatabase;
            try {
                userDatabase = new UserDatabase(
                        config.getString("database_host"),
                        config.getInt("database_port"),
                        config.getString("database_name"),
                        config.getString("database_user"),
                        config.getString("database_pass")
                );
            } catch (SQLException e) {
                e.printStackTrace();
                return;
            }
            Linking linking = new Linking(tokens, userDatabase, discordOperations, luckPerms);

            String error = discordOperations.groupsExistInTrackOnDiscordAlsoThisMethodIsReallyLongButIAmKeepingItToAnnoyPeopleAndIJustMadeItALittleBitLongerSmileyFace();
            if (error != null) {
                logger.log(Level.SEVERE, "Cannot validate that the roles from the specified tracks exist on Discord or LuckPerms: " + error);
                return;
            } else {
                logger.log(Level.INFO, "Validated that all listened tracks have related groups on discord.");
            }

            discordNetworkBot.addServerMemberJoinListener(event -> linking.memberJoin(event.getUser().getIdAsString()));

            // authcommand
            char commandChar = config.getString("discord_command_character").charAt(0);
            ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            discordNetworkBot.addMessageCreateListener(event -> {
                String rawMessage = event.getMessageContent();
                String[] splat = rawMessage.split(" ");
                String command = splat[0];
                if (!command.equals(commandChar + "auth")) {
                    event.getChannel().sendMessage("Invalid command.");
                    return;
                }
                if (splat.length != 2) {
                    event.getChannel().sendMessage("This command requires exactly one argument.");
                    return;
                }
                String token = splat[1];
                String discordId = Long.toString(event.getMessageAuthor().getId());
                String response = linking.finishLinking(discordId, token);
                try {
                    Message message = event.getChannel().sendMessage(response).get();
                    scheduledExecutorService.schedule(() -> message.delete(), 5, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                scheduledExecutorService.schedule(() -> event.getMessage().delete(), 5, TimeUnit.SECONDS);
            });

            // user smth thing
            luckPerms.getEventBus().subscribe(
                    UserDataRecalculateEvent.class,
                    event -> linking.userUpdate(event.getUser().getUniqueId())
            );

            // rest
            proxy.getPluginManager().registerListener(plugin, new OnJoinHandler(linking));
            proxy.getPluginManager().registerCommand(plugin, new DiscordCommand(linking,"discord", "networkunifier.discord", "discord"));
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