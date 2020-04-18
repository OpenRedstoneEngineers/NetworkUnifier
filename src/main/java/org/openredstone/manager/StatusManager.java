package org.openredstone.manager;

import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageSet;

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
        plugin.getProxy().getScheduler().schedule(this.plugin, this::updateStatus, 2, config.getInt("status_update_frequency"), TimeUnit.SECONDS);
    }

    private void checkServers() {
        for (ServerInfo server : plugin.getProxy().getServers().values()) {
            server.ping((ServerPing result, Throwable error) -> {
                if (error == null) {
                    serversOnline.put(server.getName(), true);
                } else {
                    serversOnline.put(server.getName(), false);
                }
            });
        }
    }

    private void updateStatus() {
        StringBuilder message = new StringBuilder();
        message.append("**Status**:\n**").append(plugin.getProxy().getPlayers().size()).append("** Player(s) online:\n");
        for (ServerInfo server : plugin.getProxy().getServers().values()) {
            if (serversOnline.containsKey(server.getName()) && serversOnline.get(server.getName())) {
                message.append("**").append(server.getName()).append("** ");
                Collection<ProxiedPlayer> players = server.getPlayers();
                if (players.size() == 0) {
                    message.append("(**0**)");
                } else {
                    message.append("(**").append(players.size()).append("**) : `");
                    String prefix = "";
                    for (ProxiedPlayer player : players) {
                        message.append(prefix);
                        prefix = ", ";
                        message.append(player.getName());
                    }
                    message.append("`");
                }
            } else {
                message.append("**").append(server.getName()).append("** is offline");
            }
            message.append("\n");
        }
        try {
            MessageSet messages = ((ServerTextChannel) this.channel).getMessages(1).get();
            if (messages.isEmpty()) {
                ((ServerTextChannel) this.channel).sendMessage(message.toString());
            } else {
                if (messages.getNewestMessage().isPresent()) {
                    messages.getNewestMessage().get().edit(message.toString());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
