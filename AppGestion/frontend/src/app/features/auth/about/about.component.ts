import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  QueryList,
  ViewChild,
  ViewChildren,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-about',
  imports: [RouterLink, MatButtonModule, TranslateModule],
  templateUrl: './about.component.html',
  styleUrl: './about.component.scss',
})
export class AboutComponent implements AfterViewInit, OnDestroy {
  @ViewChildren('revealEl') revealEls!: QueryList<ElementRef<HTMLElement>>;
  @ViewChild('carouselHost') carouselHost?: ElementRef<HTMLElement>;

  /** Segundos visibles por imagen antes del cambio automático. */
  readonly slideDurationSec = 6;

  readonly founderSlides = [
    { src: 'assets/about/founder.jpg', altKey: 'auth.about.slide0Alt' },
    { src: 'assets/about/obra-codigo-1.png', altKey: 'auth.about.slide1Alt' },
    { src: 'assets/about/obra-codigo-2.png', altKey: 'auth.about.slide2Alt' },
  ] as const;

  readonly pillarCards = [
    { emoji: '⚡', titleKey: 'auth.about.pillar1Title', bodyKey: 'auth.about.pillar1Body' },
    { emoji: '🔒', titleKey: 'auth.about.pillar2Title', bodyKey: 'auth.about.pillar2Body' },
    { emoji: '🤝', titleKey: 'auth.about.pillar3Title', bodyKey: 'auth.about.pillar3Body' },
    { emoji: '📱', titleKey: 'auth.about.pillar4Title', bodyKey: 'auth.about.pillar4Body' },
    { emoji: '💡', titleKey: 'auth.about.pillar5Title', bodyKey: 'auth.about.pillar5Body' },
    { emoji: '🔄', titleKey: 'auth.about.pillar6Title', bodyKey: 'auth.about.pillar6Body' },
  ] as const;

  readonly activeSlideIndex = signal(0);

  private revealObserver?: IntersectionObserver;
  private carouselVisibilityObserver?: IntersectionObserver;
  /** ID de `setInterval` (tipado como número para evitar conflicto con tipos de Node). */
  private tickTimer?: number;

  ngAfterViewInit(): void {
    this.revealObserver = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            entry.target.classList.add('visible');
            this.revealObserver?.unobserve(entry.target);
          }
        }
      },
      { threshold: 0.12, rootMargin: '0px 0px -5% 0px' },
    );
    for (const ref of this.revealEls) {
      this.revealObserver.observe(ref.nativeElement);
    }

    this.initCarouselVisibilityObserver();
  }

  private initCarouselVisibilityObserver(): void {
    const el = this.carouselHost?.nativeElement;
    if (!el) {
      return;
    }
    this.carouselVisibilityObserver = new IntersectionObserver(
      (entries) => {
        const visible = entries.some((e) => e.isIntersecting && e.intersectionRatio >= 0.2);
        if (visible) {
          this.startCarouselAutoplay();
        } else {
          this.stopCarouselAutoplay();
        }
      },
      { threshold: [0, 0.15, 0.25, 0.5] },
    );
    this.carouselVisibilityObserver.observe(el);
  }

  private prefersReducedMotion(): boolean {
    return typeof matchMedia !== 'undefined' && matchMedia('(prefers-reduced-motion: reduce)').matches;
  }

  private startCarouselAutoplay(): void {
    if (this.prefersReducedMotion() || this.tickTimer !== undefined) {
      return;
    }
    const ms = this.slideDurationSec * 1000;
    this.tickTimer = window.setInterval(() => {
      const n = this.founderSlides.length;
      this.activeSlideIndex.update((i) => (i + 1) % n);
    }, ms) as unknown as number;
  }

  private stopCarouselAutoplay(): void {
    if (this.tickTimer !== undefined) {
      clearInterval(this.tickTimer);
      this.tickTimer = undefined;
    }
  }

  goToSlide(index: number): void {
    if (index < 0 || index >= this.founderSlides.length) {
      return;
    }
    this.activeSlideIndex.set(index);
    if (this.tickTimer !== undefined) {
      clearInterval(this.tickTimer);
      this.tickTimer = undefined;
      this.startCarouselAutoplay();
    }
  }

  trackOffsetPercent(): string {
    return String((100 * this.activeSlideIndex()) / this.founderSlides.length);
  }

  ngOnDestroy(): void {
    this.stopCarouselAutoplay();
    this.revealObserver?.disconnect();
    this.carouselVisibilityObserver?.disconnect();
  }

  onImgError(ev: Event): void {
    const img = ev.target as HTMLImageElement;
    img.style.display = 'none';
  }

  scrollToTop(): void {
    const top = document.getElementById('login-page-top');
    if (top) {
      top.scrollIntoView({ behavior: 'smooth', block: 'start' });
      return;
    }
    const root = document.scrollingElement ?? document.documentElement;
    root.scrollTo({ top: 0, behavior: 'smooth' });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}
