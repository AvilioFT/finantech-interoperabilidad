package com.finantech.transacciones.service;

import com.finantech.transacciones.dto.TransaccionRequest;
import com.finantech.transacciones.dto.TransaccionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TransaccionService {

    private static final Logger log = LoggerFactory.getLogger(TransaccionService.class);

    private static final AtomicLong SECUENCIA = new AtomicLong(0);

    private final ConcurrentHashMap<String, TransaccionResponse> cachePorIdempotencia = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TransaccionResponse> transacciones = new ConcurrentHashMap<>();
    private final NotificationService notificationService;

    public TransaccionService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public TransaccionResponse crearTransaccion(TransaccionRequest request, String idempotencyKey) {
        String key = (idempotencyKey == null || idempotencyKey.isBlank()) ? UUID.randomUUID().toString() : idempotencyKey;

        TransaccionResponse cached = cachePorIdempotencia.get(key);
        if (cached != null) {
            log.debug("Idempotencia: transaccion {} recuperada para key {}", cached.getIdTransaccion(), key);
            return cached;
        }

        String idTransaccion = generarId();
        boolean fallida = request.getMonto() == null || request.getMonto().compareTo(BigDecimal.ZERO) <= 0;

        TransaccionResponse response = new TransaccionResponse(
                idTransaccion,
                request.getCuentaOrigen(),
                request.getCuentaDestino(),
                request.getMonto(),
                request.getMoneda(),
                fallida ? "FALLIDA" : "APROBADA",
                Instant.now(),
                fallida ? "Transaccion fallida: el monto debe ser mayor que cero"
                        : "Transaccion procesada");

        cachePorIdempotencia.put(key, response);
        transacciones.put(idTransaccion, response);

        if (fallida) {
            notificationService.notificarFallido("alertas@finantech.com",
                    "Transaccion fallida",
                    String.format("ID=%s Cuenta=%s->%s Monto=%s Concepto=%s",
                            idTransaccion, request.getCuentaOrigen(), request.getCuentaDestino(),
                            request.getMonto(), request.getDescripcion()));
        } else {
            notificationService.notificarExito("remitente.exit@finantech.com",
                    "Transaccion confirmada",
                    String.format("OK. %s->%s %.2f %s",
                            request.getCuentaOrigen(), request.getCuentaDestino(),
                            request.getMonto(), request.getDescripcion()));
        }

        return response;
    }

    public TransaccionResponse obtenerTransaccion(String idTransaccion) {
        return transacciones.get(idTransaccion);
    }

    private String generarId() {
        return String.format("TX-%d-%06d", LocalDate.now().getYear(), SECUENCIA.incrementAndGet());
    }
}
