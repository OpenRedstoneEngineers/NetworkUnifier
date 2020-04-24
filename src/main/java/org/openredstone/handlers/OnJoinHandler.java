package org.openredstone.handlers;

import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.openredstone.managers.AccountManager;
import org.openredstone.managers.NicknameManager;

public class OnJoinHandler implements Listener {

    private AccountManager accountManager;
    private NicknameManager nicknameManager;

    public OnJoinHandler(AccountManager accountManager, NicknameManager nicknameManager) {
        this.accountManager = accountManager;
        this.nicknameManager = nicknameManager;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        String userId = event.getPlayer().getUniqueId().toString();
        if (accountManager.userIsLinkedById(userId)) {
            if (!accountManager.getSavedIgnFromUserId(userId).equals(event.getPlayer().getName())) {
                accountManager.updateAccountIgn(userId, event.getPlayer().getName());
                nicknameManager.setNickname(userId, event.getPlayer().getName());
            }
        }
    }
}
