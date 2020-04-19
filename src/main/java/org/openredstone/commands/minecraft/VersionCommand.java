package org.openredstone.commands.minecraft;

import net.md_5.bungee.api.CommandSender;
import org.openredstone.NetworkUnifier;

public class VersionCommand extends NetworkUnifierSubCommand {
    public VersionCommand(String permission) {
        super(permission);
    }

    @Override
    public boolean execute(CommandSender sender) {
        if (!sender.hasPermission(getPermission())) {
            NetworkUnifier.sendMessage(sender, "You do not have permission to run this command.");
            return false;
        }

        NetworkUnifier.sendMessage(sender, "Version " + NetworkUnifier.getVersion());

        return true;
    }
}
