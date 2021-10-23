package org.openredstone.linking;

import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class OnJoinHandler implements Listener {
    private final Linking linking;

    public OnJoinHandler(Linking linking) {
        this.linking = linking;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        linking.postLogin(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }
}
