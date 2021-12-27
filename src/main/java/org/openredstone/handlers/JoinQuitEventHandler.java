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
    Configuration config;

    String greeting;
    String farewell;
    List<String> greetings;
    List<String> farewells;

    String ircChannel;

    boolean quirkyMessages;
    boolean specialFarewellGreetings;
    int quirkyMessageFrequency;
    String quirkyGreeting;
    String quirkyFarewell;

    public JoinQuitEventHandler(Configuration config, Logger logger, IrcBot bot, Channel gameChannel) {
        super();
        this.config = config;
        this.bot = bot;
        this.gameChannel = gameChannel;
        this.logger = logger;

        greeting = config.getString("greeting_message");
        farewell = config.getString("farewell_message");

        if (specialFarewellGreetings = config.getBoolean("enable_special_farewells_and_greetings")) {
            farewells = config.getStringList("message_farewells");
            greetings = config.getStringList("message_greetings");
            logger.info("Loaded Farewells: " + farewells.toString());
            logger.info("Loaded Greetings: " + greetings.toString());
        }
        if (quirkyMessages = config.getBoolean("enable_special_quirky_message")) {
            quirkyMessageFrequency = config.getInt("quirky_message_frequency");
            quirkyGreeting = config.getString("quirky_greeting_message");
            quirkyFarewell = config.getString("quirky_farewell_message");
        }
        ircChannel = config.getString("irc_channel");
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
            message = getQuirkyGreeting(name);
        } else {
            message = getGreeting(name);
        }
        if (config.getBoolean("irc_enabled")) sendToIrc(message);
        if (config.getBoolean("discord_enabled")) sendToDiscord(message);
    }

    private void sendQuits(String name) {
        String message;
        if (quirkyMessages && (rand.nextInt(quirkyMessageFrequency) == 1)) {
            message = getQuirkyFarewell(name);
        } else {
            message = getFarewell(name);
        }
        if (config.getBoolean("irc_enabled")) sendToIrc(message);
        if (config.getBoolean("discord_enabled")) sendToDiscord(message);
    }

    private String getQuirkyGreeting(String name) {
        return quirkyGreeting.replace("%USER%", name);
    }

    private String getQuirkyFarewell(String name) {
        return quirkyFarewell.replace("%USER%", name);
    }

    private String getFarewell(String name) {
        if (specialFarewellGreetings) {
            return farewell.replace("%USER%", name).replace("%QUIRKY%", getRandomFarewell());
        } else {
            return farewell.replace("%USER%", name);
        }
    }

    private String getGreeting(String name) {
        if (specialFarewellGreetings) {
            return greeting.replace("%USER%", name).replace("%QUIRKY%", getRandomGreeting());
        } else {
            return greeting.replace("%USER%", name);
        }
    }

    private void sendToIrc(String message) {
        try{
            bot.sendMessage(ircChannel, "\u000307" + message);
        } catch (IllegalArgumentException ignored) {

        }
    }

    private void sendToDiscord(String message) {
        ((ServerTextChannel) gameChannel).sendMessage("**" + message + "**");
    }

    public String getRandomFarewell() {
        return farewells.get(rand.nextInt(farewells.size() - 1 ));
    }

    public String getRandomGreeting() {
        return greetings.get(rand.nextInt(greetings.size() - 1 ));
    }
}
