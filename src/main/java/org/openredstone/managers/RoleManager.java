package org.openredstone.managers;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.track.Track;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class RoleManager {

    private AccountManager accountManager;
    private DiscordApi discordApi;
    private LuckPerms api;
    private String serverId;
    private List<String> tracks;

    public RoleManager(AccountManager accountManager, DiscordApi discordApi, LuckPerms api, String serverId, List<String> tracks) {
        this.accountManager = accountManager;
        this.discordApi = discordApi;
        this.api = api;
        this.serverId = serverId;
        this.tracks = tracks;
    }

    public boolean isGroupTracked(String group) {
        return api.getTrackManager().getLoadedTracks().stream().anyMatch(track -> track.getGroups().contains(group));
    }

    public boolean isTrackTracked(String track) {
        return tracks.contains(track);
    }

    public boolean groupsExistInTrackOnDiscordAlsoThisMethodIsReallyLongButIAmKeepingItToAnnoyPeople() {
        GroupManager groupManager = api.getGroupManager();
        Set<Track> luckTracks = api.getTrackManager().getLoadedTracks();
        Collection<Role> roles = discordApi.getRoles();
        for (String track : tracks) {
            if (luckTracks.stream().noneMatch(e -> e.getName().equals(track))) {
                return false;
            }
            List<String> groups = luckTracks.stream().filter(e -> e.getName().equals(track)).findFirst().get().getGroups();
            for (String group : groups) {
                Group displayName = groupManager.getGroup(group);
                if (roles.stream().noneMatch(role -> role.getName().equals(displayName.getDisplayName()))) {
                    return false;
                }
            }
        }
        return true;
    }

    public void removeTrackedGroups(String userId) {
        try {
            User user = discordApi.getUserById(accountManager.getDiscordId(userId)).get();
            discordApi.getServerById(serverId).ifPresent(server -> {
                List<Role> trackedRoles = filterTrackedRoles(server.getRoles(user));
                trackedRoles.forEach(trackedRole -> server.removeRoleFromUser(user, trackedRole));
            });
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void setTrackedDiscordGroup(String userId, String group) {
        if (discordApi.getRoles().stream().noneMatch(e -> e.getName().equalsIgnoreCase(group))) {
            System.out.println("Invalid configuration. No group by the name of '" + group + "' exists on Discord.");
        }

        discordApi.getServerById(serverId).ifPresent(server -> {
            server.getRoles().stream().filter(role -> role.getName().equalsIgnoreCase(group)).findFirst().ifPresent( role -> {
                if (role.getName().equalsIgnoreCase(group)) {
                    removeTrackedGroups(userId);
                    try {
                        server.addRoleToUser(discordApi.getUserById(accountManager.getDiscordId(userId)).get(), role);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
    }

    private List<Role> filterTrackedRoles(List<Role> usersRoles) {
        Set<Group> groups = api.getGroupManager().getLoadedGroups();
        return usersRoles.stream().filter(role ->
                groups.stream().anyMatch(e -> {
                    return (e.getDisplayName() != null) && (e.getDisplayName().equals(role.getName()));
                })
        ).collect(Collectors.toList());
    }
}
