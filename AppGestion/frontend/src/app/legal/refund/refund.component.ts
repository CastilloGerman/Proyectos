import { Component } from '@angular/core';
import { LegalFooterComponent } from '../legal-footer/legal-footer.component';

@Component({
  selector: 'app-refund',
  standalone: true,
  imports: [LegalFooterComponent],
  templateUrl: './refund.component.html',
})
export class RefundComponent {}
