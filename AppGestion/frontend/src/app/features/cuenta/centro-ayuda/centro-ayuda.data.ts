/** Contenido estático del centro de ayuda (sin API). */

export interface HelpFaqItem {
  id: string;
  question: string;
  answer: string;
  keywords: string[];
}

export const HELP_FAQ: HelpFaqItem[] = [
  {
    id: 'email-pdf',
    question: 'No me llega el email con el PDF de la factura o presupuesto',
    answer:
      'Comprueba la carpeta de spam o promociones. Revisa que el correo del cliente sea correcto. Si usas correo propio (SMTP) en datos de empresa, verifica usuario, contraseña de aplicación (Gmail) y puerto. Vuelve a enviar desde el detalle del documento.',
    keywords: ['email', 'correo', 'pdf', 'enviar', 'smtp', 'spam'],
  },
  {
    id: 'stripe-pago',
    question: 'Error al pagar la suscripción o con Stripe',
    answer:
      'Asegúrate de usar una tarjeta válida y con fondos. Prueba otro navegador o modo incógnito. Si el error continúa, anota el mensaje que muestra la pasarela y contacta con soporte indicando la hora aproximada del intento.',
    keywords: ['stripe', 'pago', 'tarjeta', 'suscripción', 'checkout'],
  },
  {
    id: 'pdf-vacio',
    question: 'El PDF se descarga vacío o sin datos',
    answer:
      'Comprueba que el documento tenga líneas o importes. Recarga la página y vuelve a generar el PDF. Si persiste, prueba otro navegador y contacta con soporte adjuntando captura.',
    keywords: ['pdf', 'vacío', 'blanco', 'descargar'],
  },
  {
    id: 'password',
    question: 'Olvidé mi contraseña',
    answer:
      'En la pantalla de inicio de sesión usa «¿Olvidaste tu contraseña?» y sigue el enlace que recibirás por email. Revisa spam. Si iniciaste sesión con Google, entra con Google o configura contraseña desde recuperación con tu email.',
    keywords: ['contraseña', 'password', 'login', 'acceso', 'google'],
  },
  {
    id: 'numeracion',
    question: 'Numeración de facturas y duplicados',
    answer:
      'La numeración suele ser única por cuenta. No reutilices números ya emitidos si tu gestor lo impide. Si necesitas rectificar, consulta con tu asesor la mejor forma (factura rectificativa según normativa).',
    keywords: ['número', 'numeración', 'factura', 'duplicado'],
  },
  {
    id: 'iva',
    question: 'IVA en presupuestos y facturas',
    answer:
      'Puedes activar o desactivar IVA en cada documento según corresponda. Los importes y el PDF reflejan la opción elegida. En caso de duda fiscal, consulta con tu gestor o asesor.',
    keywords: ['iva', 'impuestos', 'presupuesto', 'factura'],
  },
];

export interface HelpQuickStep {
  title: string;
  description: string;
  route: string;
  icon: string;
}

export const HELP_QUICK_STEPS: HelpQuickStep[] = [
  {
    title: '1. Crear un cliente',
    description: 'Datos básicos para facturar con claridad.',
    route: '/clientes/nuevo',
    icon: 'person_add',
  },
  {
    title: '2. Crear un presupuesto',
    description: 'Líneas, importes y envío al cliente.',
    route: '/presupuestos/nuevo',
    icon: 'request_quote',
  },
  {
    title: '3. Factura y cobro',
    description: 'Convierte o crea factura y gestiona el cobro.',
    route: '/facturas/nuevo',
    icon: 'receipt_long',
  },
];

export interface HelpGuideTile {
  label: string;
  route: string;
  icon: string;
}

export const HELP_GUIDE_TILES: HelpGuideTile[] = [
  { label: 'Presupuestos', route: '/presupuestos', icon: 'request_quote' },
  { label: 'Facturas', route: '/facturas', icon: 'receipt_long' },
  { label: 'Clientes', route: '/clientes', icon: 'groups' },
  { label: 'Materiales', route: '/materiales', icon: 'inventory_2' },
  { label: 'Suscripción', route: '/cuenta/suscripcion', icon: 'workspace_premium' },
  { label: 'Datos de empresa', route: '/cuenta/datos-empresa', icon: 'business' },
];

export interface HelpVideoLink {
  title: string;
  description: string;
  url: string;
}

/** Enlaces externos (YouTube, etc.); dejar vacío o rellenar cuando tengáis vídeos. */
export const HELP_VIDEOS: HelpVideoLink[] = [
  // { title: 'Crear un presupuesto', description: 'Menos de 1 minuto', url: 'https://...' },
];
