package org.openredstone.bots;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;

import java.io.IOException;
import java.util.logging.Logger;

public class IrcBot {

    PircBotX bot;
    Logger logger;
    Thread botThread;

    public IrcBot(Configuration config, Logger logger) {
        this.bot = new PircBotX(config);
        this.logger = logger;
    }

    public void startBot() {
        // I really don't like this but I eh I'll fix it later.™
        botThread = new Thread(() -> {
            try {
                bot.startBot();
            } catch (IOException | IrcException e) {
                e.printStackTrace();
            }
        });
        botThread.start();
    }

    public void stopBot() {
        bot.close();
        if (botThread.isAlive()) {
            botThread.interrupt();
            botThread = null;
        }
    }

    public void sendMessage(String target, String message) {
        bot.send().message(target, message);
    }
}
