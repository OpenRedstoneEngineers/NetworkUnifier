package org.openredstone.bots;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;

import java.io.IOException;
import java.util.logging.Logger;

public class IrcBot {

    PircBotX bot;
    Logger logger;

    public IrcBot(Configuration config, Logger logger) {
        this.bot = new PircBotX(config);
        this.logger = logger;
    }

    public void startBot() {
        Thread botThread = new Thread(() -> {
            try {
                bot.startBot();
            } catch (IOException | IrcException e) {
                logger.info(e.toString());
            }
        });
        botThread.start();
    }

    public void sendMessage(String target, String message) {
        bot.send().message(target, message);
    }
}
