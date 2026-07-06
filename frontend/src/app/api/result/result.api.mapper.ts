import { GroupRanking, GroupRankingEntry } from '@app/domain/result/result.model';
import { GroupRankingDto, GroupRankingEntryDto } from '@app/api/result/result.api.dto';

export class ResultApiMapper {

  static toGroupRankingDomain(dto: GroupRankingDto): GroupRanking {
    return {
      groupId: dto.groupId,
      groupName: dto.groupName,
      entries: dto.entries.map(ResultApiMapper.toGroupRankingEntryDomain),
    };
  }

  private static toGroupRankingEntryDomain(dto: GroupRankingEntryDto): GroupRankingEntry {
    return { ...dto };
  }
}
