package org.openredstone.linking;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.openredstone.NetworkUnifier;
import org.openredstone.linking.Linking;

public class DiscordCommand extends Command {
    private final Linking linking;

    public DiscordCommand(Linking linking, String name, String permission, String... aliases) {
        super(name, permission, aliases);
        this.linking = linking;
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            NetworkUnifier.sendMessage(commandSender, "This command can only be ran from in game.");
            return;
        }
        ProxiedPlayer proxiedPlayer = (ProxiedPlayer) commandSender;
        String response = linking.startLinking(proxiedPlayer.getUniqueId(), proxiedPlayer.getName());
        NetworkUnifier.sendMessage(commandSender, response);
    }
}
