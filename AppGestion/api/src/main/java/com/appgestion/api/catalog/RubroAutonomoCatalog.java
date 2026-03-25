package com.appgestion.api.catalog;

import java.util.Set;

/**
 * Códigos permitidos para {@code Empresa.rubroAutonomoCodigo} (métricas; no en facturas).
 * Debe coincidir con {@code rubro-autonomo.catalog.ts} en el frontend.
 */
public final class RubroAutonomoCatalog {

    private static final Set<String> CODIGOS = Set.of(
            // Informática y digital
            "DESARROLLO_SOFTWARE",
            "DISENO_WEB_UX",
            "CONSULTORIA_IT",
            "CIBERSEGURIDAD",
            "MARKETING_DIGITAL",
            "COMMUNITY_MANAGER",
            // Construcción y reformas
            "ALBANILERIA",
            "FONTANERIA",
            "ELECTRICIDAD",
            "CARPINTERIA",
            "PINTURA",
            "REFORMAS_INTEGRALES",
            "INSTALACIONES_CLIMATIZACION",
            // Comercio
            "COMERCIO_MINORISTA",
            "COMERCIO_ONLINE",
            "DISTRIBUCION",
            // Hostelería
            "RESTAURACION",
            "BAR_CAFETERIA",
            "CATERING",
            "PASTELERIA",
            // Salud y bienestar
            "FISIOTERAPIA",
            "ENFERMERIA_LIBERAL",
            "PSICOLOGIA",
            "NUTRICION_DIETETICA",
            "PODOLOGIA",
            "FARMACIA",
            // Educación y formación
            "FORMACION_PROFESIONAL",
            "IDIOMAS",
            "REFUERZO_ESCOLAR",
            "COACHING",
            // Transporte y motor
            "TAXI_VTC",
            "TRANSPORTE_MERCANCIAS",
            "MECANICA_VEHICULOS",
            "MOTO_TALLER",
            // Servicios profesionales y admin
            "ASESORIA_FISCAL_LABORAL",
            "ABOGACIA",
            "GESTORIA_ADMINISTRATIVA",
            "ARQUITECTURA",
            "INGENIERIA_TECNICA",
            "MEDIACION",
            // Arte, foto y audiovisual
            "FOTOGRAFIA",
            "VIDEO_AUDIOVISUAL",
            "DISENO_GRAFICO",
            "ILUSTRACION",
            // Belleza y cuidado personal
            "PELUQUERIA",
            "ESTETICA",
            "UNAS_MICROPIGMENTACION",
            // Limpieza, jardín y mantenimiento
            "LIMPIEZA",
            "JARDINERIA",
            "MANTENIMIENTO_EDIFICIOS",
            "CONTROL_DE_PLAGAS",
            // Agricultura y ganadería
            "AGRICULTURA_GANADERIA",
            "VITICULTURA_ENOLOGIA",
            // Otros
            "OTRO",
            "PREFIERO_NO_DECIR"
    );

    private RubroAutonomoCatalog() {}

    public static boolean esCodigoValido(String codigo) {
        return codigo != null && CODIGOS.contains(codigo);
    }
}
