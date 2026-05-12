import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { take } from 'rxjs';

/** Enlaces antiguos `/invite/:token/` redirigen al login conservando `ref`. */
@Component({
    selector: 'app-invite-accept',
    template: `<p class="invite-redirect-msg">Redirigiendo…</p>`,
    standalone: true,
    styles: [
        `.invite-redirect-msg { margin: 48px; text-align: center; font-family: system-ui, sans-serif; color: #475569; }`,
    ],
})
export class InviteAcceptComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);

    ngOnInit(): void {
        this.route.paramMap.pipe(take(1)).subscribe((pm) => {
            const token = pm.get('token');
            if (token?.trim()) {
                void this.router.navigate(['/login'], { queryParams: { ref: token.trim() }, replaceUrl: true });
            } else {
                void this.router.navigate(['/login'], { replaceUrl: true });
            }
        });
    }
}
