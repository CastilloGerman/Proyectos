import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/auth/auth.service';
import { GastoService } from '../../../core/services/gasto.service';
import { Gasto } from '../../../core/models/gasto.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { HintBannerComponent } from '../../../shared/hint-banner/hint-banner.component';

@Component({
  selector: 'app-gasto-list',
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatTooltipModule,
    TranslateModule,
    HintBannerComponent,
  ],
  template: `
    <div class="gasto-list">
      <app-hint-banner
        storageKey="hint_gastos_v1"
        [title]="'gastos.hintTitle' | translate"
        [steps]="[
          { icon: 'receipt_long', text: ('gastos.hintStep1' | translate) },
          { icon: 'account_balance', text: ('gastos.hintStep2' | translate) },
          { icon: 'edit', text: ('gastos.hintStep3' | translate) }
        ]"
      />
      <div class="header">
        <h1>{{ 'gastos.title' | translate }}</h1>
        @if (auth.canMutate()) {
          <a mat-raised-button color="primary" routerLink="/gastos/nuevo">
            <mat-icon>add</mat-icon>
            {{ 'gastos.new' | translate }}
          </a>
        }
      </div>
      <div class="table-container">
        <table mat-table [dataSource]="dataSource" class="full-width">
          <ng-container matColumnDef="fecha">
            <th mat-header-cell *matHeaderCellDef>{{ 'gastos.colDate' | translate }}</th>
            <td mat-cell *matCellDef="let row">{{ row.fecha | date:'dd/MM/yyyy' }}</td>
          </ng-container>
          <ng-container matColumnDef="proveedor">
            <th mat-header-cell *matHeaderCellDef>{{ 'gastos.colSupplier' | translate }}</th>
            <td mat-cell *matCellDef="let row">{{ row.proveedor }}</td>
          </ng-container>
          <ng-container matColumnDef="concepto">
            <th mat-header-cell *matHeaderCellDef>{{ 'gastos.colConcept' | translate }}</th>
            <td mat-cell *matCellDef="let row">{{ row.concepto }}</td>
          </ng-container>
          <ng-container matColumnDef="baseImponible">
            <th mat-header-cell *matHeaderCellDef>{{ 'gastos.colBase' | translate }}</th>
            <td mat-cell *matCellDef="let row">{{ row.baseImponible | number:'1.2-2' }} €</td>
          </ng-container>
          <ng-container matColumnDef="cuotaIva">
            <th mat-header-cell *matHeaderCellDef>{{ 'gastos.colVat' | translate }}</th>
            <td mat-cell *matCellDef="let row">{{ row.cuotaIva | number:'1.2-2' }} €</td>
          </ng-container>
          <ng-container matColumnDef="categoria">
            <th mat-header-cell *matHeaderCellDef>{{ 'gastos.colCategory' | translate }}</th>
            <td mat-cell *matCellDef="let row">{{ ('gastos.category.' + row.categoria) | translate }}</td>
          </ng-container>
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let row">
              @if (auth.canMutate()) {
                <button mat-icon-button [routerLink]="['/gastos', row.id]" [matTooltip]="'common.edit' | translate">
                  <mat-icon>edit</mat-icon>
                </button>
                <button mat-icon-button color="warn" (click)="delete(row)" [matTooltip]="'common.delete' | translate">
                  <mat-icon>delete</mat-icon>
                </button>
              }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          <tr class="mat-row" *matNoDataRow>
            <td class="mat-cell" colspan="7">{{ 'gastos.empty' | translate }}</td>
          </tr>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }

    .table-container {
      overflow-x: auto;
    }

    .full-width {
      width: 100%;
    }
  `],
})
export class GastoListComponent implements OnInit {
  displayedColumns = ['fecha', 'proveedor', 'concepto', 'baseImponible', 'cuotaIva', 'categoria', 'actions'];
  dataSource = new MatTableDataSource<Gasto>([]);

  constructor(
    public auth: AuthService,
    private gastoService: GastoService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private translate: TranslateService,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.gastoService.getAll().subscribe((data) => {
      this.dataSource.data = data;
    });
  }

  delete(gasto: Gasto): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: this.translate.instant('gastos.deleteTitle'),
        message: this.translate.instant('gastos.deleteMessage', { concepto: gasto.concepto }),
      },
    });
    ref.afterClosed().subscribe((ok) => {
      if (ok) {
        this.gastoService.delete(gasto.id).subscribe({
          next: () => {
            this.snackBar.open(
              this.translate.instant('gastos.deleted'),
              this.translate.instant('common.close'),
              { duration: 3000 },
            );
            this.load();
          },
          error: () => {
            this.snackBar.open(
              this.translate.instant('gastos.deleteFail'),
              this.translate.instant('common.close'),
              { duration: 3000 },
            );
          },
        });
      }
    });
  }
}
