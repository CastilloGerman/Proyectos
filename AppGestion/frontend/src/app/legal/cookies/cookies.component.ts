import { Component } from '@angular/core';
import { LegalFooterComponent } from '../legal-footer/legal-footer.component';

@Component({
  selector: 'app-cookies',
  standalone: true,
  imports: [LegalFooterComponent],
  templateUrl: './cookies.component.html',
})
export class CookiesComponent {}
