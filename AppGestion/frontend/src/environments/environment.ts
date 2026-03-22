function devAppPublicUrl(): string {
  if (typeof window === 'undefined' || !window.location?.origin) {
    return 'http://localhost:4200';
  }
  return window.location.origin;
}

export const environment = {
  production: false,
  /**
   * Base de la API (sin barra final). Con `ng serve` y `proxy.conf.js`, `/api` se reenvía al backend
   * aunque entres desde el móvil por la IP de la LAN (mismo origen que :4200).
   */
  apiUrl: '/api',
  /** URL pública del front (home/login). En dev se adapta al origen real (p. ej. IP de la LAN en el móvil). */
  get appPublicUrl(): string {
    return devAppPublicUrl();
  },
  /** Google OAuth 2.0 Client ID (tipo "Aplicación web") para "Iniciar con Google". Opcional. */
  googleClientId: '622654316729-itkgprp568mrobd3v8lgnah0cfjchog9.apps.googleusercontent.com' as string,
  /** Centro de ayuda (docs/FAQ). Si está vacío, el menú de usuario abre la vista interna de ayuda. */
  helpCenterUrl: '' as string,
  /**
   * Déjalo vacío: “Contactar soporte” abre el formulario interno y el API envía al buzón configurado en el servidor
   * (`app.support.inbox-email`). Si pones un email aquí, el menú usará `mailto:` y el correo será visible en el navegador.
   */
  supportEmail: '' as string,
  /** Nombre mostrado en la pantalla de suscripción (un solo plan / precio Stripe). */
  subscriptionPlanDisplayName: 'Plan profesional' as string,
};
