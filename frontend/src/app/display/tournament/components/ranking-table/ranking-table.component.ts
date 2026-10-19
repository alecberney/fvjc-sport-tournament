import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { GroupRanking } from '@app/domain/result/result.model';

@Component({
  selector: 'app-ranking-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './ranking-table.component.html',
  styleUrl: './ranking-table.component.scss',
  standalone: true,
})
export class RankingTableComponent {
  @Input({ required: true }) ranking!: GroupRanking;
}
