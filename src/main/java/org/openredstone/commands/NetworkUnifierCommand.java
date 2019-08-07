package org.openredstone.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class NetworkUnifierCommand extends Command {

    public NetworkUnifierCommand(String name, String permission, String... aliases) {
        super(name, permission, aliases);
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (strings.length == 1) {
            new VersionCommand("networkunifier.version").execute(commandSender);
            return;
        }

        switch (strings[2]) {
            case "reload":
                new ReloadCommand("networkunifier.reload").execute(commandSender);
                return;
            case "version":
                new VersionCommand("networkunifier.version").execute(commandSender);
        }

    }

}
