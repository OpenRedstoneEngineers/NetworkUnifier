package org.openredstone.handlers;

import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.openredstone.bots.IrcBot;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class JoinQuitEventHandler implements Listener {

    Random rand = new Random();
    IrcBot bot;
    Channel gameChannel;
    Logger logger;

    List<String> greetings;
    List<String> farewells;

    String ircChannel;

    boolean quirkyMessages;
    boolean specialFarewellGreetings;
    int quirkyMessageFrequency;

    public JoinQuitEventHandler(Configuration config, Logger logger, IrcBot bot, Channel gameChannel) {
        super();
        this.bot = bot;
        this.gameChannel = gameChannel;
        this.logger = logger;
        if (specialFarewellGreetings = config.getBoolean("enable_special_farewells_and_greetings")) {
            farewells = config.getStringList("message_farewells");
            greetings = config.getStringList("message_greetings");
            logger.info("Loaded Farewells: " + farewells.toString());
            logger.info("Loaded Greetings: " + greetings.toString());
        }
        if (quirkyMessages = config.getBoolean("enable_special_quirky_message")) {
            quirkyMessageFrequency = config.getInt("quirky_message_frequency");
        }
        ircChannel = config.getString("irc_channel");
        logger.info("Sending game greetings to #" + ircChannel + " on IRC and #" + gameChannel.toString() + " on Discord.");
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        Thread postLoginThread = new Thread(() -> {
            sendJoins(event.getPlayer().getDisplayName());
        });
        postLoginThread.start();
    }

    @EventHandler
    public void onQuitEvent(PlayerDisconnectEvent event) {
        Thread postQuitThread = new Thread(() -> {
            sendQuits(event.getPlayer().getDisplayName());
        });
        postQuitThread.start();
    }

    private void sendJoins(String name) {
        String message;
        if (quirkyMessages && (rand.nextInt(quirkyMessageFrequency) == 1)) {
            message = "Running " + name + ".exe ...";
        } else if (specialFarewellGreetings) {
            message = name + " joined the network." + getRandomWelcome();
        } else {
            message = name + " joined the network.";
        }
        sendToIrc(message);
        sendToDiscord(message);
    }

    private void sendQuits(String name) {
        String message;
        if (quirkyMessages && (rand.nextInt(quirkyMessageFrequency) == 1)) {
            message = name + ".exe has stopped working.";
        } else if (specialFarewellGreetings) {
            message = name + " left the network." + getRandomFarewell();
        } else {
            message = name + " left the network.";
        }
        sendToIrc(message);
        sendToDiscord(message);
    }

    private void sendToIrc(String message) {
        bot.sendMessage(ircChannel, "\u000307" + message);
    }

    private void sendToDiscord(String message) {
        ((ServerTextChannel) gameChannel).sendMessage("**" + message + "**");
    }

    public String getRandomFarewell() {
        return farewells.get(rand.nextInt(farewells.size() - 1 ));
    }

    public String getRandomWelcome() {
        return greetings.get(rand.nextInt(greetings.size() - 1 ));
    }
}
