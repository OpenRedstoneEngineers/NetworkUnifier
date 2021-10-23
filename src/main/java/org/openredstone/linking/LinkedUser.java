package org.openredstone.linking;

import java.util.Objects;
import java.util.UUID;

public class LinkedUser {
    public final UUID uuid;
    public final String discordId;
    public final String name;

    public LinkedUser(UUID uuid, String discordId, String name) {
        this.uuid = uuid;
        this.discordId = discordId;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinkedUser that = (LinkedUser) o;
        return uuid.equals(that.uuid) && discordId.equals(that.discordId) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, discordId, name);
    }
}
