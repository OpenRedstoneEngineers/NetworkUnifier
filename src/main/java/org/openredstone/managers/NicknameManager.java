package org.openredstone.managers;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.server.Server;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class NicknameManager {

    private DiscordApi discordApi;
    private AccountManager accountManager;
    private String serverId;

    public NicknameManager(DiscordApi discordApi, AccountManager accountManager, String serverId) {
        this.discordApi = discordApi;
        this.accountManager = accountManager;
        this.serverId = serverId;
    }

    public Optional<String> getNickname(String userId) {
        Optional<Server> server = discordApi.getServerById(serverId);
        if (server.isPresent()) {
            try {
                return server.get().getNickname(discordApi.getUserById(accountManager.getDiscordId(userId)).get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return Optional.empty();
    }

    public void setNickname(String userId, String name) {
        discordApi.getServerById(serverId).ifPresent(server -> {
            try {
                server.resetNickname(discordApi.getUserById(accountManager.getDiscordId(userId)).get(), name);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }
}
