package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.GroupId;
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
