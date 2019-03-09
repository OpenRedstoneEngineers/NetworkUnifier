package org.openredstone;

import net.md_5.bungee.config.Configuration;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

public class IrcToDiscordListener extends ListenerAdapter {

    private Configuration config;

    public IrcToDiscordListener(Configuration config) {
        this.config = config;
    }

    @Override
    public void onMessage(MessageEvent event) throws Exception {
        DiscordApi api = new DiscordApiBuilder().setToken(config.getString("discord_irc_bot_token")).login().join();
        Channel channel = api.getServerTextChannelById(config.getString("discord_channel_id")).get();
        if (config.getStringList("irc_ignore_names").contains(event.getUser().getNick())) return;
        ((ServerTextChannel) channel).sendMessage("**" + event.getUser().getNick() + "**: " + event.getMessage());
    }
}