package org.openredstone.handlers;

import net.md_5.bungee.config.Configuration;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.openredstone.bots.IrcBot;

import java.util.List;
import java.util.logging.Logger;

public class DiscordToIrcHandler {

    Configuration config;
    Logger logger;
    IrcBot bot;

    List<String> ignoredDiscordIds;

    DiscordApiBuilder discordApi;
    DiscordApi discordBot;

    public DiscordToIrcHandler(Configuration config, Logger logger, IrcBot bot) {
        this.config = config;
        this.logger = logger;
        this.bot = bot;
        ignoredDiscordIds = config.getStringList("discord_ignore_ids");
        logger.info("Ignoring Discord IDs: " + ignoredDiscordIds.toString());
        discordApi = new DiscordApiBuilder().setToken(config.getString("discord_irc_bot_token"));

    }

    public void startBot() {
        discordBot = discordApi.login().join();
        discordBot.addMessageCreateListener(event -> {
            if (!event.getChannel().getIdAsString().equals(config.getString("discord_channel_id"))) {
                return;
            }

            if ((!event.getMessageContent().isEmpty() || !event.getMessageAttachments().isEmpty()) && !ignoredDiscordIds.contains(event.getMessageAuthor().getIdAsString())) {
                List<User> mentionedUsers = event.getMessage().getMentionedUsers();
                List<CustomEmoji> mentionedEmojis = event.getMessage().getCustomEmojis();
                List<Role> mentionedRoles = event.getMessage().getMentionedRoles();
                List<MessageAttachment> attachments = event.getMessageAttachments();

                String message = event.getMessageContent();

                for (User user : mentionedUsers) {
                    String toReplace = "<@!?" + user.getIdAsString() + ">";
                    message = message.replaceAll(toReplace, "@" + user.getDisplayName(user.getMutualServers().iterator().next()));
                }

                for (CustomEmoji emoji : mentionedEmojis) {
                    String toReplace = "<a?:" + emoji.getName() + ":" + emoji.getIdAsString() + ">";
                    message = message.replaceAll(toReplace, ":"+ emoji.getName() + ":");
                }

                for (Role role : mentionedRoles) {
                    String toReplace = "<@&" + role.getIdAsString() + ">";
                    message = message.replaceAll(toReplace, "@" + role.getName());
                }

                StringBuilder attachmentUrls = new StringBuilder();
                for (MessageAttachment a : attachments) {
                    attachmentUrls.append(" ").append(a.getUrl());
                }

                message += attachmentUrls.toString();
                message = message.trim();

                bot.sendMessage(config.getString("irc_channel"), "\u000307" + event.getMessageAuthor().getDisplayName() + "\u000f: " + message);

            } else if (event.getMessageContent().isEmpty() && (event.getMessage().getEmbeds().size() > 0)){
                for (Embed embed : event.getMessage().getEmbeds()) {
                    bot.sendMessage(config.getString("irc_channel"), "\u000307" + embed.getUrl().toString());
                }
            }
        });
    }

    public void stopBot() {
        discordBot.disconnect();
    }
}
