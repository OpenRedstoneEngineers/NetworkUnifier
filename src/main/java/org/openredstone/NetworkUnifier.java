package org.openredstone;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.api.plugin.Listener;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.user.UserStatus;
import org.openredstone.bots.IrcBot;
import org.openredstone.handlers.*;
import org.pircbotx.Configuration;

import java.io.File;
import java.io.IOException;

public class NetworkUnifier extends Plugin implements Listener {

    private net.md_5.bungee.config.Configuration config;

    DiscordApi discordIrcBot;
    DiscordApi discordNetworkBot;
    Channel gameChannel;

    IrcBot ircNetworkBot;
    IrcBot ircDiscordBot;

    @Override
    public void onEnable() {

        loadConfig();

        discordIrcBot = new DiscordApiBuilder().setToken(config.getString("discord_irc_bot_token")).login().join();
        discordNetworkBot = new DiscordApiBuilder().setToken(config.getString("discord_network_bot_token")).login().join();
        discordIrcBot.updateStatus(UserStatus.fromString(config.getString("discord_irc_bot_playing_message")));
        discordNetworkBot.updateStatus(UserStatus.fromString(config.getString("discord_network_bot_playing_message")));
        gameChannel = discordNetworkBot.getServerTextChannelById(config.getString("discord_channel_id")).get();

        ircNetworkBot = new IrcBot(new Configuration.Builder()
                .setName(config.getString("irc_network_bot_name"))
                .addServer(config.getString("irc_host"))
                .addAutoJoinChannel(config.getString("irc_channel"))
                .setNickservPassword(config.getString("irc_network_bot_pass"))
                .addListener(new IrcToGameHandler(this.getProxy(), this, config))
                .setAutoReconnect(true)
                .buildConfiguration(),
                getLogger());

        ircDiscordBot = new IrcBot(new Configuration.Builder()
                .setName(config.getString("irc_discord_bot_name"))
                .addServer(config.getString("irc_host"))
                .addAutoJoinChannel(config.getString("irc_channel"))
                .setNickservPassword(config.getString("irc_discord_bot_pass"))
                .addListener(new IrcToDiscordHandler(config, discordIrcBot))
                .setAutoReconnect(true)
                .buildConfiguration(),
                getLogger());

        ircNetworkBot.startBot();
        ircDiscordBot.startBot();

        DiscordToIrcHandler discordToIrc = new DiscordToIrcHandler(config, getLogger(), ircDiscordBot);
        discordToIrc.startBot();

        getProxy().getPluginManager().registerListener(this, new JoinQuitEventHandler(config, getLogger(), ircNetworkBot, gameChannel));
        getProxy().getPluginManager().registerListener(this, new GameToIrcHandler(config, ircNetworkBot));

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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}