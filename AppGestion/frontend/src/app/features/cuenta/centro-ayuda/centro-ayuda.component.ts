import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatExpansionModule } from '@angular/material/expansion';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { environment } from '../../../../environments/environment';
import { HELP_VIDEOS } from './centro-ayuda.data';

interface QuickStepUi {
  route: string;
  icon: string;
  titleKey: string;
  descKey: string;
}

interface GuideTileUi {
  route: string;
  icon: string;
  labelKey: string;
}

/** Catálogo FAQ: textos desde i18n; keywords para buscar (ES + inglés habitual). */
const FAQ_META: readonly { questionKey: string; answerKey: string; keywords: string[] }[] = [
  {
    questionKey: 'acctHelp.faq_email_pdf_q',
    answerKey: 'acctHelp.faq_email_pdf_a',
    keywords: ['email', 'correo', 'pdf', 'enviar', 'smtp', 'spam'],
  },
  {
    questionKey: 'acctHelp.faq_stripe_q',
    answerKey: 'acctHelp.faq_stripe_a',
    keywords: ['stripe', 'pago', 'tarjeta', 'suscripción', 'checkout'],
  },
  {
    questionKey: 'acctHelp.faq_pdf_empty_q',
    answerKey: 'acctHelp.faq_pdf_empty_a',
    keywords: ['pdf', 'vacío', 'blanco', 'descargar'],
  },
  {
    questionKey: 'acctHelp.faq_pw_q',
    answerKey: 'acctHelp.faq_pw_a',
    keywords: ['contraseña', 'password', 'login', 'acceso', 'google'],
  },
  {
    questionKey: 'acctHelp.faq_invoice_num_q',
    answerKey: 'acctHelp.faq_invoice_num_a',
    keywords: ['número', 'numeración', 'factura', 'duplicado'],
  },
  {
    questionKey: 'acctHelp.faq_iva_q',
    answerKey: 'acctHelp.faq_iva_a',
    keywords: ['iva', 'impuestos', 'presupuesto', 'factura'],
  },
];

@Component({
    selector: 'app-centro-ayuda',
    imports: [
        RouterLink,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatFormFieldModule,
        MatInputModule,
        MatExpansionModule,
        TranslateModule,
    ],
    templateUrl: './centro-ayuda.component.html',
    styleUrl: './centro-ayuda.component.scss'
})
export class CentroAyudaComponent {
  private readonly translate = inject(TranslateService);

  readonly externalHelpUrl = environment.helpCenterUrl?.trim() ?? '';

  readonly quickSteps: QuickStepUi[] = [
    {
      route: '/clientes/nuevo',
      icon: 'person_add',
      titleKey: 'acctHelp.step1t',
      descKey: 'acctHelp.step1d',
    },
    {
      route: '/presupuestos/nuevo',
      icon: 'request_quote',
      titleKey: 'acctHelp.step2t',
      descKey: 'acctHelp.step2d',
    },
    {
      route: '/facturas/nuevo',
      icon: 'receipt_long',
      titleKey: 'acctHelp.step3t',
      descKey: 'acctHelp.step3d',
    },
  ];

  readonly guideTiles: GuideTileUi[] = [
    { route: '/presupuestos', icon: 'request_quote', labelKey: 'acctHelp.tileBudgets' },
    { route: '/facturas', icon: 'receipt_long', labelKey: 'acctHelp.tileInvoices' },
    { route: '/clientes', icon: 'groups', labelKey: 'acctHelp.tileClients' },
    { route: '/materiales', icon: 'inventory_2', labelKey: 'acctHelp.tileMaterials' },
    { route: '/cuenta/suscripcion', icon: 'workspace_premium', labelKey: 'acctHelp.tileSubscription' },
    { route: '/cuenta/datos-empresa', icon: 'business', labelKey: 'acctHelp.tileCompany' },
  ];

  readonly videos = HELP_VIDEOS;

  readonly searchQuery = signal('');

  /** Texto resuelto para cada FAQ (filtro sobre el idioma actual). */
  readonly faqResolved = computed(() => {
    return FAQ_META.map((m) => ({
      questionKey: m.questionKey,
      answerKey: m.answerKey,
      keywords: m.keywords,
      qText: this.translate.instant(m.questionKey),
      aText: this.translate.instant(m.answerKey),
    }));
  });

  readonly filteredFaq = computed(() => {
    const qRaw = this.searchQuery().trim().toLowerCase();
    const resolved = this.faqResolved();
    if (!qRaw) {
      return resolved;
    }
    return resolved.filter(
      (item) =>
        item.qText.toLowerCase().includes(qRaw) ||
        item.aText.toLowerCase().includes(qRaw) ||
        item.keywords.some((k) => k.includes(qRaw)),
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
