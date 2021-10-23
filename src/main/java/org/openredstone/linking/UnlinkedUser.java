package org.openredstone.linking;

import java.util.Objects;
import java.util.UUID;

public class UnlinkedUser {
    public final UUID uuid;
    public final String name;

    public UnlinkedUser(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public LinkedUser withLink(String discordId) {
        return new LinkedUser(uuid, discordId, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnlinkedUser that = (UnlinkedUser) o;
        return uuid.equals(that.uuid) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, name);
    }
}
