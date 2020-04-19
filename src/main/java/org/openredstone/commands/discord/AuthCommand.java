package org.openredstone.commands.discord;

import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import org.openredstone.manager.AccountManager;

public class AuthCommand extends DiscordCommand {

    private AccountManager accountManager;

    public AuthCommand(AccountManager accountManager) {
        super("auth", 1, true);
        this.accountManager = accountManager;
    }

    @Override
    public void runCommand(MessageCreateEvent event) {
        String rawMessage = event.getMessageContent();
        String arg1 = rawMessage.substring(rawMessage.indexOf(" ")+1);

        if (!accountManager.tokenExists(arg1)) {
            event.getChannel().sendMessage("Invalid token.");
        } else {
            accountManager.linkDiscordAccount(arg1, Long.toString(event.getMessageAuthor().getId()));
        }

        User user = event.getMessageAuthor().asUser().get();
        String ign = accountManager.getIgnFromDiscordId(user.getIdAsString());

        user.updateNickname(event.getServer().get(), ign).exceptionally(ExceptionLogger.get());

        event.deleteMessage();
    }
}
