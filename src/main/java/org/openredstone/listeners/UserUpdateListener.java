package org.openredstone.listeners;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.UserManager;
import org.openredstone.managers.AccountManager;
import org.openredstone.managers.RoleManager;

import java.util.Optional;
import java.util.UUID;

public class UserUpdateListener {

    private RoleManager roleManager;
    private AccountManager accountManager;
    private LuckPerms api;

    public UserUpdateListener(RoleManager roleManager, AccountManager accountManager, LuckPerms api) {
        this.roleManager = roleManager;
        this.accountManager = accountManager;
        this.api = api;

        EventBus eventBus = api.getEventBus();

        eventBus.subscribe(UserDataRecalculateEvent.class, this::onUserUpdate);

    }

    private void onUserUpdate(UserDataRecalculateEvent event) {
        UUID userId = event.getUser().getUniqueId();
        if (!accountManager.userIsLinkedById(userId.toString())) {
            return;
        }

        UserManager userManager = api.getUserManager();
        Optional.ofNullable(userManager.getUser(userId)).ifPresent(user -> {
            if (roleManager.isGroupTracked(user.getPrimaryGroup())) {
                Group primaryGroup = api.getGroupManager().getGroup(user.getPrimaryGroup());
                roleManager.setTrackedDiscordGroup(userId.toString(), primaryGroup.getDisplayName());
            } else {
                roleManager.removeTrackedGroups(userId.toString());
            }
        });
    }
}