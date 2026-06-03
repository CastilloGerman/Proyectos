import { Component } from '@angular/core';
import { LegalFooterComponent } from '../legal-footer/legal-footer.component';

@Component({
  selector: 'app-terms',
  standalone: true,
  imports: [LegalFooterComponent],
  templateUrl: './terms.component.html',
})
export class TermsComponent {}
