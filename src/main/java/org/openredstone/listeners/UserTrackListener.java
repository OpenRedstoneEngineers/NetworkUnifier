package org.openredstone.listeners;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.user.track.UserTrackEvent;
import org.openredstone.managers.RoleManager;

public class UserTrackListener {

    private RoleManager roleManager;

    public UserTrackListener(RoleManager roleManager, LuckPerms api) {
        this.roleManager = roleManager;

        EventBus eventBus = api.getEventBus();

        eventBus.subscribe(UserTrackEvent.class, this::onUserTrack);
    }

    private void onUserTrack(UserTrackEvent event) {
        if (!roleManager.isTracked(event.getTrack().getName())) {
            return;
        }

        String groupTo = event.getGroupTo().get();
        String userId = event.getUser().getUniqueId().toString();
        roleManager.setDiscordGroup(userId, groupTo);
    }
}