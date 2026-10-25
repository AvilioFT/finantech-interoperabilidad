package com.finantech.transacciones;

import com.finantech.transacciones.dto.TransaccionRequest;
import com.finantech.transacciones.dto.TransaccionResponse;
import com.finantech.transacciones.service.NotificationService;
import com.finantech.transacciones.service.TransaccionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class TransaccionServiceTest {

    private TransaccionService service;

    @BeforeEach
    void setUp() {
        service = new TransaccionService(new NotificationService(mock(JavaMailSender.class)));
    }

    private TransaccionRequest request(BigDecimal monto) {
        TransaccionRequest req = new TransaccionRequest();
        req.setCuentaOrigen("001-1234567890");
        req.setCuentaDestino("002-0987654321");
        req.setMonto(monto);
        req.setMoneda("PEN");
        req.setDescripcion("Prueba de transaccion");
        return req;
    }

    @Test
    void shouldCreateTransaccionSuccessfully() {
        TransaccionResponse resp = service.crearTransaccion(request(BigDecimal.valueOf(500.00)), "test-key-1");

        assertNotNull(resp);
        assertEquals("APROBADA", resp.getEstado());
        assertNotNull(resp.getIdTransaccion());
        assertTrue(resp.getIdTransaccion().matches("TX-\\d{4}-\\d{6}"), "Id inesperado: " + resp.getIdTransaccion());
        assertEquals("001-1234567890", resp.getCuentaOrigen());
        assertEquals("002-0987654321", resp.getCuentaDestino());
        assertEquals(0, BigDecimal.valueOf(500.00).compareTo(resp.getMonto()));
    }

    @Test
    void shouldReuseTransactionOnDuplicateKey() {
        TransaccionRequest req = request(BigDecimal.valueOf(100.00));

        TransaccionResponse first = service.crearTransaccion(req, "idem-key");
        TransaccionResponse second = service.crearTransaccion(req, "idem-key");

        assertEquals(first.getIdTransaccion(), second.getIdTransaccion());
    }

    @Test
    void shouldFailInvalidAmount() {
        TransaccionResponse resp = service.crearTransaccion(request(BigDecimal.ZERO), "invalid-key");

        assertNotNull(resp);
        assertEquals("FALLIDA", resp.getEstado());
    }

    @Test
    void shouldReturnNotFoundWhenTransaccionDoesNotExist() {
        assertNull(service.obtenerTransaccion("TX-9999-999999"));
    }
}
