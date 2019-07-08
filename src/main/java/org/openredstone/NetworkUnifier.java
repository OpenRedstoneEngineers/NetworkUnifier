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
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.user.UserStatus;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkUnifier extends Plugin implements Listener {


    private net.md_5.bungee.config.Configuration config;

    List<String> ignoredDiscordIds;
    List<String> ignoredIrcNames;

    List<String> greetings;
    List<String> farewells;

    DiscordApi discordIrcBot;
    DiscordApi discordNetworkBot;
    Channel gameChannel;

    Configuration ircDiscordBotConf;
    Configuration ircNetworkBotConf;
    PircBotX ircDiscordBot;
    PircBotX ircNetworkBot;
    Random rand = new Random();

    boolean quirkyMessages;
    boolean specialFarewellGreetings;
    int quirkyMessageFrequency;

    @Override
    public void onEnable() {

        loadConfig();

        discordIrcBot = new DiscordApiBuilder().setToken(config.getString("discord_irc_bot_token")).login().join();
        discordNetworkBot = new DiscordApiBuilder().setToken(config.getString("discord_network_bot_token")).login().join();
        discordIrcBot.updateStatus(UserStatus.fromString(config.getString("discord_irc_bot_playing_message")));
        discordNetworkBot.updateStatus(UserStatus.fromString(config.getString("discord_network_bot_playing_message")));
        gameChannel = discordNetworkBot.getServerTextChannelById(config.getString("discord_channel_id")).get();

        ircNetworkBotConf = new Configuration.Builder()
                .setName(config.getString("irc_network_bot_name"))
                .addServer(config.getString("irc_host"))
                .addAutoJoinChannel(config.getString("irc_channel"))
                .setNickservPassword(config.getString("irc_network_bot_pass"))
                .addListener(new IrcToGameListener(this.getProxy(), this, config))
                .setAutoReconnect(true)
                .buildConfiguration();
        ircNetworkBot = new PircBotX(ircNetworkBotConf);
        ircDiscordBotConf = new Configuration.Builder()
                .setName(config.getString("irc_discord_bot_name"))
                .addServer(config.getString("irc_host"))
                .addAutoJoinChannel(config.getString("irc_channel"))
                .setNickservPassword(config.getString("irc_discord_bot_pass"))
                .addListener(new IrcToDiscordListener(config, discordIrcBot))
                .setAutoReconnect(true)
                .buildConfiguration();
        ircDiscordBot = new PircBotX(ircDiscordBotConf);
        ircDiscordBotThread();
        ircNetworkBotThread();
        discordThread();
        getProxy().getPluginManager().registerListener(this, this);
        getLogger().info("Loaded Join/Disconnect linker");

    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        Thread postLoginThread = new Thread(() -> {
            sendJoins(event.getPlayer().getDisplayName());
        });
        postLoginThread.start();
    }

    @EventHandler
    public void onQuitEvent(PlayerDisconnectEvent event) {
        Thread postQuitThread = new Thread(() -> {
            sendQuits(event.getPlayer().getDisplayName());
        });
        postQuitThread.start();
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
            getLogger().info("Ignoring Discord IDs: " + ignoredDiscordIds.toString());
            getLogger().info("ignoring IRC Names: " + ignoredIrcNames.toString());

            if (specialFarewellGreetings = config.getBoolean("enable_special_farewells_and_greetings")) {
                farewells = config.getStringList("message_farewells");
                greetings = config.getStringList("message_greetings");
                getLogger().info("Loaded Farewells: " + farewells.toString());
                getLogger().info("Loaded Greetings: " + greetings.toString());
            }

            if (quirkyMessages = config.getBoolean("enable_special_quirky_message")) {
                quirkyMessageFrequency = config.getInt("quirky_message_frequency");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onMessage(ChatEvent e) {
        ProxiedPlayer pp = (ProxiedPlayer) e.getSender();
        if ((e.getMessage().length() > 0) && (!e.getMessage().startsWith("/"))) {
            ircNetworkBot.send().message(config.getString("irc_channel"), "\u000307" + pp.getDisplayName() + "\u000f: " + e.getMessage().replaceAll("&([0-9]|[abcdefklmnor])", "").trim());
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
        new DiscordApiBuilder().setToken(config.getString("discord_irc_bot_token")).login().thenAccept(api -> {
            api.addMessageCreateListener(event -> {
                if(event.getChannel().getIdAsString().equals(config.getString("discord_channel_id"))) {
                    if (!event.getMessageContent().isEmpty() && !ignoredDiscordIds.contains(event.getMessageAuthor().getIdAsString())) {
                        List<User> mentionedUsers = event.getMessage().getMentionedUsers();
                        List<CustomEmoji> mentionedEmojis = event.getMessage().getCustomEmojis();
                        List<Role> mentionedRoles = event.getMessage().getMentionedRoles();
                        String message = event.getMessageContent();

                        if (!mentionedUsers.isEmpty()) {
                            message = replaceUsers(mentionedUsers, message);
                        }
                        if (!mentionedEmojis.isEmpty()) {
                            message = replaceEmojis(mentionedEmojis, message);
                        }
                        if (!mentionedRoles.isEmpty()) {
                            message = replaceRoles(mentionedRoles, message);
                        }

                        ircDiscordBot.send().message(config.getString("irc_channel"), "\u000307" + event.getMessageAuthor().getDisplayName() + "\u000f: " + message);

                    } else if (event.getMessageContent().isEmpty() && (event.getMessage().getEmbeds().size() > 0)){
                        for (Embed embed : event.getMessage().getEmbeds()) {
                            ircDiscordBot.send().message(config.getString("irc_channel"), "\u000307" + embed.getUrl().toString());
                        }
                    }
                }
            });
        });
    }

    private String replaceUsers(List<User> users, String message) {
        for (User user : users) {
            String toReplace = "<@!" + user.getIdAsString() + ">";
            message.replaceAll(toReplace, "@" + user.getDisplayName(user.getMutualServers().iterator().next()));
        }
        return message;
    }

    private String replaceEmojis(List<CustomEmoji> emojis, String message) {
        for (CustomEmoji emoji : emojis) {
            String toReplace = "<:" + emoji.getName() + ":" + emoji.getIdAsString() + ">";
            message.replaceAll(toReplace, ":"+ emoji.getName() + ":");
        }
        return message;
    }

    private String replaceRoles(List<Role> roles, String message) {
        for (Role role : roles) {
            String toReplace = "<@&" + role.getIdAsString() + ">";
            message.replaceAll(toReplace, "@" + role.getName());
        }
        return message;
    }

    private void sendJoins(String name) {
        String message;
        if (quirkyMessages && (rand.nextInt(quirkyMessageFrequency) == 1)) {
            message = "Running " + name + ".exe ...";
        } else {
            if (specialFarewellGreetings) message = name + " joined the network." + getRandomWelcome();
            else message = name + " joined the network.";
        }
        sendToIrc(message);
        sendToDiscord(message);
    }

    private void sendQuits(String name) {
        String message;
        if (quirkyMessages && (rand.nextInt(quirkyMessageFrequency) == 1)) {
            message = name + ".exe has stopped working.";
        } else {
            if (specialFarewellGreetings) message = name + " left the network." + getRandomFarewell();
            else message = name + " left the network.";
        }
        sendToIrc(message);
        sendToDiscord(message);
    }

    private void sendToIrc(String message) {
        ircNetworkBot.send().message(config.getString("irc_channel"), "\u000307" + message);
    }

    private void sendToDiscord(String message) {
        ((ServerTextChannel) gameChannel).sendMessage("**" + message + "**");
    }

    public String getRandomFarewell() {
        return farewells.get(rand.nextInt(farewells.size() - 1 ));
    }

    public String getRandomWelcome() {
        return greetings.get(rand.nextInt(greetings.size() - 1 ));
    }
}