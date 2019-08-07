package org.openredstone.commands;

import net.md_5.bungee.api.CommandSender;

public abstract class NetworkUnifierSubCommand {

    private final String permission;

    public NetworkUnifierSubCommand(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }

    public abstract boolean execute(CommandSender sender);
}
