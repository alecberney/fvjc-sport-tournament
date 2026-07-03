export interface GroupTeamDto {
  id: string;
  name: string;
  organisationId: string;
}

export interface GroupDto {
  id: string;
  name: string;
  teams: GroupTeamDto[];
}

export interface GroupDistributionDto {
  numberOfGroups: number;
  groupsOfBaseSize: number;
  groupsOfBaseSizePlusOne: number;
  baseSize: number;
  totalTeams: number;
}

export interface GroupGenerateRequestDto {
  groupSize: number;
}

export interface GroupSwapRequestDto {
  teamId1: string;
  teamId2: string;
}
