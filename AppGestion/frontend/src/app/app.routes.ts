import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent) },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'presupuestos', loadComponent: () => import('./features/presupuestos/presupuesto-list/presupuesto-list.component').then(m => m.PresupuestoListComponent) },
      { path: 'presupuestos/nuevo', loadComponent: () => import('./features/presupuestos/presupuesto-form/presupuesto-form.component').then(m => m.PresupuestoFormComponent) },
      { path: 'presupuestos/:id', loadComponent: () => import('./features/presupuestos/presupuesto-form/presupuesto-form.component').then(m => m.PresupuestoFormComponent) },
      { path: 'materiales', loadComponent: () => import('./features/materiales/material-list/material-list.component').then(m => m.MaterialListComponent) },
      { path: 'materiales/nuevo', loadComponent: () => import('./features/materiales/material-form/material-form.component').then(m => m.MaterialFormComponent) },
      { path: 'materiales/:id', loadComponent: () => import('./features/materiales/material-form/material-form.component').then(m => m.MaterialFormComponent) },
      { path: 'clientes', loadComponent: () => import('./features/clientes/cliente-list/cliente-list.component').then(m => m.ClienteListComponent) },
      { path: 'clientes/nuevo', loadComponent: () => import('./features/clientes/cliente-form/cliente-form.component').then(m => m.ClienteFormComponent) },
      { path: 'clientes/:id', loadComponent: () => import('./features/clientes/cliente-form/cliente-form.component').then(m => m.ClienteFormComponent) },
      { path: 'facturas', loadComponent: () => import('./features/facturas/factura-list/factura-list.component').then(m => m.FacturaListComponent) },
      { path: 'facturas/nuevo', loadComponent: () => import('./features/facturas/factura-form/factura-form.component').then(m => m.FacturaFormComponent) },
      { path: 'facturas/:id', loadComponent: () => import('./features/facturas/factura-form/factura-form.component').then(m => m.FacturaFormComponent) },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
