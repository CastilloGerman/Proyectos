import { Component } from '@angular/core';
import { LegalFooterComponent } from '../legal-footer/legal-footer.component';

@Component({
  selector: 'app-privacy',
  standalone: true,
  imports: [LegalFooterComponent],
  templateUrl: './privacy.component.html',
})
export class PrivacyComponent {}
