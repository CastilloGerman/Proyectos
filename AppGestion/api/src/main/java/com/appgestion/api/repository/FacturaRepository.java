package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.Factura;
import com.appgestion.api.domain.enums.TipoFactura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {

    List<Factura> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    List<Factura> findByUsuarioIdAndClienteIdOrderByFechaCreacionDesc(Long usuarioId, Long clienteId);

    /** Pares (presupuesto_id, factura_id) para clientes con facturas ligadas a presupuesto. */
    @Query("SELECT f.presupuesto.id, f.id FROM Factura f WHERE f.usuario.id = :usuarioId AND f.cliente.id = :clienteId AND f.presupuesto IS NOT NULL")
    List<Object[]> findPresupuestoFacturaIdPairsByCliente(@Param("usuarioId") Long usuarioId, @Param("clienteId") Long clienteId);

    @Query(value = "SELECT * FROM facturas WHERE id = :id AND usuario_id = :usuarioId", nativeQuery = true)
    Optional<Factura> findByIdAndUsuarioId(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    @Query("SELECT f FROM Factura f JOIN FETCH f.cliente JOIN FETCH f.usuario WHERE f.id = :id AND f.usuario.id = :usuarioId")
    Optional<Factura> findByIdAndUsuarioIdWithRelaciones(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    boolean existsByIdAndUsuarioId(Long id, Long usuarioId);

    Optional<Factura> findByNumeroFacturaAndUsuarioId(String numeroFactura, Long usuarioId);

    Optional<Factura> findFirstByPresupuesto_IdAndUsuario_Id(Long presupuestoId, Long usuarioId);

    Optional<Factura> findByPresupuesto_IdAndUsuario_IdAndTipoFactura(
            Long presupuestoId, Long usuarioId, TipoFactura tipoFactura);

    /**
     * Facturas de venta principal (excluye {@link TipoFactura#ANTICIPO}).
     * Usar enums como parámetros: en JPQL los literales 'NORMAL' no siempre excluyen bien ANTICIPO y
     * el presupuesto podía mostrar facturaId = factura de anticipo → la UI ocultaba «Facturar» el resto.
     */
    @Query("SELECT f FROM Factura f WHERE f.presupuesto.id = :pid AND f.usuario.id = :uid "
            + "AND (f.tipoFactura IS NULL OR f.tipoFactura = :normal OR f.tipoFactura = :finalConAnticipo) "
            + "ORDER BY f.id DESC")
    List<Factura> findVentasPrincipalesPorPresupuesto(
            @Param("pid") Long presupuestoId,
            @Param("uid") Long usuarioId,
            @Param("normal") TipoFactura normal,
            @Param("finalConAnticipo") TipoFactura finalConAnticipo);

    long countByUsuarioId(Long usuarioId);

    @Query("SELECT f FROM Factura f JOIN FETCH f.usuario JOIN FETCH f.cliente " +
           "WHERE f.estadoPago NOT IN ('Pagada') " +
           "AND f.fechaVencimiento IS NOT NULL " +
           "AND f.fechaVencimiento <= :hasta " +
           "AND (f.recordatorioEnviado IS NULL OR f.recordatorioEnviado = false)")
    List<Factura> findFacturasParaRecordatorio(@Param("hasta") LocalDate hasta);

    /** Facturas impagadas cuya fecha de vencimiento coincide con alguna de las dadas (p. ej. hoy−7, hoy−15). */
    @Query("SELECT f FROM Factura f JOIN FETCH f.usuario u JOIN FETCH f.cliente c "
            + "WHERE f.estadoPago <> 'Pagada' "
            + "AND f.fechaVencimiento IS NOT NULL "
            + "AND f.fechaVencimiento IN :fechas")
    List<Factura> findImpagadasConFechaVencimientoEn(@Param("fechas") Collection<LocalDate> fechas);

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(numero_factura FROM '[0-9]+$') AS INTEGER)), 0) FROM facturas WHERE usuario_id = :usuarioId AND numero_factura LIKE :pattern", nativeQuery = true)
    int findMaxNumeroInYear(@Param("usuarioId") Long usuarioId, @Param("pattern") String pattern);
}
