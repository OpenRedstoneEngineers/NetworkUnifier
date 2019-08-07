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
        try {
            bot.startBot();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IrcException e) {
            e.printStackTrace();
        }
    }

    public void stopBot() {
        bot.close();
    }

    public void sendMessage(String target, String message) {
        bot.send().message(target, message);
    }
}
