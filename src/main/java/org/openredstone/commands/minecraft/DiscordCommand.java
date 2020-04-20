package org.openredstone.commands.minecraft;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.openredstone.NetworkUnifier;
import org.openredstone.managers.AccountManager;

public class DiscordCommand extends Command {

    private AccountManager accountManager;

    public DiscordCommand(AccountManager accountManager, String name, String permission, String... aliases) {
        super(name, permission, aliases);
        this.accountManager = accountManager;
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            NetworkUnifier.sendMessage(commandSender, "This command can only be ran from in game.");
            return;
        }

        ProxiedPlayer proxiedPlayer = (ProxiedPlayer) commandSender;

        if (!proxiedPlayer.hasPermission(getPermission())) {
            NetworkUnifier.sendMessage(commandSender, "You do not have permission to run this command.");
            return;
        }

        String token = accountManager.createAccount(proxiedPlayer.getUniqueId().toString(), proxiedPlayer.getName());

        if (token != null) {
            NetworkUnifier.sendMessage(commandSender, "Type \"!auth " + token + "\" anywhere on our Discord to finish validating your account.");
        } else {
            NetworkUnifier.sendMessage(commandSender, "There was an issue processing this command.");
        }

    }
}
