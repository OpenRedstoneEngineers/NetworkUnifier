package org.openredstone.managers;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
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

    public boolean isTracked(String track) {
        return tracks.contains(track);
    }

    public boolean groupsExistInTrackOnDiscordAlsoThisMethodIsReallyLongButIAmKeepingItToAnnoyPeople() {
        Set<Track> luckTracks = api.getTrackManager().getLoadedTracks();
        Collection<Role> roles = discordApi.getRoles();
        for (String track : tracks) {
            if (luckTracks.stream().noneMatch(e -> e.getName().equals(track))) {
                return false;
            }
            List<String> groups = luckTracks.stream().filter(e -> e.getName().equals(track)).findFirst().get().getGroups();
            for (String group : groups) {
                if (roles.stream().noneMatch(role -> role.getName().equals(group))) {
                    return false;
                }
            }
        }
        return true;
    }

    public void setDiscordGroup(String userId, String group) {
        if (discordApi.getRoles().stream().noneMatch(e -> e.getName().equals(group))) {
            System.out.println("Invalid configuration. No group by the name of '" + group + "' exists on Discord.");
        }

        Role role = discordApi.getRoles().stream().filter(e -> e.getName().equals(group)).findFirst().get();

        try {
            User user = discordApi.getUserById(accountManager.getDiscordId(userId)).get();
            discordApi.getServerById(serverId).ifPresent(server -> {
                List<Role> trackedRoles = filterTrackedRoles(server.getRoles(user));
                trackedRoles.forEach(trackedRole -> server.removeRoleFromUser(user, trackedRole));
                server.addRoleToUser(user, role);
            });
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private List<Role> filterTrackedRoles(List<Role> usersRoles) {
        Set<Group> groups = api.getGroupManager().getLoadedGroups();
        return usersRoles.stream().filter(role ->
                groups.stream().anyMatch(e ->
                        e.getName().equals(role.getName())
                )
        ).collect(Collectors.toList());
    }
}
