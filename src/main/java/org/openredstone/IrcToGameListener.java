package org.openredstone;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IrcToGameListener extends ListenerAdapter {

    private Configuration config;
    private ProxyServer ps;
    private Plugin p;

    public IrcToGameListener(ProxyServer ps, Plugin p, Configuration config) {
        this.config = config;
        this.ps = ps;
        this.p = p;
    }

    public static TextComponent renderTextComponent(String raw) {
        String urlRegex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
        Matcher urlMatcher = pattern.matcher(raw);

        TextComponent textBuilder = new TextComponent();
        int lastIndex = 0;

        while (urlMatcher.find()) {
            int startingIndex = urlMatcher.start();
            int endingIndex = urlMatcher.end();
            TextComponent prefix = new TextComponent(raw.substring(lastIndex, startingIndex));
            textBuilder.addExtra(prefix);
            TextComponent url = new TextComponent(raw.substring(startingIndex, endingIndex));
            url.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Visit " + raw.substring(startingIndex, endingIndex)).create()));
            url.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, raw.substring(startingIndex, endingIndex)));
            textBuilder.addExtra(url);
            lastIndex = endingIndex;
        }
        textBuilder.addExtra(new TextComponent(raw.substring(lastIndex)));
        return textBuilder;
    }

    @Override
    public void onMessage(MessageEvent event) throws Exception {
        if (config.getStringList("irc_to_game_ignore_names").contains(event.getUser().getNick())) return;
        ps.getScheduler().runAsync(p, new Runnable() {
            @Override
            public void run() {
                TextComponent bs = new TextComponent(renderTextComponent("§cIRC §7| §f" + event.getUser().getNick() + "§7:§r " + event.getMessage()));
                for (ProxiedPlayer player : ps.getPlayers()) {
                    player.sendMessage(bs);
                }
            }
        });
    }
}