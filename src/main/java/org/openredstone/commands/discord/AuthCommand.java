package org.openredstone.commands.discord;

import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import org.openredstone.managers.AccountManager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuthCommand extends DiscordCommand {

    private AccountManager accountManager;
    private ScheduledExecutorService scheduledExecutorService;

    public AuthCommand(AccountManager accountManager) {
        super("auth", 1, true);
        this.accountManager = accountManager;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void runCommand(MessageCreateEvent event) {
        String rawMessage = event.getMessageContent();
        String arg1 = rawMessage.substring(rawMessage.indexOf(" ")+1);

        String response = "";

        if (accountManager.userIsLinkedByDiscordId(Long.toString(event.getMessageAuthor().getId()))) {
            response = "You are already linked.";
        } else if (!accountManager.validToken(arg1)) {
            response = "Invalid token.";
        } else {
            accountManager.linkDiscordAccount(arg1, Long.toString(event.getMessageAuthor().getId()));

            event.getMessageAuthor().asUser().ifPresent(user -> {
                String ign = accountManager.getIgnFromDiscordId(user.getIdAsString());
                user.updateNickname(event.getServer().get(), ign);
            });

            response = "You are now linked.";
        }

        try {
            Message message = event.getChannel().sendMessage(response).get();
            scheduledExecutorService.schedule(() -> message.delete(), 5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        scheduledExecutorService.schedule(() -> event.getMessage().delete(), 5, TimeUnit.SECONDS);

    }
}
