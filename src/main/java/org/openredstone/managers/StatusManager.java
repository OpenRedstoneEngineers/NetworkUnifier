package org.openredstone.managers;

import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.util.logging.ExceptionLogger;

import java.awt.*;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class StatusManager {

    private Channel channel;
    private Plugin plugin;

    private final HashMap<String, Boolean> serversOnline = new HashMap<>();

    public StatusManager(Configuration config, DiscordApi api, Plugin plugin) {
        this.channel = api.getServerTextChannelById(config.getString("discord_status_channel_id")).get();
        this.plugin = plugin;
        plugin.getProxy().getScheduler().schedule(this.plugin, this::checkServers, 1, config.getInt("status_update_frequency"), TimeUnit.SECONDS);
        plugin.getProxy().getScheduler().schedule(this.plugin, () -> updateStatus(generateStatusMessage()), 2, config.getInt("status_update_frequency"), TimeUnit.SECONDS);
    }

    private void checkServers() {
        for (ServerInfo server : plugin.getProxy().getServers().values()) {
            server.ping((ServerPing result, Throwable error) -> {
                serversOnline.put(server.getName(), error == null);
            });
        }
    }

    private EmbedBuilder generateStatusMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setColor(Color.decode("#cd2d0a"))
                .setTitle("Status")
                .addField("**Players Online**", String.valueOf(plugin.getProxy().getPlayers().size()))
                .setThumbnail("https://openredstone.org/wp-content/uploads/2018/07/icon-mini.png")
                .setTimestamp(Instant.now());

        for (ServerInfo server : plugin.getProxy().getServers().values()) {
            if (serversOnline.containsKey(server.getName()) && serversOnline.get(server.getName())) {
                Collection<ProxiedPlayer> players = server.getPlayers();
                if (players.isEmpty()) {
                    embedBuilder.addField(server.getName() + " (**0**)", "☹");
                } else {
                    StringBuilder message = new StringBuilder();
                    message.append("`");
                    String prefix = "";
                    for (ProxiedPlayer player : players) {
                        message.append(prefix);
                        prefix = ", ";
                        message.append(player.getName());
                    }
                    message.append("`");
                    embedBuilder.addField(server.getName() + " (**" + players.size() + "**)", message.toString());
                }
            } else {
                embedBuilder.addField(server.getName() + " is **offline**",  "☠");
            }
        }
        return embedBuilder;
    }

    private void updateStatus(EmbedBuilder embedBuilder) {
        try {
            MessageSet messages = ((ServerTextChannel) this.channel).getMessages(1).get();
            if (messages.isEmpty()) {
                ((ServerTextChannel) this.channel).sendMessage(embedBuilder).exceptionally(ExceptionLogger.get());
            } else {
                if (messages.getNewestMessage().isPresent()) {
                    messages.getNewestMessage().get().edit("");
                    messages.getNewestMessage().get().edit(embedBuilder);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
