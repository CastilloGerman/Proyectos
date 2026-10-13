package com.appgestion.api.unit.service;

import com.appgestion.api.domain.entity.Factura;
import com.appgestion.api.domain.entity.FacturaAuditoria;
import com.appgestion.api.repository.FacturaAuditoriaRepository;
import com.appgestion.api.service.FacturaAuditoriaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** SEC-15: la pista de auditorÃ­a de facturas es una cadena de hashes verificable. */
class FacturaAuditoriaServiceTest {

    private FacturaAuditoriaRepository repository;
    private FacturaAuditoriaService service;
    private final List<FacturaAuditoria> almacen = new ArrayList<>();

    @BeforeEach
    void setUp() {
        repository = mock(FacturaAuditoriaRepository.class);
        when(repository.save(any(FacturaAuditoria.class))).thenAnswer(inv -> {
            FacturaAuditoria r = inv.getArgument(0);
            almacen.add(r);
            return r;
        });
        when(repository.findTopByFacturaIdOrderByIdDesc(anyLong())).thenAnswer(inv ->
                almacen.isEmpty() ? Optional.empty() : Optional.of(almacen.get(almacen.size() - 1)));
        when(repository.findByFacturaIdOrderByIdAsc(anyLong())).thenAnswer(inv -> List.copyOf(almacen));
        service = new FacturaAuditoriaService(repository, new ObjectMapper());
    }

    private static Factura factura() {
        Factura f = new Factura();
        f.setId(7L);
        f.setNumeroFactura("2026-0001");
        f.setSubtotal(100.0);
        f.setIva(21.0);
        f.setTotal(121.0);
        return f;
    }

    @Test
    void cadenaIntegra_seVerifica() {
        Factura f = factura();
        service.registrar(f, FacturaAuditoriaService.EVENTO_CREADA, null, 1L);
        Map<String, Object> antes = service.snapshot(f);
        f.setTotal(150.0);
        service.registrar(f, FacturaAuditoriaService.EVENTO_EDITADA, antes, 1L);

        assertEquals(2, almacen.size());
        assertEquals(FacturaAuditoriaService.GENESIS_HASH, almacen.get(0).getPrevHash());
        assertEquals(almacen.get(0).getHash(), almacen.get(1).getPrevHash());
        assertTrue(service.verificarCadena(7L));
    }

    @Test
    void alterarUnRegistroHistorico_rompeLaCadena() {
        Factura f = factura();
        service.registrar(f, FacturaAuditoriaService.EVENTO_CREADA, null, 1L);
        service.registrar(f, FacturaAuditoriaService.EVENTO_EDITADA, service.snapshot(f), 1L);

        almacen.get(0).setDatosJson(almacen.get(0).getDatosJson().replace("121.0", "1.0"));

        assertFalse(service.verificarCadena(7L));
    }

    @Test
    void borrarUnRegistroIntermedio_rompeLaCadena() {
        Factura f = factura();
        service.registrar(f, FacturaAuditoriaService.EVENTO_CREADA, null, 1L);
        service.registrar(f, FacturaAuditoriaService.EVENTO_EDITADA, service.snapshot(f), 1L);
        service.registrar(f, FacturaAuditoriaService.EVENTO_ANULADA, service.snapshot(f), 1L);

        almacen.remove(1);

        assertFalse(service.verificarCadena(7L));
    }
}
