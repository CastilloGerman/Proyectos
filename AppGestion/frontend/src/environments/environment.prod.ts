export const environment = {
  production: true,
  /**
   * - URL absoluta del API si el SPA y el backend están en subdominios distintos (p. ej. Railway + Cloudflare).
   * - `/api` si un proxy del mismo origen reenvía `/api/*` al backend.
   */
  apiUrl: 'https://api.noemiweb.com',
  /** Base pública del SPA (enlaces en invitaciones, etc.). Vacío = `window.location.origin` en runtime. */
  appPublicUrl: 'https://app.noemiweb.com' as string,
  /** Mismo Client ID que en development; añade el origen de producción en Google Cloud Console */
  googleClientId: '622654316729-itkgprp568mrobd3v8lgnah0cfjchog9.apps.googleusercontent.com',
  helpCenterUrl: '' as string,
  /** Vacío = formulario interno + destino solo en el API. No rellenar si no quieres mostrar el mail en mailto:. */
  supportEmail: '' as string,
  subscriptionPlanDisplayName: 'Plan profesional' as string,
};
