/**
 * Configuración del menú de usuario (dropdown superior).
 * Para añadir secciones o ítems: extiende USER_MENU_SECTIONS.
 * Las rutas bajo `/cuenta/:slug` se resuelven en {@link CuentaSeccionComponent}.
 */
export type UserMenuItemKind = 'route' | 'external' | 'action';

export type UserMenuBuiltInAction =
  | 'subscription'
  | 'help-center'
  | 'contact-support';

export interface UserMenuItem {
  /** Identificador estable para tests / analytics */
  id: string;
  label: string;
  icon: string;
  kind: UserMenuItemKind;
  /** Ruta interna (sin dominio), ej. `/facturas` o `/cuenta/perfil` */
  route?: string;
  /** URL absoluta https... */
  href?: string;
  /** Acciones manejadas en el componente (Stripe, mailto, ayuda). */
  action?: UserMenuBuiltInAction;
  /** Deshabilitado visualmente (funcionalidad futura). */
  disabled?: boolean;
}

export interface UserMenuSection {
  id: string;
  label: string;
  items: UserMenuItem[];
}

export const USER_MENU_SECTIONS: UserMenuSection[] = [
  {
    id: 'cuenta',
    label: 'Cuenta',
    items: [
      { id: 'perfil', label: 'Perfil', icon: 'person', kind: 'route', route: '/cuenta/perfil' },
      {
        id: 'config-cuenta',
        label: 'Configuración de cuenta',
        icon: 'manage_accounts',
        kind: 'route',
        route: '/cuenta/configuracion-cuenta',
      },
      {
        id: 'preferencias',
        label: 'Preferencias (idioma, zona horaria, moneda)',
        icon: 'tune',
        kind: 'route',
        route: '/cuenta/preferencias',
      },
      {
        id: 'notificaciones',
        label: 'Notificaciones',
        icon: 'notifications_outlined',
        kind: 'route',
        route: '/cuenta/notificaciones',
      },
    ],
  },
  {
    id: 'empresa',
    label: 'Empresa',
    items: [
      {
        id: 'datos-empresa',
        label: 'Datos de la empresa',
        icon: 'business',
        kind: 'route',
        route: '/cuenta/datos-empresa',
      },
    ],
  },
  {
    id: 'facturacion',
    label: 'Facturación',
    items: [
      {
        id: 'suscripcion',
        label: 'Suscripción / plan actual',
        icon: 'workspace_premium',
        kind: 'route',
        route: '/cuenta/suscripcion',
      },
      {
        id: 'metodos-pago',
        label: 'Métodos de pago',
        icon: 'credit_card',
        kind: 'route',
        route: '/cuenta/metodos-pago',
      },
      {
        id: 'historial-facturas',
        label: 'Historial de facturas (suscripción)',
        icon: 'receipt_long',
        kind: 'route',
        route: '/cuenta/historial-suscripcion',
      },
      {
        id: 'datos-fiscales',
        label: 'Datos fiscales',
        icon: 'gavel',
        kind: 'route',
        route: '/cuenta/datos-fiscales',
      },
    ],
  },
  {
    id: 'seguridad',
    label: 'Seguridad',
    items: [
      {
        id: 'cambiar-password',
        label: 'Cambiar contraseña',
        icon: 'lock_reset',
        kind: 'route',
        route: '/cuenta/cambiar-contrasena',
      },
      { id: '2fa', label: 'Autenticación en dos factores (2FA)', icon: 'phonelink_lock', kind: 'route', route: '/cuenta/2fa' },
      {
        id: 'sesiones',
        label: 'Sesiones activas',
        icon: 'devices',
        kind: 'route',
        route: '/cuenta/sesiones-activas',
      },
      {
        id: 'historial-accesos',
        label: 'Historial de accesos',
        icon: 'history',
        kind: 'route',
        route: '/cuenta/historial-accesos',
      },
    ],
  },
  {
    id: 'personalizacion',
    label: 'Personalización',
    items: [
      {
        id: 'plantillas',
        label: 'Plantillas de presupuestos/facturas',
        icon: 'description',
        kind: 'route',
        route: '/cuenta/plantillas',
      },
      {
        id: 'impuestos',
        label: 'Configuración de impuestos',
        icon: 'percent',
        kind: 'route',
        route: '/cuenta/impuestos',
      },
    ],
  },
  {
    id: 'soporte',
    label: 'Soporte',
    items: [
      {
        id: 'ayuda',
        label: 'Centro de ayuda',
        icon: 'help_outline',
        kind: 'action',
        action: 'help-center',
      },
      {
        id: 'contacto',
        label: 'Contactar soporte',
        icon: 'support_agent',
        kind: 'action',
        action: 'contact-support',
      },
    ],
  },
];
