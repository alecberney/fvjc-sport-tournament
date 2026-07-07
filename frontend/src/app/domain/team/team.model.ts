export interface Person {
  firstName: string;
  lastName: string;
}

export interface Team {
  id: string;
  name: string;
  paid: boolean;
  organisationId: string;
  responsible: Person;
}

export interface TeamRegistration {
  name: string;
  responsibleFirstName: string;
  responsibleLastName: string;
  count: number;
  paid: boolean[];
}

export interface TeamUpdate {
  name: string;
  responsibleFirstName: string;
  responsibleLastName: string;
  paid: boolean;
}

export interface TeamGroup {
  organisationId: string;
  responsibleName: string;
  teams: Team[];
}
