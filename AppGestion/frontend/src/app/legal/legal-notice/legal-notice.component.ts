import { Component } from '@angular/core';
import { LegalFooterComponent } from '../legal-footer/legal-footer.component';

@Component({
  selector: 'app-legal-notice',
  standalone: true,
  imports: [LegalFooterComponent],
  templateUrl: './legal-notice.component.html',
})
export class LegalNoticeComponent {}
