package abe.fvjc.tournament.group.domain;

import java.util.UUID;

public record GroupId(UUID value) {

    public static GroupId of(final UUID value) {
        return new GroupId(value);
    }

    public static GroupId empty() {
        return new GroupId(null);
    }

    public boolean isEmpty() {
        return value == null;
    }
}
