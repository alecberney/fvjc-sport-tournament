package abe.fvjc.tournament.domain.group;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GroupRef {
    GroupId id;
    String name;

    public static GroupRef toGroupRef(final GroupId id, final String name) {
        return GroupRef.builder()
                .id(id)
                .name(name)
                .build();
    }
}
