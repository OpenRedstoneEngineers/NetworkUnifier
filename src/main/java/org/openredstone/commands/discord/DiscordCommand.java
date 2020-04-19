package org.openredstone.commands.discord;

import org.javacord.api.event.message.MessageCreateEvent;

public abstract class DiscordCommand {

    private String command;
    private int argCount;
    private boolean delete;

    public DiscordCommand(String command, int argCount, boolean delete) {
        this.command = command;
        this.argCount = argCount;
        this.delete = delete;
    }

    public abstract void runCommand(MessageCreateEvent event);

    public String getCommand() {
        return command;
    }

    public int getArgCount() {
        return argCount;
    }

    public boolean isDelete() {
        return delete;
    }
}
