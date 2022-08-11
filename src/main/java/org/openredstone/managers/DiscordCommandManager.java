package org.openredstone.managers;

import net.luckperms.api.LuckPerms;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.message.MessageCreateEvent;
import org.openredstone.commands.discord.AuthCommand;
import org.openredstone.commands.discord.DiscordCommand;

import java.util.ArrayList;

public class DiscordCommandManager {

    private DiscordApi discordApi;
    private char commandChar;
    private ArrayList<DiscordCommand> commands = new ArrayList<>();

    public DiscordCommandManager(DiscordApi discordApi, AccountManager accountManager, RoleManager roleManager, LuckPerms api, char commandChar) {
        this.discordApi = discordApi;
        this.commandChar = commandChar;
        commands.add(new AuthCommand(accountManager, roleManager, api));
        registerCommandListeners();
    }

    private void registerCommandListeners() {
        discordApi.addMessageCreateListener(event -> {
            String rawMessage = event.getMessageContent();
            // require at least commandChar + 1 char command = 2 chars
            if (rawMessage.length() < 2 || event.getMessageContent().charAt(0) != commandChar) {
                return;
            }
            String commandName = rawMessage.split(" ")[0].substring(1);
            commands
                    .stream()
                    .filter(command -> command.getCommand().equals(commandName))
                    .findFirst()
                    .orElse(unknownCommand)
                    .runCommand(event);
        });
    }

    private final DiscordCommand unknownCommand = new DiscordCommand("unknown", 0, false) {
        @Override
        public void runCommand(MessageCreateEvent event) {
            event.getChannel().sendMessage("Invalid command.");
        }
    };
}
