package org.openredstone.handlers;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;
import org.openredstone.bots.IrcBot;

public class GameToIrcHandler implements Listener {

    Configuration config;
    IrcBot bot;

    public GameToIrcHandler(Configuration config, IrcBot bot) {
        this.config = config;
        this.bot = bot;
    }

    @EventHandler
    public void onMessage(ChatEvent e) {

        Thread dispatchMessage = new Thread(() -> {

            if (e.isCancelled()) {
                return;
            }

            ProxiedPlayer pp = (ProxiedPlayer) e.getSender();
            if ((e.getMessage().length() > 0) && (!e.getMessage().startsWith("/"))) {
                try {
                    bot.sendMessage(
                        config.getString("irc_channel"),
                        "\u000307" + pp.getDisplayName() + "\u000f: " +
                            e.getMessage().replaceAll("&([0-9]|[abcdefklmnor])", "")
                                .replaceAll("#[0-9a-fA-F]{6}", "").trim());
                } catch (IllegalArgumentException ignored) {

                }
            }

        });

        dispatchMessage.start();

    }
}
