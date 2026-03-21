import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleMutateGuard } from './core/auth/role-mutate.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'invite/:token', loadComponent: () => import('./features/auth/invite-accept/invite-accept.component').then(m => m.InviteAcceptComponent) },
  { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent) },
  { path: 'forgot-password', loadComponent: () => import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent) },
  { path: 'reset-password', loadComponent: () => import('./features/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent) },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: 'subscription/success', loadComponent: () => import('./features/subscription/subscription-success/subscription-success.component').then(m => m.SubscriptionSuccessComponent) },
      { path: 'subscription/cancel', loadComponent: () => import('./features/subscription/subscription-cancel/subscription-cancel.component').then(m => m.SubscriptionCancelComponent) },
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'presupuestos', loadComponent: () => import('./features/presupuestos/presupuesto-list/presupuesto-list.component').then(m => m.PresupuestoListComponent) },
      { path: 'presupuestos/rapido', canActivate: [roleMutateGuard], loadComponent: () => import('./features/presupuestos/presupuesto-rapido/presupuesto-rapido.component').then(m => m.PresupuestoRapidoComponent) },
      { path: 'presupuestos/nuevo', canActivate: [roleMutateGuard], loadComponent: () => import('./features/presupuestos/presupuesto-form/presupuesto-form.component').then(m => m.PresupuestoFormComponent) },
      { path: 'presupuestos/:id', canActivate: [roleMutateGuard], loadComponent: () => import('./features/presupuestos/presupuesto-form/presupuesto-form.component').then(m => m.PresupuestoFormComponent) },
      { path: 'materiales', loadComponent: () => import('./features/materiales/material-list/material-list.component').then(m => m.MaterialListComponent) },
      { path: 'materiales/nuevo', canActivate: [roleMutateGuard], loadComponent: () => import('./features/materiales/material-form/material-form.component').then(m => m.MaterialFormComponent) },
      { path: 'materiales/:id', canActivate: [roleMutateGuard], loadComponent: () => import('./features/materiales/material-form/material-form.component').then(m => m.MaterialFormComponent) },
      { path: 'clientes', loadComponent: () => import('./features/clientes/cliente-list/cliente-list.component').then(m => m.ClienteListComponent) },
      { path: 'clientes/nuevo', canActivate: [roleMutateGuard], loadComponent: () => import('./features/clientes/cliente-form/cliente-form.component').then(m => m.ClienteFormComponent) },
      { path: 'clientes/:id', canActivate: [roleMutateGuard], loadComponent: () => import('./features/clientes/cliente-form/cliente-form.component').then(m => m.ClienteFormComponent) },
      { path: 'facturas/nuevo', canActivate: [roleMutateGuard], loadComponent: () => import('./features/facturas/factura-form/factura-form.component').then(m => m.FacturaFormComponent) },
      { path: 'facturas/:id', canActivate: [roleMutateGuard], loadComponent: () => import('./features/facturas/factura-form/factura-form.component').then(m => m.FacturaFormComponent) },
      { path: 'facturas', loadComponent: () => import('./features/facturas/factura-list/factura-list.component').then(m => m.FacturaListComponent) },
      {
        path: 'cuenta/perfil',
        loadComponent: () => import('./features/cuenta/perfil/perfil.component').then((m) => m.PerfilComponent),
      },
      {
        path: 'cuenta/configuracion-cuenta',
        loadComponent: () =>
          import('./features/cuenta/config-cuenta/config-cuenta.component').then((m) => m.ConfigCuentaComponent),
      },
      {
        path: 'cuenta/preferencias',
        loadComponent: () =>
          import('./features/cuenta/preferencias/preferencias.component').then((m) => m.PreferenciasComponent),
      },
      {
        path: 'cuenta/notificaciones',
        loadComponent: () =>
          import('./features/cuenta/notificaciones/notificaciones.component').then((m) => m.NotificacionesComponent),
      },
      {
        path: 'cuenta/datos-empresa',
        loadComponent: () =>
          import('./features/cuenta/datos-empresa/datos-empresa.component').then((m) => m.DatosEmpresaComponent),
      },
      {
        path: 'cuenta/suscripcion',
        loadComponent: () =>
          import('./features/cuenta/suscripcion-plan/suscripcion-plan.component').then((m) => m.SuscripcionPlanComponent),
      },
      {
        path: 'cuenta/metodos-pago',
        loadComponent: () =>
          import('./features/cuenta/metodos-pago/metodos-pago.component').then((m) => m.MetodosPagoComponent),
      },
      {
        path: 'cuenta/historial-suscripcion',
        loadComponent: () =>
          import('./features/cuenta/historial-suscripcion/historial-suscripcion.component').then(
            (m) => m.HistorialSuscripcionComponent
          ),
      },
      {
        path: 'cuenta/datos-fiscales',
        loadComponent: () =>
          import('./features/cuenta/datos-fiscales/datos-fiscales.component').then((m) => m.DatosFiscalesComponent),
      },
      {
        path: 'cuenta/cambiar-contrasena',
        loadComponent: () =>
          import('./features/cuenta/cambiar-contrasena/cambiar-contrasena.component').then((m) => m.CambiarContrasenaComponent),
      },
      {
        path: 'cuenta/2fa',
        loadComponent: () =>
          import('./features/cuenta/totp-2fa/totp-2fa.component').then((m) => m.Totp2FaComponent),
      },
      {
        path: 'cuenta/sesiones-activas',
        loadComponent: () =>
          import('./features/cuenta/sesiones-activas/sesiones-activas.component').then(
            (m) => m.SesionesActivasComponent
          ),
      },
      {
        path: 'cuenta/historial-accesos',
        loadComponent: () =>
          import('./features/cuenta/historial-accesos/historial-accesos.component').then(
            (m) => m.HistorialAccesosComponent
          ),
      },
      {
        path: 'cuenta/plantillas',
        loadComponent: () =>
          import('./features/cuenta/plantillas-documentos/plantillas-documentos.component').then(
            (m) => m.PlantillasDocumentosComponent
          ),
      },
      {
        path: 'cuenta/centro-ayuda',
        loadComponent: () =>
          import('./features/cuenta/centro-ayuda/centro-ayuda.component').then((m) => m.CentroAyudaComponent),
      },
      {
        path: 'cuenta/contactar-soporte',
        loadComponent: () =>
          import('./features/cuenta/contactar-soporte/contactar-soporte.component').then(
            (m) => m.ContactarSoporteComponent
          ),
      },
      {
        path: 'cuenta/:slug',
        loadComponent: () => import('./features/cuenta/cuenta-seccion.component').then((m) => m.CuentaSeccionComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
