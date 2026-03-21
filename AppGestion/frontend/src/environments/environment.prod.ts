export const environment = {
  production: true,
  apiUrl: '/api',
  /** Ej.: 'https://tu-dominio.com'. Vacío = se usa el origen actual (recomendado si sirves el SPA en ese dominio). */
  appPublicUrl: '' as string,
  /** Mismo Client ID que en development; añade el origen de producción en Google Cloud Console */
  googleClientId: '622654316729-itkgprp568mrobd3v8lgnah0cfjchog9.apps.googleusercontent.com',
  helpCenterUrl: '' as string,
  /** Vacío = formulario interno + destino solo en el API. No rellenar si no quieres mostrar el mail en mailto:. */
  supportEmail: '' as string,
  subscriptionPlanDisplayName: 'Plan profesional' as string,
};
