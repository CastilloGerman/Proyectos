import { Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatExpansionModule } from '@angular/material/expansion';
import { environment } from '../../../../environments/environment';
import { HELP_FAQ, HELP_GUIDE_TILES, HELP_QUICK_STEPS, HELP_VIDEOS } from './centro-ayuda.data';

@Component({
  selector: 'app-centro-ayuda',
  standalone: true,
  imports: [
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatExpansionModule,
  ],
  templateUrl: './centro-ayuda.component.html',
  styleUrl: './centro-ayuda.component.scss',
})
export class CentroAyudaComponent {
  readonly externalHelpUrl = environment.helpCenterUrl?.trim() ?? '';

  readonly quickSteps = HELP_QUICK_STEPS;
  readonly guideTiles = HELP_GUIDE_TILES;
  readonly videos = HELP_VIDEOS;

  readonly searchQuery = signal('');

  readonly filteredFaq = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    if (!q) {
      return HELP_FAQ;
    }
    return HELP_FAQ.filter(
      (item) =>
        item.question.toLowerCase().includes(q) ||
        item.answer.toLowerCase().includes(q) ||
        item.keywords.some((k) => k.includes(q))
    );
  });

  onSearchInput(event: Event): void {
    const v = (event.target as HTMLInputElement).value;
    this.searchQuery.set(v.length > 80 ? v.slice(0, 80) : v);
  }

  clearSearch(): void {
    this.searchQuery.set('');
  }
}
