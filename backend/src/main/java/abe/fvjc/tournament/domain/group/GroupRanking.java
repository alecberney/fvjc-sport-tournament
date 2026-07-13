package abe.fvjc.tournament.domain.group;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class GroupRanking {
    GroupId groupId;
    String groupName;
    List<GroupRankingEntry> entries;
}
