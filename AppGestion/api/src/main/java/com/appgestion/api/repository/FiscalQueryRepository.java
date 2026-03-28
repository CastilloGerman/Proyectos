package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.Factura;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Consultas nativas de agregación fiscal (303 / 347). Sin CRUD genérico.
 */
public interface FiscalQueryRepository extends Repository<Factura, Long> {

    @Query(value = """
            SELECT CAST(COALESCE(SUM(f.subtotal), 0) AS numeric),
                   CAST(COALESCE(SUM(f.iva), 0) AS numeric),
                   COUNT(f.id)
            FROM facturas f
            WHERE f.usuario_id = :usuarioId
              AND COALESCE(f.fecha_expedicion, f.fecha_operacion, CAST(f.fecha_creacion AS date)) BETWEEN :desde AND :hasta
              AND (NOT :soloPagadas OR TRIM(f.estado_pago) = 'Pagada')
            """, nativeQuery = true)
    Object[] aggregateVentasModelo303Devengo(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("soloPagadas") boolean soloPagadas
    );

    @Query(value = """
            SELECT CAST(COALESCE(SUM(f.subtotal), 0) AS numeric),
                   CAST(COALESCE(SUM(f.iva), 0) AS numeric),
                   COUNT(DISTINCT f.id)
            FROM facturas f
            INNER JOIN (
                SELECT factura_id, MAX(fecha) AS ultima_fecha
                FROM factura_cobros
                GROUP BY factura_id
            ) cob ON cob.factura_id = f.id
            WHERE f.usuario_id = :usuarioId
              AND TRIM(f.estado_pago) = 'Pagada'
              AND cob.ultima_fecha BETWEEN :desde AND :hasta
            """, nativeQuery = true)
    Object[] aggregateVentasModelo303Caja(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );

    @Query(value = """
            SELECT c.id, c.nombre, c.dni, CAST(COALESCE(SUM(f.subtotal), 0) AS numeric)
            FROM facturas f
            INNER JOIN clientes c ON c.id = f.cliente_id
            WHERE f.usuario_id = :usuarioId
              AND COALESCE(f.fecha_expedicion, f.fecha_operacion, CAST(f.fecha_creacion AS date)) BETWEEN :desde AND :hasta
              AND c.dni IS NOT NULL
              AND TRIM(c.dni) <> ''
            GROUP BY c.id, c.nombre, c.dni
            HAVING COALESCE(SUM(f.subtotal), 0) > :umbral
            ORDER BY 4 DESC
            """, nativeQuery = true)
    List<Object[]> findClientesBaseAnualSuperaUmbral347(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("umbral") double umbral
    );
}
