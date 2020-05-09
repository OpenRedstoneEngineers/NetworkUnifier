package org.openredstone.managers;

import net.luckperms.api.LuckPerms;
import org.javacord.api.DiscordApi;
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
            if (event.getMessageContent().charAt(0) != commandChar) {
                return;
            }

            String rawMessage = event.getMessageContent();

            if (!rawMessage.contains(" ")) {
                rawMessage += " ";
            }

            String arg0 = rawMessage.substring(1, rawMessage.indexOf(" "));
            if (commands.stream().anyMatch(command -> command.getCommand().equals(arg0))) {
                commands.stream().filter(command -> command.getCommand().equals(arg0)).findFirst().get().runCommand(event);
            } else {
                event.getChannel().sendMessage("Invalid command.");
            }

        });
    }
}