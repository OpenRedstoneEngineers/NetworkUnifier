package org.openredstone;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

public class IrcToGameListener extends ListenerAdapter {

    private Configuration config;
    private ProxyServer ps;
    private Plugin p;

    public IrcToGameListener(ProxyServer ps, Plugin p, Configuration config) {
        this.config = config;
        this.ps = ps;
        this.p = p;
    }

    @Override
    public void onMessage(MessageEvent event) throws Exception {
        if (config.getStringList("irc_ignore_names").contains(event.getUser().getNick())) return;
        ps.getScheduler().runAsync(p, new Runnable() {
            @Override
            public void run() {
                TextComponent bs = new TextComponent("§cIRC §7| §f" + event.getUser().getNick() + "§7:§r " + event.getMessage());
                for (ProxiedPlayer player : ps.getPlayers()) {
                    player.sendMessage(bs);
                }
            }
        });
    }
}