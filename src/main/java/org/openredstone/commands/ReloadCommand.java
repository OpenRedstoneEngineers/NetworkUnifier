package org.openredstone.commands;

import net.md_5.bungee.api.CommandSender;
import org.openredstone.NetworkUnifier;

public class ReloadCommand extends NetworkUnifierSubCommand{
    public ReloadCommand(String permission) {
        super(permission);
    }

    @Override
    public boolean execute(CommandSender sender) {
        if (!sender.hasPermission(getPermission())) {
            NetworkUnifier.sendMessage(sender, "You do not have permission to run this command.");
            return false;
        }

        NetworkUnifier.unload();
        NetworkUnifier.load();

        NetworkUnifier.sendMessage(sender, "Reloaded NetworkUnifier");

        return true;
    }
}
