export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081',
  /** URL pública del front (home/login). Si vacío, el diálogo de referido usa `window.location.origin`. */
  appPublicUrl: 'http://localhost:4200',
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
