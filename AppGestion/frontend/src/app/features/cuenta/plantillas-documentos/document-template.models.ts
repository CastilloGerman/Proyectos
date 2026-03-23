/**
 * Modelos de dominio (frontend) para el editor avanzado de plantillas PDF.
 * Referencia hasta que exista API REST + persistencia en PostgreSQL.
 */

export type DocumentTemplateKind = 'PRESUPUESTO' | 'FACTURA';

export type LogoPosition = 'LEFT' | 'RIGHT' | 'CENTER';

export type TemplateFontPreset = 'SYSTEM' | 'MODERN_SANS' | 'CLASSIC_SERIF';

/** Resumen para listado lateral / selector */
export interface DocumentTemplateSummary {
  id: string;
  nombre: string;
  tipo: DocumentTemplateKind;
  esPredeterminada: boolean;
  actualizadoEn: string; // ISO 8601
}

/** A. General */
export interface DocumentTemplateGeneral {
  nombre: string;
  tipo: DocumentTemplateKind;
}

/** B. Branding (parte puede seguir viniendo de Empresa; aquí overrides por plantilla) */
export interface DocumentTemplateBranding {
  logoAssetId: string | null;
  logoPosicion: LogoPosition;
  colorPrimario: string;
  colorAcento: string;
  /** Si true, usar datos de empresa global; si false, usar snapshot en `empresaOverride` */
  heredarEmpresa: boolean;
  empresaOverride: {
    nombre: string;
    nif: string;
    direccion: string;
    codigoPostal: string;
    ciudad: string;
    provincia: string;
    pais: string;
    telefono: string;
    email: string;
  } | null;
}

/** C. Layout */
export interface DocumentTemplateLayout {
  fuente: TemplateFontPreset;
  tamanoBasePt: number;
  densidad: 'COMPACT' | 'NORMAL' | 'RELAXED';
  margenMm: { superior: number; inferior: number; izquierdo: number; derecho: number };
  mostrarBordeCabecera: boolean;
  alturaMinimaHeaderMm: number;
  alturaMinimaFooterMm: number;
}

/** D. Contenido con placeholders {{variable}} */
export interface DocumentTemplateContent {
  introduccionHtml: string;
  notasPie: string;
  condicionesHtml: string;
}

/** Columna configurable en tabla de líneas */
export interface LineColumnConfig {
  id: string;
  clave: 'concepto' | 'cantidad' | 'unidad' | 'precioUnitario' | 'descuento' | 'iva' | 'importe';
  visible: boolean;
  orden: number;
  encabezado: string;
}

/** E. Tabla productos/servicios */
export interface DocumentTemplateLineTable {
  columnas: LineColumnConfig[];
  formatoMoneda: string; // ej. EUR
  simboloMoneda: string; // ej. €
  decimales: number;
  alinearImportes: 'LEFT' | 'RIGHT';
  mostrarSubtotalesPorIva: boolean;
}

/** F. Footer */
export interface DocumentTemplateFooter {
  textoPago: string;
  iban: string;
  notasLegales: string;
}

/** Plantilla completa (payload editor + preview) */
export interface DocumentTemplate extends DocumentTemplateSummary {
  general: DocumentTemplateGeneral;
  branding: DocumentTemplateBranding;
  layout: DocumentTemplateLayout;
  contenido: DocumentTemplateContent;
  tablaLineas: DocumentTemplateLineTable;
  pie: DocumentTemplateFooter;
}

/** Perfil de datos de ejemplo (HTML + mismo escenario en PDF del servidor). */
export type PlantillaPdfPreviewEscenario = 'DEFAULT' | 'MIXED_IVA' | 'LONG_LINES' | 'LONG_FOOTER';

export const PREVIEW_SCENARIO_LABELS: Record<PlantillaPdfPreviewEscenario, string> = {
  DEFAULT: 'Trabajo normal (varias líneas)',
  MIXED_IVA: 'Con partidas con IVA y sin IVA',
  LONG_LINES: 'Nombres de trabajo muy largos',
  LONG_FOOTER: 'Solo para probar mucho texto al final',
};

/** Texto de ejemplo (menos de 1000 caracteres) para probar saltos de línea en el pie del PDF. */
export const SAMPLE_LONG_FOOTER_TEXT =
  'Condiciones: pago a 30 días fecha factura. Cualquier reclamación sobre cantidades o calidad deberá ' +
  'formularse por escrito en un plazo máximo de 8 días hábiles desde la recepción del servicio. ' +
  'Los precios no incluyen desplazamientos fuera del área metropolitana salvo acuerdo expreso. ' +
  'En virtud de la normativa vigente (Ley 37/1992 del IVA), esta factura cumple los requisitos del art. 6 ' +
  'del Reglamento de facturación. El tratamiento de datos personales se ajusta al RGPD y la LOPDGDD. ' +
  'Para ejercer sus derechos contacte en el correo indicado en el membrete. IBAN disponible en la zona ' +
  'de cobro de la aplicación. Gracias por su confianza.';

/** Misma descripción larga que usa el backend en LONG_LINES (vista HTML alineada). */
export const PREVIEW_LONG_CONCEPTO =
  'Instalación integral de sistema de climatización por conductos con unidad exterior inverter, ' +
  'distribución en falsos techos registrables, rejillas lineales y puesta en marcha con certificado ' +
  'de eficiencia. Incluye desplazamientos en radio 40 km, PPE y retirada de residuos no peligrosos ' +
  'según normativa local. Validez del presupuesto 30 días.';

/** Datos mock para vista previa (sustituir placeholders) */
export interface DocumentPreviewMock {
  numeroDocumento: string;
  fechaEmision: string;
  clienteNombre: string;
  clienteNif: string;
  clienteDireccion: string;
  lineas: Array<{
    concepto: string;
    cantidad: number;
    unidad: string;
    precioUnitario: number;
    ivaPct: number;
    importe: number;
  }>;
  subtotal: number;
  iva: number;
  total: number;
  /** Si true, la tabla HTML muestra columna IVA por línea (orientativa). */
  mostrarIvaEnLineas?: boolean;
}

export const PREVIEW_MOCK_DEFAULT: DocumentPreviewMock = {
  numeroDocumento: 'FAC-2025-0142',
  fechaEmision: '2025-03-22',
  clienteNombre: 'María Gómez · Reformas',
  clienteNif: 'B12345678',
  clienteDireccion: 'Calle Mayor 12, 28001 Madrid',
  lineas: [
    {
      concepto: 'Reforma de baño (alicatado, fontanería y electricidad)',
      cantidad: 1,
      unidad: 'obra',
      precioUnitario: 650,
      ivaPct: 21,
      importe: 650,
    },
    {
      concepto: 'Mantenimiento de jardín y poda',
      cantidad: 1,
      unidad: 'mes',
      precioUnitario: 120,
      ivaPct: 21,
      importe: 120,
    },
  ],
  subtotal: 770,
  iva: 161.7,
  total: 931.7,
};

/** Coherente con escenario MIXED_IVA en API (bases 100+50+30, IVA 21% solo líneas sujetas). */
export const PREVIEW_MOCK_MIXED_IVA: DocumentPreviewMock = {
  numeroDocumento: 'FAC-PREV-2025-0001',
  fechaEmision: '2025-03-22',
  clienteNombre: 'María Gómez · Reformas',
  clienteNif: 'B12345678',
  clienteDireccion: 'Calle Mayor 12, 28001 Madrid',
  mostrarIvaEnLineas: true,
  lineas: [
    {
      concepto: 'Montaje y mano de obra',
      cantidad: 1,
      unidad: 'ud',
      precioUnitario: 100,
      ivaPct: 21,
      importe: 100,
    },
    {
      concepto: 'Transporte de materiales (sin IVA)',
      cantidad: 1,
      unidad: 'ud',
      precioUnitario: 50,
      ivaPct: 0,
      importe: 50,
    },
    {
      concepto: 'Tornillería y consumibles',
      cantidad: 2,
      unidad: 'ud',
      precioUnitario: 15,
      ivaPct: 21,
      importe: 30,
    },
  ],
  subtotal: 180,
  iva: 27.3,
  total: 207.3,
};

export const PREVIEW_MOCK_LONG_LINES: DocumentPreviewMock = {
  numeroDocumento: 'FAC-2025-0142',
  fechaEmision: '2025-03-22',
  clienteNombre: 'María Gómez · Reformas',
  clienteNif: 'B12345678',
  clienteDireccion: 'Calle Mayor 12, 28001 Madrid',
  lineas: [
    {
      concepto: PREVIEW_LONG_CONCEPTO,
      cantidad: 10,
      unidad: 'h',
      precioUnitario: 65,
      ivaPct: 21,
      importe: 650,
    },
    {
      concepto:
        'Mantenimiento mensual — revisión de filtros, comprobación de presiones y registro fotográfico en panel de control.',
      cantidad: 1,
      unidad: 'mes',
      precioUnitario: 120,
      ivaPct: 21,
      importe: 120,
    },
  ],
  subtotal: 770,
  iva: 161.7,
  total: 931.7,
};

export const PREVIEW_MOCK_BY_SCENARIO: Record<PlantillaPdfPreviewEscenario, DocumentPreviewMock> = {
  DEFAULT: PREVIEW_MOCK_DEFAULT,
  MIXED_IVA: PREVIEW_MOCK_MIXED_IVA,
  LONG_LINES: PREVIEW_MOCK_LONG_LINES,
  LONG_FOOTER: PREVIEW_MOCK_DEFAULT,
};

/** Placeholders soportados (documentación + autocompletado futuro) */
export interface PlaceholderDef {
  token: string;
  descripcion: string;
  ejemplo: string;
}

export const PLACEHOLDER_CATALOG: PlaceholderDef[] = [
  { token: '{{client_name}}', descripcion: 'Nombre del cliente', ejemplo: 'María Gómez · Reformas' },
  { token: '{{client_tax_id}}', descripcion: 'NIF o CIF del cliente', ejemplo: 'B12345678' },
  { token: '{{client_address}}', descripcion: 'Dirección del cliente', ejemplo: 'Calle Mayor 12' },
  { token: '{{doc_number}}', descripcion: 'Número del presupuesto o factura', ejemplo: 'FAC-2025-0142' },
  { token: '{{doc_date}}', descripcion: 'Fecha del documento', ejemplo: '22/03/2025' },
  { token: '{{subtotal}}', descripcion: 'Suma antes de impuestos', ejemplo: '770,00 €' },
  { token: '{{tax_total}}', descripcion: 'Impuestos (IVA)', ejemplo: '161,70 €' },
  { token: '{{total}}', descripcion: 'Total a pagar', ejemplo: '931,70 €' },
];
