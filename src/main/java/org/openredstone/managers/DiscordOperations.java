package org.openredstone.managers;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.track.Track;
import net.luckperms.api.track.TrackManager;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class DiscordOperations {
    private final DiscordApi discordApi;
    private final LuckPerms api;
    private final String serverId;
    private final List<String> tracks;

    public DiscordOperations(DiscordApi discordApi, LuckPerms api, String serverId, List<String> tracks) {
        this.discordApi = discordApi;
        this.api = api;
        this.serverId = serverId;
        this.tracks = tracks;
    }

    public boolean isGroupInATrack(String group) {
        return api.getTrackManager().getLoadedTracks().stream().anyMatch(track -> track.getGroups().contains(group));
    }

    public @Nullable String groupsExistInTrackOnDiscordAlsoThisMethodIsReallyLongButIAmKeepingItToAnnoyPeopleAndIJustMadeItALittleBitLongerSmileyFace() {
        GroupManager groupManager = api.getGroupManager();
        TrackManager trackManager = api.getTrackManager();
        Set<String> roleNames = discordApi.getRoles().stream().map(Role::getName).collect(toSet());
        for (String trackName : tracks) {
            Track track = trackManager.getTrack(trackName);
            if (track == null) {
                return "Track " + trackName + " does not exist in LuckPerms!";
            }
            List<String> groups = track.getGroups();
            for (String groupName : groups) {
                Group group = groupManager.getGroup(groupName);
                if (group == null) {
                    // we literally just got these groups from the track
                    return "LuckPerms shat itself!";
                }
                if (!roleNames.contains(group.getDisplayName())) {
                    return "LuckPerms group " + groupName + " (within track " + trackName + ") does not have a corresponding Discord role!";
                }
            }
        }
        return null;
    }

    public void removeTrackedGroups(String discordId) {
        try {
            User user = discordApi.getUserById(discordId).get();
            discordApi.getServerById(serverId).ifPresent(server -> {
                List<Role> trackedRoles = filterTrackedRoles(server.getRoles(user));
                trackedRoles.forEach(trackedRole -> server.removeRoleFromUser(user, trackedRole));
            });
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void setTrackedDiscordGroup(String discordId, String group) {
        if (discordApi.getRoles().stream().noneMatch(e -> e.getName().equalsIgnoreCase(group))) {
            System.out.println("Invalid configuration. No group by the name of '" + group + "' exists on Discord.");
        }

        discordApi.getServerById(serverId).ifPresent(server -> {
            server.getRoles().stream().filter(role -> role.getName().equalsIgnoreCase(group)).findFirst().ifPresent( role -> {
                if (role.getName().equalsIgnoreCase(group)) {
                    removeTrackedGroups(discordId);
                    try {
                        server.addRoleToUser(discordApi.getUserById(discordId).get(), role);
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
                groups.stream().anyMatch(group ->
                    (group.getDisplayName() != null) && group.getDisplayName().equals(role.getName())
                )
        ).collect(toList());
    }

    public void setNickname(String discordId, String name) {
        discordApi.getServerById(serverId).ifPresent(server -> {
            try {
                server.updateNickname(discordApi.getUserById(discordId).get(), name);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }
}
