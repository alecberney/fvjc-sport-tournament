import { Component, Input } from '@angular/core';
import { GroupRanking } from '@app/domain/result/result.model';

@Component({
  selector: 'app-ranking-table',
  templateUrl: './ranking-table.component.html',
  styleUrl: './ranking-table.component.scss',
  standalone: true,
})
export class RankingTableComponent {
  @Input({ required: true }) ranking!: GroupRanking;
}
