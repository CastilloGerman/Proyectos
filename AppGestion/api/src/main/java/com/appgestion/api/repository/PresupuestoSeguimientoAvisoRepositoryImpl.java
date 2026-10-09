package com.appgestion.api.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.sql.Timestamp;

@Repository
public class PresupuestoSeguimientoAvisoRepositoryImpl implements PresupuestoSeguimientoAvisoRepositoryCustom {
    private final JdbcTemplate jdbcTemplate;

    public PresupuestoSeguimientoAvisoRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public int insertIfAbsent(Long presupuestoId, String tipo, int numeroAviso, Instant creadoAt) {
        String database = jdbcTemplate.execute(
                (org.springframework.jdbc.core.ConnectionCallback<String>)
                        connection -> connection.getMetaData().getDatabaseProductName());
        if ("PostgreSQL".equalsIgnoreCase(database)) {
            return jdbcTemplate.update("""
                    INSERT INTO presupuesto_seguimiento_aviso (presupuesto_id, tipo, numero_aviso, creado_at)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (presupuesto_id, numero_aviso) DO NOTHING
                    """, presupuestoId, tipo, numeroAviso, Timestamp.from(creadoAt));
        }
        Integer existing = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM presupuesto_seguimiento_aviso
                WHERE presupuesto_id = ? AND numero_aviso = ?
                """, Integer.class, presupuestoId, numeroAviso);
        if (existing != null && existing > 0) return 0;
        return jdbcTemplate.update("""
                MERGE INTO presupuesto_seguimiento_aviso
                (presupuesto_id, tipo, numero_aviso, creado_at)
                KEY (presupuesto_id, numero_aviso) VALUES (?, ?, ?, ?)
                """, presupuestoId, tipo, numeroAviso, Timestamp.from(creadoAt));
    }
}
