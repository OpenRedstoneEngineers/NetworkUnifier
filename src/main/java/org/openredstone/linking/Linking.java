package org.openredstone.linking;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import org.jetbrains.annotations.NotNull;
import org.openredstone.managers.DiscordOperations;

import java.util.UUID;

public class Linking {
    private final UserDatabase userDatabase;
    private final Tokens tokens;
    private final DiscordOperations discordOperations;
    private final LuckPerms luckPerms;

    public Linking(Tokens tokens, UserDatabase userDatabase, DiscordOperations discordOperations, LuckPerms api) {
        this.userDatabase = userDatabase;
        this.tokens = tokens;
        this.discordOperations = discordOperations;
        this.luckPerms = api;
    }

    @NotNull
    public String startLinking(UUID uuid, String name) {
        LinkedUser user = userDatabase.userByMinecraftUuid(uuid);
        if (user != null) {
            return "You are already linked to Discord.";
        }
        String token = tokens.createFor(new UnlinkedUser(uuid, name));
        return "Type \"!auth " + token + "\" anywhere on our Discord to finish validating your account.";
    }

    @NotNull
    public String finishLinking(String discordId, String token) {
        if (userDatabase.userByDiscordId(discordId) != null) {
            return "You are already linked.";
        }

        UnlinkedUser unlinked = tokens.tryConsume(token);
        if (unlinked == null) {
            return "Invalid token.";
        }

        LinkedUser user = unlinked.withLink(discordId);
        userDatabase.createUser(user);

        String primaryGroup = luckPerms.getUserManager().getUser(user.uuid).getPrimaryGroup();
        discordOperations.setTrackedDiscordGroup(user.discordId, luckPerms.getGroupManager().getGroup(primaryGroup).getDisplayName());
        discordOperations.setNickname(discordId, user.name);

        return "You are now linked.";
    }

    public void postLogin(UUID userId, String name) {
        // on login: update name in db and set nickname on discord, if changed
        LinkedUser user = userDatabase.userByMinecraftUuid(userId);
        if (user != null && !user.name.equals(name)) {
            userDatabase.updateUserName(userId, name);
            discordOperations.setNickname(user.discordId, name);
        }
    }

    // - on luckperms user update
    // - when user is linked
    // - refresh user's tracked discord roles
    public void userUpdate(UUID userId) {
        LinkedUser userDto = userDatabase.userByMinecraftUuid(userId);
        if (userDto == null) {
            return;
        }

        UserManager userManager = luckPerms.getUserManager();
        User user = userManager.getUser(userId);
        if (user == null) {
            return;
        }
        if (discordOperations.isGroupInATrack(user.getPrimaryGroup())) {
            Group primaryGroup = luckPerms.getGroupManager().getGroup(user.getPrimaryGroup());
            discordOperations.setTrackedDiscordGroup(userDto.discordId, primaryGroup.getDisplayName());
        } else {
            discordOperations.removeTrackedGroups(userDto.discordId);
        }
    }

    // member join
    // - when user is linked
    // - set discord nick name (saved to db earlier)
    // - when primary luckperms group is tracked
    // - refresh user's tracked discord role
    public void memberJoin(String id) {
        // nickName, trackedDiscordGroup
        LinkedUser userDto = userDatabase.userByDiscordId(id);
        if (userDto == null) {
            return;
        }
        discordOperations.setNickname(userDto.discordId, userDto.name);
        User user = luckPerms.getUserManager().getUser(userDto.uuid);
        if (user == null) {
            return;
        }
        String primaryGroup = user.getPrimaryGroup();
        if (!discordOperations.isGroupInATrack(primaryGroup)) {
            return;
        }
        Group luckPrimaryGroup = luckPerms.getGroupManager().getGroup(primaryGroup);
        if (luckPrimaryGroup == null) {
            return;
        }
        discordOperations.setTrackedDiscordGroup(id, luckPrimaryGroup.getDisplayName());
    }
}
