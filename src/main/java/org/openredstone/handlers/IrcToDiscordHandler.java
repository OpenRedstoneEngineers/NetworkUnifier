package org.openredstone.handlers;

import net.md_5.bungee.config.Configuration;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.QuitEvent;
import org.pircbotx.hooks.events.TopicEvent;
import org.pircbotx.hooks.events.KickEvent;

public class IrcToDiscordHandler extends ListenerAdapter {

    private Configuration config;
    Channel channel;

    public IrcToDiscordHandler(Configuration config, DiscordApi api) {
        this.config = config;
        this.channel = api.getServerTextChannelById(config.getString("discord_channel_id")).get();
    }

    @Override
    public void onMessage(MessageEvent event) throws Exception {
        sendToDiscord(event.getUser().getNick(), "**%USER%**: " + event.getMessage());
    }

    @Override
    public void onJoin(JoinEvent event) {
        sendToDiscord(event.getUser().getNick(), "**%USER%** joined IRC");
    }

    @Override
    public void onQuit(QuitEvent event) {
        sendToDiscord(event.getUser().getNick(),"**%USER%** left IRC");
    }

    @Override
    public void onTopic(TopicEvent event) {
        ((ServerTextChannel) channel).updateTopic(event.getTopic());
    }

    @Override
    public void onKick(KickEvent event) {
        sendToDiscord(event.getRecipient().getNick(), "**%USER%** was kicked from IRC");
    }

    public void sendToDiscord(String user, String message) {
        if (config.getStringList("irc_to_discord_ignore_names").contains(user)) {
            return;
        }

        message = message.replaceAll("%USER%", user);
        ((ServerTextChannel) channel).sendMessage(message);
    }
}