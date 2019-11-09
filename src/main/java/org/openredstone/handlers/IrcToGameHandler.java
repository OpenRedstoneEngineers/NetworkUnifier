package org.openredstone.handlers;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IrcToGameHandler extends ListenerAdapter {

    private Configuration config;
    private ProxyServer ps;
    private Plugin p;

    public IrcToGameHandler(ProxyServer ps, Plugin p, Configuration config) {
        this.config = config;
        this.ps = ps;
        this.p = p;
    }

    public static TextComponent renderTextComponent(String raw) {
        String urlRegex = "(http|https)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
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
        sendToGame(event.getUser().getNick(), "§cIRC §7| §f%USER%§7:§r " + event.getMessage().replaceAll("§", "&"));
    }
    @Override
    public void onJoin(JoinEvent event) {
        sendToGame(event.getUser().getNick(), "§cIRC §7| %USER% joined IRC");
    }

    @Override
    public void onQuit(QuitEvent event) {
        sendToGame(event.getUser().getNick(),"§cIRC §7| %USER% left IRC");
    }

    @Override
    public void onTopic(TopicEvent event) {
        sendToGame(event.getUser().getNick(), "§cIRC §7| %USER% set the topic to: " + event.getTopic());
    }

    @Override
    public void onKick(KickEvent event) {
        sendToGame(event.getRecipient().getNick(), "§cIRC §7| %USER% was kicked from IRC");
    }

    @Override
    public void onPrivateMessage(PrivateMessageEvent event) throws Exception {
        String user = event.getMessage().substring(0,event.getMessage().indexOf(" "));
        String message = event.getMessage().substring(event.getMessage().indexOf(" "));
        for (ProxiedPlayer player : ps.getPlayers()) {
            if (player.getDisplayName().equals(user)) {
                player.sendMessage(new TextComponent(message));
                event.respondPrivateMessage("Sent to " + user + ": " + event.getMessage());
                return;
            }
        }
        event.respondPrivateMessage("User \"" + user + "\" not found.");
    }

    private void sendToGame(String user, String message) {
        if (config.getStringList("irc_to_game_ignore_names").contains(user)) {
            return;
        }

        String messageToSend = message.replaceAll("%USER%", user);
        ps.getScheduler().runAsync(p, () -> {
            BaseComponent[] bs = (new ComponentBuilder(renderTextComponent(messageToSend))).create();
            for (ProxiedPlayer player : ps.getPlayers()) {
                player.sendMessage(bs);
            }
        });
    }
}