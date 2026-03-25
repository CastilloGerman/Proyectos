/**
 * Rubros de autónomos por categoría (códigos estables para métricas en servidor).
 * Debe coincidir con {@link com.appgestion.api.catalog.RubroAutonomoCatalog} en la API.
 */
export interface RubroAutonomoOpcion {
  codigo: string;
  etiqueta: string;
}

export interface RubroAutonomoCategoria {
  id: string;
  etiqueta: string;
  opciones: RubroAutonomoOpcion[];
}

export const RUBRO_AUTONOMO_CATEGORIAS: RubroAutonomoCategoria[] = [
  {
    id: 'informatica',
    etiqueta: 'Informática y digital',
    opciones: [
      { codigo: 'DESARROLLO_SOFTWARE', etiqueta: 'Desarrollo de software' },
      { codigo: 'DISENO_WEB_UX', etiqueta: 'Diseño web / UX' },
      { codigo: 'CONSULTORIA_IT', etiqueta: 'Consultoría informática' },
      { codigo: 'CIBERSEGURIDAD', etiqueta: 'Ciberseguridad' },
      { codigo: 'MARKETING_DIGITAL', etiqueta: 'Marketing digital' },
      { codigo: 'COMMUNITY_MANAGER', etiqueta: 'Community manager' },
    ],
  },
  {
    id: 'construccion',
    etiqueta: 'Construcción y reformas',
    opciones: [
      { codigo: 'ALBANILERIA', etiqueta: 'Albañilería' },
      { codigo: 'FONTANERIA', etiqueta: 'Fontanería' },
      { codigo: 'ELECTRICIDAD', etiqueta: 'Electricidad' },
      { codigo: 'CARPINTERIA', etiqueta: 'Carpintería' },
      { codigo: 'PINTURA', etiqueta: 'Pintura' },
      { codigo: 'REFORMAS_INTEGRALES', etiqueta: 'Reformas integrales' },
      { codigo: 'INSTALACIONES_CLIMATIZACION', etiqueta: 'Climatización / frío industrial' },
    ],
  },
  {
    id: 'comercio',
    etiqueta: 'Comercio',
    opciones: [
      { codigo: 'COMERCIO_MINORISTA', etiqueta: 'Comercio minorista (tienda física)' },
      { codigo: 'COMERCIO_ONLINE', etiqueta: 'Comercio online / e‑commerce' },
      { codigo: 'DISTRIBUCION', etiqueta: 'Distribución / mayorista' },
    ],
  },
  {
    id: 'hosteleria',
    etiqueta: 'Hostelería',
    opciones: [
      { codigo: 'RESTAURACION', etiqueta: 'Restauración' },
      { codigo: 'BAR_CAFETERIA', etiqueta: 'Bar / cafetería' },
      { codigo: 'CATERING', etiqueta: 'Catering' },
      { codigo: 'PASTELERIA', etiqueta: 'Pastelería / panadería artesanal' },
    ],
  },
  {
    id: 'salud',
    etiqueta: 'Salud y bienestar',
    opciones: [
      { codigo: 'FISIOTERAPIA', etiqueta: 'Fisioterapia' },
      { codigo: 'ENFERMERIA_LIBERAL', etiqueta: 'Enfermería (ejercicio liberal)' },
      { codigo: 'PSICOLOGIA', etiqueta: 'Psicología' },
      { codigo: 'NUTRICION_DIETETICA', etiqueta: 'Nutrición / dietética' },
      { codigo: 'PODOLOGIA', etiqueta: 'Podología' },
      { codigo: 'FARMACIA', etiqueta: 'Farmacia (titular)' },
    ],
  },
  {
    id: 'educacion',
    etiqueta: 'Educación y formación',
    opciones: [
      { codigo: 'FORMACION_PROFESIONAL', etiqueta: 'Formación profesional / cursos' },
      { codigo: 'IDIOMAS', etiqueta: 'Idiomas' },
      { codigo: 'REFUERZO_ESCOLAR', etiqueta: 'Refuerzo escolar' },
      { codigo: 'COACHING', etiqueta: 'Coaching / desarrollo personal' },
    ],
  },
  {
    id: 'transporte',
    etiqueta: 'Transporte y motor',
    opciones: [
      { codigo: 'TAXI_VTC', etiqueta: 'Taxi / VTC' },
      { codigo: 'TRANSPORTE_MERCANCIAS', etiqueta: 'Transporte de mercancías' },
      { codigo: 'MECANICA_VEHICULOS', etiqueta: 'Mecánica de vehículos' },
      { codigo: 'MOTO_TALLER', etiqueta: 'Taller de motos' },
    ],
  },
  {
    id: 'profesional',
    etiqueta: 'Servicios profesionales y administración',
    opciones: [
      { codigo: 'ASESORIA_FISCAL_LABORAL', etiqueta: 'Asesoría fiscal / laboral' },
      { codigo: 'ABOGACIA', etiqueta: 'Abogacía' },
      { codigo: 'GESTORIA_ADMINISTRATIVA', etiqueta: 'Gestoría administrativa' },
      { codigo: 'ARQUITECTURA', etiqueta: 'Arquitectura' },
      { codigo: 'INGENIERIA_TECNICA', etiqueta: 'Ingeniería técnica' },
      { codigo: 'MEDIACION', etiqueta: 'Mediación / arbitraje' },
    ],
  },
  {
    id: 'arte',
    etiqueta: 'Arte, foto y audiovisual',
    opciones: [
      { codigo: 'FOTOGRAFIA', etiqueta: 'Fotografía' },
      { codigo: 'VIDEO_AUDIOVISUAL', etiqueta: 'Vídeo / audiovisual' },
      { codigo: 'DISENO_GRAFICO', etiqueta: 'Diseño gráfico' },
      { codigo: 'ILUSTRACION', etiqueta: 'Ilustración' },
    ],
  },
  {
    id: 'belleza',
    etiqueta: 'Belleza y estética',
    opciones: [
      { codigo: 'PELUQUERIA', etiqueta: 'Peluquería' },
      { codigo: 'ESTETICA', etiqueta: 'Estética / tratamientos' },
      { codigo: 'UNAS_MICROPIGMENTACION', etiqueta: 'Uñas / micropigmentación' },
    ],
  },
  {
    id: 'limpieza',
    etiqueta: 'Limpieza y mantenimiento',
    opciones: [
      { codigo: 'LIMPIEZA', etiqueta: 'Limpieza (locales, oficinas…)' },
      { codigo: 'JARDINERIA', etiqueta: 'Jardinería' },
      { codigo: 'MANTENIMIENTO_EDIFICIOS', etiqueta: 'Mantenimiento de edificios' },
      { codigo: 'CONTROL_DE_PLAGAS', etiqueta: 'Control de plagas' },
    ],
  },
  {
    id: 'agricultura',
    etiqueta: 'Agricultura y sector primario',
    opciones: [
      { codigo: 'AGRICULTURA_GANADERIA', etiqueta: 'Agricultura / ganadería' },
      { codigo: 'VITICULTURA_ENOLOGIA', etiqueta: 'Viticultura / enología' },
    ],
  },
  {
    id: 'otros',
    etiqueta: 'Otros',
    opciones: [
      { codigo: 'OTRO', etiqueta: 'Otro (genérico)' },
      { codigo: 'PREFIERO_NO_DECIR', etiqueta: 'Prefiero no indicarlo' },
    ],
  },
];
