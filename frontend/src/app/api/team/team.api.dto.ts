export interface TeamDto {
  id: string;
  name: string;
  paid: boolean;
  organisationId: string;
  responsibleFirstName: string;
  responsibleLastName: string;
}

export interface TeamRegisterRequestDto {
  name: string;
  responsibleFirstName: string;
  responsibleLastName: string;
  count: number;
  paid: boolean[];
}

export interface TeamUpdateRequestDto {
  name: string;
  responsibleFirstName: string;
  responsibleLastName: string;
  paid: boolean;
}

export interface TeamPaidRequestDto {
  paid: boolean;
}
