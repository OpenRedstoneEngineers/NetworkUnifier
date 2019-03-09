package org.openredstone;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;


public class NetworkUnifier extends Plugin implements Listener {


    private net.md_5.bungee.config.Configuration config;

    List<String> ignoredDiscordIds;
    List<String> ignoredIrcNames;

    List<String> greetings;
    List<String> farewells;

    Configuration ircDiscordBotConf;
    Configuration ircNetworkBotConf;
    PircBotX ircDiscordBot;
    PircBotX ircNetworkBot;
    Random rand = new Random();

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        sendJoinToDiscord(event.getPlayer().getDisplayName());
        sendJoinToIrc(event.getPlayer().getDisplayName());
    }

    @EventHandler
    public void onQuitEvent(PlayerDisconnectEvent event) {
        sendQuitToDiscord(event.getPlayer().getDisplayName());
        sendQuitToIrc(event.getPlayer().getDisplayName());
    }

    @Override
    public void onEnable() {

        loadConfig();

        ircNetworkBotConf = new Configuration.Builder()
                .setName(config.getString("irc_network_bot_name")) //Set the nick of the bot. CHANGE IN YOUR CODE
                .addServer(config.getString("irc_host")) //Join the freenode network
                .addAutoJoinChannel(config.getString("irc_channel")) //Join the official #pircbotx channel
                .addListener(new IrcToGameListener(this.getProxy(), this, config)) //Add our listener that will be called on Events
                .setAutoReconnect(true)
                .buildConfiguration();
        ircNetworkBot = new PircBotX(ircNetworkBotConf);
        ircDiscordBotConf = new Configuration.Builder()
                .setName(config.getString("irc_discord_bot_name")) //Set the nick of the bot. CHANGE IN YOUR CODE
                .addServer(config.getString("irc_host")) //Join the freenode network
                .addAutoJoinChannel(config.getString("irc_channel")) //Join the official #pircbotx channel
                .addListener(new IrcToDiscordListener(config)) //Add our listener that will be called on Events
                .setAutoReconnect(true)
                .buildConfiguration();
        ircDiscordBot = new PircBotX(ircDiscordBotConf);
        ircDiscordBotThread();
        ircNetworkBotThread();
        discordThread();
        getProxy().getPluginManager().registerListener(this, this);
        getLogger().info("Loaded Join/Disconnect linker");
    }

    private void loadConfig() {
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
            }
            File file = new File(getDataFolder(), "config.yml");
            if (!file.exists()) {
                file.createNewFile();
            }
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));

            ignoredDiscordIds = config.getStringList("discord_ignore_ids");
            ignoredIrcNames = config.getStringList("irc_ignore_names");
            farewells = config.getStringList("message_farewells");
            greetings = config.getStringList("message_greetings");

            getLogger().info("Ignoring Discord IDs: " + ignoredDiscordIds.toString());
            getLogger().info("ignoring IRC Names: " + ignoredIrcNames.toString());
            getLogger().info("Loaded Farewells: " + farewells.toString());
            getLogger().info("Loaded Greetings: " + greetings.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onMessage(ChatEvent e) {
        ProxiedPlayer pp = (ProxiedPlayer) e.getSender();
        if ((e.getMessage().length() > 0) && (!e.getMessage().startsWith("/"))) {
            ircNetworkBot.send().message(config.getString("irc_channel"), "\u000307" + pp.getDisplayName() + "\u000f: " + e.getMessage());
        }
    }

    private void ircNetworkBotThread() {
        // The bot would print out to the proxy without slapping it into its own thread.
        Thread botThread = new Thread(() -> {
            try {
                ircNetworkBot.startBot();
            } catch (IOException e) {
                getLogger().info(e.toString());
                this.getProxy().getScheduler().runAsync(this, new Runnable() {
                    @Override
                    public void run() {
                        getLogger().info(e.getMessage());
                    }
                });
            } catch (IrcException e) {
                getLogger().info(e.toString());
                this.getProxy().getScheduler().runAsync(this, new Runnable() {
                    @Override
                    public void run() {
                        getLogger().info(e.getMessage());
                    }
                });
            }
        });
        botThread.start();
    }

    private void ircDiscordBotThread() {
        // The bot would print out to the proxy without slapping it into its own thread.
        Thread botThread = new Thread(() -> {
            try {
                ircDiscordBot.startBot();
            } catch (IOException e) {
                getLogger().info(e.toString());
                this.getProxy().getScheduler().runAsync(this, new Runnable() {
                    @Override
                    public void run() {
                        getLogger().info(e.getMessage());
                    }
                });
            } catch (IrcException e) {
                getLogger().info(e.toString());
                this.getProxy().getScheduler().runAsync(this, new Runnable() {
                    @Override
                    public void run() {
                        getLogger().info(e.getMessage());
                    }
                });
            }
        });
        botThread.start();
    }

    private void discordThread() {
        Thread discordThread = new Thread(() -> {
            new DiscordApiBuilder().setToken(config.getString("discord_irc_bot_token")).login().thenAccept(api -> {
                api.addMessageCreateListener(event -> {
                    if(event.getChannel().getIdAsString().equals(config.getString("discord_channel_id"))) {
                        if (!event.getMessageContent().isEmpty() && !ignoredDiscordIds.contains(event.getMessageAuthor().getIdAsString())) {
                            ircDiscordBot.send().message(config.getString("irc_channel"), "\u000307" + event.getMessageAuthor().getDisplayName() + "\u000f: " + event.getMessageContent());
                        }
                    }
                });
            });
        });
        discordThread.start();
    }

    private void sendJoinToIrc(String name) {
        ircNetworkBot.send().message(config.getString("irc_channel"), "\u000307" + name + " joined the network! " + getRandomWelcome());
    }

    private void sendQuitToIrc(String name) {
        ircNetworkBot.send().message(config.getString("irc_channel"), "\u000307" + name + " left the network! " + getRandomFarewell());
    }

    private void sendJoinToDiscord(String name) {
        DiscordApi api = new DiscordApiBuilder().setToken(config.getString("discord_network_bot_token")).login().join();
        Channel channel = api.getServerTextChannelById(config.getString("discord_channel_id")).get();
        ((ServerTextChannel) channel).sendMessage("**" + name + "** joined the network! " + getRandomWelcome());
    }

    private void sendQuitToDiscord(String name) {
        DiscordApi api = new DiscordApiBuilder().setToken(config.getString("discord_network_bot_token")).login().join();
        Channel channel = api.getServerTextChannelById(config.getString("discord_channel_id")).get();
        ((ServerTextChannel) channel).sendMessage("**" + name + "** left the network! " + getRandomFarewell());
    }

    public String getRandomFarewell() {
        return farewells.get(rand.nextInt(farewells.size() - 1 ));
    }

    public String getRandomWelcome() {
        return greetings.get(rand.nextInt(greetings.size() - 1 ));
    }
}