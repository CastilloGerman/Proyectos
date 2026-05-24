import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

interface Line {
  points: string;
  stroke: string;
  strokeWidth: number;
  dasharray: number;
  duration: number;
  delay: number;
}

@Component({
  selector: 'app-animated-background',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './animated-background.component.html',
  styleUrls: ['./animated-background.component.scss'],
})
export class AnimatedBackgroundComponent implements OnInit {
  // Opacidad global 0–1. Ajusta este valor para cada sección.
  // Login: 0.45 | Hero: 0.35 | Secciones interiores: 0.25
  @Input() opacity = 0.45;

  // Viewbox del SVG — se adapta al contenedor
  viewBox = '0 0 1440 900';

  lines: Line[] = [];

  // Paleta exacta del logo Noemi
  private palette = {
    purple: '#6B3FA0',
    orange: '#F5843A',
    coral: '#F05A5A',
    navy: '#2D3E50',
  };

  ngOnInit(): void {
    this.lines = [
      {
        points: '0,680 240,680 460,320 660,320 860,560 1100,560 1440,420',
        stroke: this.palette.purple,
        strokeWidth: 1.2,
        dasharray: 1100,
        duration: 5.0,
        delay: 0.0,
      },
      {
        points: '0,260 200,260 400,100 620,100 820,300 1060,300 1440,200',
        stroke: this.palette.orange,
        strokeWidth: 0.9,
        dasharray: 1200,
        duration: 6.0,
        delay: 0.8,
      },
      {
        points: '0,820 160,820 360,520 580,520 760,720 980,720 1440,600',
        stroke: this.palette.coral,
        strokeWidth: 1.4,
        dasharray: 1050,
        duration: 4.5,
        delay: 1.6,
      },
      {
        points: '0,130 220,130 420,420 640,420 780,160 1020,160 1440,300',
        stroke: this.palette.navy,
        strokeWidth: 0.6,
        dasharray: 1300,
        duration: 7.0,
        delay: 0.4,
      },
      {
        points: '0,480 280,480 480,220 700,220 900,480 1140,480 1440,360',
        stroke: this.palette.orange,
        strokeWidth: 0.7,
        dasharray: 1150,
        duration: 5.5,
        delay: 2.0,
      },
      {
        points: '0,880 120,880 320,580 560,580 720,800 960,800 1440,700',
        stroke: this.palette.purple,
        strokeWidth: 0.5,
        dasharray: 1400,
        duration: 8.0,
        delay: 1.2,
      },
      {
        points: '0,60  160,60  300,280 520,280 700,60  960,60  1440,180',
        stroke: this.palette.coral,
        strokeWidth: 0.8,
        dasharray: 950,
        duration: 4.0,
        delay: 2.8,
      },
    ];
  }

  lineStyle(line: Line): Record<string, string> {
    return {
      '--dasharray': `${line.dasharray}`,
      '--duration': `${line.duration}s`,
      '--delay': `${line.delay}s`,
      'stroke-dasharray': `${line.dasharray}`,
      'stroke-dashoffset': `${line.dasharray}`,
      'animation-duration': `${line.duration}s`,
      'animation-delay': `${line.delay}s`,
    };
  }
}
