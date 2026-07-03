import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Group } from '@app/domain/group/group.model';
import { Team } from '@app/domain/team/team.model';

export interface SwapRequest {
  team: Team;
  currentGroupName: string;
}

@Component({
  selector: 'app-group-list',
  templateUrl: './group-list.component.html',
  styleUrl: './group-list.component.scss',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
})
export class GroupListComponent {
  @Input({ required: true }) groups!: Group[];
  @Input({ required: true }) isDraft!: boolean;
  @Output() swapRequested = new EventEmitter<SwapRequest>();

  requestSwap(team: Team, groupName: string): void {
    this.swapRequested.emit({ team, currentGroupName: groupName });
  }
}
